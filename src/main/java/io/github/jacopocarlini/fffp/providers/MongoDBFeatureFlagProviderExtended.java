package io.github.jacopocarlini.fffp.providers;

import static io.github.jacopocarlini.fffp.util.ProviderUtility.*;

import io.github.jacopocarlini.fffp.entity.Flag;
import io.github.jacopocarlini.fffp.exceptions.InvalidFeatureFlagException;
import dev.openfeature.sdk.*;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import java.util.ArrayList;
import java.util.List;

public class MongoDBFeatureFlagProviderExtended extends MongoDBFeatureFlagProvider {

  public MongoDBFeatureFlagProviderExtended(String connectionString) {
    super(connectionString);
  }

  @Override
  public Metadata getMetadata() {
    return () -> "MongoDBFeatureFlagProviderExtended";
  }

  public List<Flag> getFlags() {
    return super.flagRepository.findAll();
  }

  public Flag getFlag(String flagKey) throws InvalidFeatureFlagException {
    return getFlagIfIsPresent(flagKey);
  }

  public void crateFlag(Flag flag) throws InvalidFeatureFlagException {
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
      super.assignedTargetRepository.deleteAllByFlagKey(flagKey);
    }

    flagRepository.save(newFlag);
  }

  public void deleteFlag(String flagKey) throws InvalidFeatureFlagException {
    var flag = flagRepository.findFirstByFlagKey(flagKey);
    if (flag.isEmpty()) {
      throw new InvalidFeatureFlagException("Flag not found");
    }
    assignedTargetRepository.deleteAllByFlagKey(flagKey);
    flagRepository.deleteByFlagKey(flagKey);
  }

  private Flag getFlagIfIsPresent(String flagKey) throws InvalidFeatureFlagException {
    var flag = flagRepository.findFirstByFlagKey(flagKey);
    if (flag.isEmpty()) {
      throw new InvalidFeatureFlagException("Flag not found");
    }
    return flag.get();
  }

  private static void validateFlag(Flag flag) throws InvalidFeatureFlagException {
    Validator validator;
    try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
      validator = factory.getValidator();
    }
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
