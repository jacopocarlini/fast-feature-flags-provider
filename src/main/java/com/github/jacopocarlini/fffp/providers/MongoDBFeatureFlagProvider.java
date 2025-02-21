package com.github.jacopocarlini.fffp.providers;


import com.github.jacopocarlini.fffp.config.MongoClientManager;
import com.github.jacopocarlini.fffp.entity.AssignedTarget;
import com.github.jacopocarlini.fffp.entity.Flag;
import com.github.jacopocarlini.fffp.repository.AssignedTargetRepository;
import com.github.jacopocarlini.fffp.repository.FlagRepository;
import dev.openfeature.sdk.*;
import java.util.List;
import java.util.Optional;

import static com.github.jacopocarlini.fffp.util.ProviderUtility.*;
import static com.github.jacopocarlini.fffp.util.Reason.*;

public class MongoDBFeatureFlagProvider extends EventProvider {

  private final FlagRepository flagRepository;

  private final AssignedTargetRepository assignedTargetRepository;

  private final MongoClientManager mongoClientManager;

  public MongoDBFeatureFlagProvider(String connectionString) {
    mongoClientManager = new MongoClientManager();
    mongoClientManager.updateConnection(connectionString);
    flagRepository = new FlagRepository(mongoClientManager);
    assignedTargetRepository = new AssignedTargetRepository(mongoClientManager);
  }

  @Override
  public void shutdown() {
    super.shutdown();
    mongoClientManager.shutdown();
  }

  @Override
  public Metadata getMetadata() {
    return () -> "MongoDBFeatureFlagProvider";
  }

  @Override
  public ProviderEvaluation<String> getStringEvaluation(
      String flagKey, String defaultValue, EvaluationContext ctx) {
    return evaluation(flagKey, defaultValue, ctx, String.class);
  }

  @Override
  public ProviderEvaluation<Boolean> getBooleanEvaluation(
      String flagKey, Boolean defaultValue, EvaluationContext ctx) {
    return evaluation(flagKey, defaultValue, ctx, Boolean.class);
  }

  @Override
  public ProviderEvaluation<Integer> getIntegerEvaluation(
      String flagKey, Integer defaultValue, EvaluationContext ctx) {
    return evaluation(flagKey, defaultValue, ctx, Integer.class);
  }

  @Override
  public ProviderEvaluation<Double> getDoubleEvaluation(
      String flagKey, Double defaultValue, EvaluationContext ctx) {
    return evaluation(flagKey, defaultValue, ctx, Double.class);
  }

  @Override
  public ProviderEvaluation<Value> getObjectEvaluation(
      String flagKey, Value defaultValue, EvaluationContext ctx) {
    return evaluation(flagKey, defaultValue, ctx, Value.class);
  }

  public List<Flag> getFlags() {
    return flagRepository.findAll();
  }

  public Flag getFlag(String flagKey) {
    return getFlagIfIsPresent(flagKey);
  }

  public void crateFlag(Flag flag) {
    var isPresent = flagRepository.findFirstByFlagKey(flag.getFlagKey()).isPresent();
    if (isPresent) {
      throw new it.jacopocarlini.fffp.exceptions.InvalidFeatureFlagException("Conflict. Flag already present");
    }

    checkRolloutPercentage(flag);
    checkVariant(flag);

    flagRepository.save(flag);
  }

  public void updateFlag(String flagKey, Flag newFlag) {
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

  public void deleteFlag(String flagKey) {
    var flag = flagRepository.findFirstByFlagKey(flagKey);
    if (flag.isEmpty()) {
      throw new it.jacopocarlini.fffp.exceptions.InvalidFeatureFlagException("Flag not found");
    }
    assignedTargetRepository.deleteAllByFlagKey(flagKey);
    flagRepository.deleteByFlagKey(flagKey);
  }

  private <T> ProviderEvaluation<T> evaluation(
      String flagKey, T defaultValue, EvaluationContext ctx, Class<T> valueType) {
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

    } catch (ClassCastException e) {
      throw new it.jacopocarlini.fffp.exceptions.FeatureFlagEvaluationException("Value type mismatch for flag: " + flagKey, e);
    } catch (Exception e) {
      throw new it.jacopocarlini.fffp.exceptions.FeatureFlagEvaluationException("Error evaluating flag: " + flagKey, e);
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
      Flag flag, EvaluationContext ctx, Class<T> valueType) {
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

  private Flag getFlagIfIsPresent(String flagKey) {
    var flag = flagRepository.findFirstByFlagKey(flagKey);
    if (flag.isEmpty()) {
      throw new it.jacopocarlini.fffp.exceptions.InvalidFeatureFlagException("Flag not found");
    }
    return flag.get();
  }
}
