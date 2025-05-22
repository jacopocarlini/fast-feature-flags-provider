package io.github.jacopocarlini.fffp.providers;

import static io.github.jacopocarlini.fffp.util.ProviderUtility.*;
import static io.github.jacopocarlini.fffp.util.Reason.*;

import dev.openfeature.sdk.*;
import io.github.jacopocarlini.fffp.config.FlagEngineConfig;
import io.github.jacopocarlini.fffp.entity.AssignedTarget;
import io.github.jacopocarlini.fffp.entity.Flag;
import io.github.jacopocarlini.fffp.exceptions.InvalidFeatureFlagException;
import io.github.jacopocarlini.fffp.repository.AssignedTargetRepository;
import io.github.jacopocarlini.fffp.repository.FlagRepository;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
public class MongoDBFeatureFlagProvider extends EventProvider {

  protected final AssignedTargetRepository assignedTargetRepository;
  protected final FlagRepository flagRepository;
  protected final FlagService flagService;

  public MongoDBFeatureFlagProvider(String connectionString) {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.getEnvironment().getSystemProperties().put("mongo.connection", connectionString);
    context.register(FlagEngineConfig.class);
    context.refresh();
    this.flagRepository = context.getBean(FlagRepository.class);
    this.assignedTargetRepository = context.getBean(AssignedTargetRepository.class);
    this.flagService = context.getBean(FlagService.class);
  }

  @Override
  public void shutdown() {
    super.shutdown();
    //    mongoClientManager.shutdown();
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

  private <T> ProviderEvaluation<T> evaluation(
      String flagKey, T defaultValue, EvaluationContext ctx, Class<T> valueType) {
    return flagService.evaluation(flagKey, defaultValue, ctx, valueType);
  }
}
