package io.github.jacopocarlini.fffp.providers;

import static io.github.jacopocarlini.fffp.util.ProviderUtility.*;
import static io.github.jacopocarlini.fffp.util.Reason.*;

import dev.openfeature.sdk.*;
import io.github.jacopocarlini.fffp.entity.AssignedTarget;
import io.github.jacopocarlini.fffp.entity.Flag;
import io.github.jacopocarlini.fffp.exceptions.InvalidFeatureFlagException;
import io.github.jacopocarlini.fffp.repository.AssignedTargetRepository;
import io.github.jacopocarlini.fffp.repository.FlagRepository;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class FlagService {

  @Autowired AssignedTargetRepository assignedTargetRepository;
  @Autowired FlagRepository flagRepository;

  private Validator validator;

  public FlagService() {
    try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
      validator = factory.getValidator();
    }
  }

  public <T> ProviderEvaluation<T> evaluation(
      String flagKey, T defaultValue, EvaluationContext ctx, Class<T> valueType) {
    System.out.println("EVAL start");
    try {
      Optional<Flag> optionalFlag = flagRepository.findFirstByFlagKey(flagKey);

      if (optionalFlag.isEmpty()) {
        return createDefaultEvaluation(defaultValue, "flag_not_found");
      }

      Flag flag = optionalFlag.get();

      // check if the flag is enabled
      if (!Boolean.TRUE.equals(flag.getEnabled())) {
        return createDefaultEvaluation(defaultValue, FLAG_DISABLED.name());
      }

      // check time window
      if (isOutsideTimeWindow(flag)) {
        return createDefaultEvaluation(defaultValue, OUTSIDE_TIME_WINDOW.name());
      }

      // check the target
      Optional<ProviderEvaluation<T>> targetMatch = checkTargetMatch(flag, ctx, valueType);
      if (targetMatch.isPresent()) {
        return targetMatch.get();
      }

      // handle rollout
      if (flag.getRolloutPercentage() != null && !flag.getRolloutPercentage().isEmpty()) {
        return handleRollout(flag, ctx, valueType);
      }

      // base case: return default variant
      return ProviderEvaluation.<T>builder()
          .value(convertValue(flag.getVariants().get(flag.getDefaultVariant()), valueType))
          .variant(flag.getDefaultVariant())
          .reason("default variant")
          .build();

    } catch (InvalidFeatureFlagException | ClassCastException e) {
      log.error("Evaluation error for flag {}: {}", flagKey, e.getMessage());
      return createDefaultEvaluation(defaultValue, INVALID_FLAG_DATA.name());
    } catch (DataAccessException e) {
      log.error("Database error evaluating flag {}: {}", flagKey, e.getMessage(), e);
      return createDefaultEvaluation(defaultValue, "database_error");
    } finally {
      System.out.println("EVAL end");
    }
  }

  private <T> ProviderEvaluation<T> createDefaultEvaluation(T defaultValue, String reason) {
    return ProviderEvaluation.<T>builder()
        .value(defaultValue)
        .reason(reason)
        .variant(DEFAULT_VALUE.name())
        .build();
  }

  private <T> ProviderEvaluation<T> handleRollout(
      Flag flag, EvaluationContext ctx, Class<T> valueType) throws InvalidFeatureFlagException {
    String targetKey = ctx.getTargetingKey();
    String variant = determineVariantForRollout(flag);
    if (targetKey != null) {
      // check if the user is already assigned to a variant
      Optional<AssignedTarget> optionalAssignedTarget =
          assignedTargetRepository.findFirstByFlagKeyAndTargetKey(flag.getFlagKey(), targetKey);

      if (optionalAssignedTarget.isPresent()) {
        AssignedTarget assignedTarget = optionalAssignedTarget.get();
        return ProviderEvaluation.<T>builder()
            .value(convertValue(flag.getVariants().get(assignedTarget.getVariant()), valueType))
            .variant(assignedTarget.getVariant())
            .reason(ALREADY_ASSIGNED.name())
            .build();
      }

      // assign the user to the variant
      assignedTargetRepository.save(new AssignedTarget(flag.getFlagKey(), targetKey, variant));
    }
    return ProviderEvaluation.<T>builder()
        .value(convertValue(flag.getVariants().get(variant), valueType))
        .reason(ROLLOUT.name())
        .variant(variant)
        .build();
  }

  public List<Flag> getFlags() {
    return flagRepository.findAll();
  }

  public Flag getFlag(String flagKey) throws InvalidFeatureFlagException {
    return getFlagIfIsPresent(flagKey);
  }

  public void createFlag(Flag flag) throws InvalidFeatureFlagException {
    validateFlag(flag);

    var isPresent = flagRepository.findFirstByFlagKey(flag.getFlagKey()).isPresent();
    if (isPresent) {
      throw new InvalidFeatureFlagException("Conflict. Flag already present");
    }

    checkRolloutPercentage(flag);
    checkVariant(flag);

    flagRepository.save(flag);
  }

  public void updateFlag(String flagKey, Flag newFlag) throws InvalidFeatureFlagException {
    validateFlag(newFlag);

    var flag = getFlagIfIsPresent(flagKey);

    checkRolloutPercentage(newFlag);
    checkVariant(newFlag);

    newFlag.setId(flag.getId());

    if (newFlag.getRolloutPercentage() == null
        || newFlag.getRolloutPercentage().equals(flag.getRolloutPercentage())) {
      assignedTargetRepository.deleteAllByFlagKey(flagKey);
    }

    flagRepository.save(newFlag);
  }

  public void deleteFlag(String flagKey) throws InvalidFeatureFlagException {
    System.out.println("DELETE start");
    var flag = flagRepository.findFirstByFlagKey(flagKey);
    if (flag.isEmpty()) {
      throw new InvalidFeatureFlagException("Flag not found");
    }
    assignedTargetRepository.deleteAllByFlagKey(flagKey);
    flagRepository.deleteByFlagKey(flagKey);
    System.out.println("DELETE end");
  }

  private Flag getFlagIfIsPresent(String flagKey) throws InvalidFeatureFlagException {
    var flag = flagRepository.findFirstByFlagKey(flagKey);
    if (flag.isEmpty()) {
      throw new InvalidFeatureFlagException("Flag not found");
    }
    return flag.get();
  }

  private void validateFlag(Flag flag) throws InvalidFeatureFlagException {
    var errors = validator.validate(flag);
    if (!errors.isEmpty()) {
      var messages = new ArrayList<>();
      for (var error : errors) {
        var message = error.getPropertyPath() + ": " + error.getMessage();
        messages.add(message);
      }
      throw new InvalidFeatureFlagException("Invalid feature flag:" + messages);
    }
  }
}
