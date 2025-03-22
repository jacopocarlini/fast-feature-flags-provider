package io.github.jacopocarlini.fffp.util;

import static io.github.jacopocarlini.fffp.util.Reason.TARGET_MATCHED;

import io.github.jacopocarlini.fffp.entity.Flag;
import io.github.jacopocarlini.fffp.entity.Target;
import io.github.jacopocarlini.fffp.exceptions.InvalidFeatureFlagException;
import dev.openfeature.sdk.EvaluationContext;
import dev.openfeature.sdk.ProviderEvaluation;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ProviderUtility {
  private static final Random RANDOM = new Random();
  public static void checkRolloutPercentage(Flag flag) throws InvalidFeatureFlagException {
    if (flag.getRolloutPercentage() == null) {
      return;
    }
    int percentage = 0;
    for (var entry : flag.getRolloutPercentage().entrySet()) {
      percentage += entry.getValue();
    }
    if (percentage != 100 && percentage != 0) {
      throw new InvalidFeatureFlagException(
          "Rollout percentage not valid. Sum of percentage must be 0 or 100");
    }
  }

  public static void checkVariant(Flag flag) throws InvalidFeatureFlagException {
    if (flag.getVariants().isEmpty()) {
      throw new InvalidFeatureFlagException("Variants not present. Add at least one variant");
    }
    var defaultNotPresent = flag.getVariants().get(flag.getDefaultVariant()) == null;
    if (defaultNotPresent) {
      throw new InvalidFeatureFlagException("Default not present. Set a default variant");
    }
  }

  public static boolean isOutsideTimeWindow(Flag flag) {
    if (flag.getTimeWindowStart() == null) {
      return false;
    }

    ZonedDateTime now = ZonedDateTime.now();
    return now.isBefore(flag.getTimeWindowStart()) || now.isAfter(flag.getTimeWindowEnd());
  }

  public static <T> Optional<ProviderEvaluation<T>> checkTargetMatch(
      Flag flag, EvaluationContext ctx, Class<T> valueType) throws InvalidFeatureFlagException {
    if (flag.getTarget() == null || ctx.getTargetingKey() == null) {
      return Optional.empty();
    }

    for (Target target : flag.getTarget()) {
      Pattern pattern = Pattern.compile(target.getFilter());
      if (pattern.matcher(ctx.getTargetingKey()).find()) {
        Object value = flag.getVariants().get(target.getVariant());

        return Optional.of(
            ProviderEvaluation.<T>builder()
                .value(convertValue(value, valueType))
                .reason(TARGET_MATCHED.name())
                .variant(target.getVariant())
                .build());
      }
    }

    return Optional.empty();
  }

  public static String determineVariantForRollout(Flag flag) {
    int randomValue = RANDOM.nextInt(100);
    String variant = flag.getDefaultVariant();
    int cumulativePercentage = 0;

    for (Map.Entry<String, Integer> entry : flag.getRolloutPercentage().entrySet()) {
      cumulativePercentage += entry.getValue();
      if (randomValue < cumulativePercentage) {
        variant = entry.getKey();
        break;
      }
    }

    return variant;
  }

  @SuppressWarnings("unchecked")
  public static <T> T convertValue(Object value, Class<T> targetType) throws InvalidFeatureFlagException {
    if (value == null) {
      throw new InvalidFeatureFlagException("value is null");
    }

    if (targetType.isInstance(value)) {
      return (T) value;
    }

    throw new ClassCastException(
        "Cannot convert value of type "
            + value.getClass().getName()
            + " to "
            + targetType.getName());
  }
}
