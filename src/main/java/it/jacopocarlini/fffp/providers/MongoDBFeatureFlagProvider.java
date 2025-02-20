package it.jacopocarlini.fffp.providers;

import static it.jacopocarlini.fffp.ProviderUtility.*;

import dev.openfeature.sdk.*;
import it.jacopocarlini.fffp.config.MongoClientManager;
import it.jacopocarlini.fffp.entity.AssignedTarget;
import it.jacopocarlini.fffp.entity.Flag;
import it.jacopocarlini.fffp.exceptions.FeatureFlagEvaluationException;
import it.jacopocarlini.fffp.exceptions.InvalidFeatureFlagException;
import it.jacopocarlini.fffp.repository.AssignedTargetRepository;
import java.util.List;
import java.util.Optional;

import it.jacopocarlini.fffp.repository.FlagRepository;

public class MongoDBFeatureFlagProvider extends EventProvider {

  private static final String DEFAULT_VALUE = "default_value";
  private static final String FLAG_DISABLED = "flag is disabled";
  private static final String OUTSIDE_TIME_WINDOW = "outside time window";
  private static final String ALREADY_ASSIGNED = "already assigned";
  private static final String ROLLOUT = "rollout";
  public static final String FLAG_NOT_FOUND = "flag not found";

   private FlagRepository flagRepository;

   private AssignedTargetRepository assignedTargetRepository;

   private MongoClientManager mongoClientManager;

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
      throw new InvalidFeatureFlagException("conflict. flag already present");
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

    if (newFlag.getRolloutPercentage().equals(flag.getRolloutPercentage())) {
      assignedTargetRepository.deleteAllByFlagKey(flagKey);
    }

    flagRepository.save(newFlag);
  }

  public void deleteFlag(String flagKey) {
    var flag = flagRepository.findFirstByFlagKey(flagKey);
    if (flag.isEmpty()) {
      throw new InvalidFeatureFlagException(FLAG_NOT_FOUND);
    }
    assignedTargetRepository.deleteAllByFlagKey(flagKey);
    flagRepository.deleteByFlagKey(flagKey);
  }

  private <T> ProviderEvaluation<T> evaluation(
      String flagKey, T defaultValue, EvaluationContext ctx, Class<T> valueType) {
    try {
      Optional<Flag> optionalFlag = flagRepository.findFirstByFlagKey(flagKey);

      if (optionalFlag.isEmpty()) {
        return createDefaultEvaluation(defaultValue, FLAG_NOT_FOUND);
      }

      Flag flag = optionalFlag.get();

      // check if the flag is enabled
      if (!Boolean.TRUE.equals(flag.getEnabled())) {
        return createDefaultEvaluation(defaultValue, FLAG_DISABLED);
      }

      // check time window
      if (isOutsideTimeWindow(flag)) {
        return createDefaultEvaluation(defaultValue, OUTSIDE_TIME_WINDOW);
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
      throw new FeatureFlagEvaluationException("Value type mismatch for flag: " + flagKey, e);
    } catch (Exception e) {
      throw new FeatureFlagEvaluationException("Error evaluating flag: " + flagKey, e);
    }
  }

  private <T> ProviderEvaluation<T> createDefaultEvaluation(T defaultValue, String reason) {
    return ProviderEvaluation.<T>builder()
        .value(defaultValue)
        .reason(reason)
        .variant(DEFAULT_VALUE)
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
            .reason(ALREADY_ASSIGNED)
            .build();
      }

      // assign the user to the variant
      assignedTargetRepository.save(new AssignedTarget(flag.getFlagKey(), targetKey, variant));
    }
    return ProviderEvaluation.<T>builder()
        .value(convertValue(flag.getVariants().get(variant), valueType))
        .reason(ROLLOUT)
        .variant(variant)
        .build();
  }

  private Flag getFlagIfIsPresent(String flagKey) {
    var flag = flagRepository.findFirstByFlagKey(flagKey);
    if (flag.isEmpty()) {
      throw new InvalidFeatureFlagException(FLAG_NOT_FOUND);
    }
    return flag.get();
  }
}
