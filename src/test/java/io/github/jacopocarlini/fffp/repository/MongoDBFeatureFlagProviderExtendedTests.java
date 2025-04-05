package io.github.jacopocarlini.fffp.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.openfeature.sdk.Client;
import dev.openfeature.sdk.MutableContext;
import dev.openfeature.sdk.OpenFeatureAPI;
import io.github.jacopocarlini.fffp.entity.Flag;
import io.github.jacopocarlini.fffp.entity.Target;
import io.github.jacopocarlini.fffp.exceptions.InvalidFeatureFlagException;
import io.github.jacopocarlini.fffp.providers.MongoDBFeatureFlagProviderExtended;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.HashMap;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

public class MongoDBFeatureFlagProviderExtendedTests {

  MongoDBFeatureFlagProviderExtended provider =
      new MongoDBFeatureFlagProviderExtended(
          "mongodb://admin:adminpassword@localhost:27017/openfeature?authSource=admin");

  @SneakyThrows
  @Test
  public void getStringValue_enabled() {
    eraseDB();
    provider.createFlag(
        Flag.builder()
            .flagKey("font")
            .enabled(true)
            .variants(
                new HashMap<>() {
                  {
                    put("small", "12");
                    put("medium", "14");
                    put("big", "18");
                  }
                })
            .defaultVariant("medium")
            .build());

    OpenFeatureAPI openFeatureAPI = OpenFeatureAPI.getInstance();
    openFeatureAPI.setProvider(provider);

    Client client = openFeatureAPI.getClient();
    var value = client.getStringValue("font", "0");
    assertEquals("14", value);
  }

  @SneakyThrows
  @Test
  public void getStringValue_disabled() {
    eraseDB();
    provider.createFlag(
        Flag.builder()
            .flagKey("font")
            .enabled(false)
            .variants(
                new HashMap<>() {
                  {
                    put("small", "12");
                    put("medium", "14");
                    put("big", "18");
                  }
                })
            .defaultVariant("medium")
            .build());

    OpenFeatureAPI openFeatureAPI = OpenFeatureAPI.getInstance();
    openFeatureAPI.setProvider(provider);

    Client client = openFeatureAPI.getClient();
    var value = client.getStringValue("font", "0");
    assertEquals("0", value);
  }


  @SneakyThrows
  @Test
  public void getStringValue_timewindow_out() {
    eraseDB();
    provider.createFlag(
        Flag.builder()
            .flagKey("font")
            .enabled(true)
            .variants(
                new HashMap<>() {
                  {
                    put("small", "12");
                    put("medium", "14");
                    put("big", "18");
                  }
                })
            .defaultVariant("medium")
            .timeWindowStart(LocalDateTime.now().minusDays(2).toInstant(ZoneId.systemDefault().getRules().getOffset(Instant.now())))
            .timeWindowEnd(LocalDateTime.now().minusDays(1).toInstant(ZoneId.systemDefault().getRules().getOffset(Instant.now())))
            .build());

    OpenFeatureAPI openFeatureAPI = OpenFeatureAPI.getInstance();
    openFeatureAPI.setProvider(provider);

    Client client = openFeatureAPI.getClient();
    var value = client.getStringValue("font", "0");
    assertEquals("0", value);
  }
  @SneakyThrows
  @Test
  public void getStringValue_timewindow_in() {
    eraseDB();
    provider.createFlag(
        Flag.builder()
            .flagKey("font")
            .enabled(true)
            .variants(
                new HashMap<>() {
                  {
                    put("small", "12");
                    put("medium", "14");
                    put("big", "18");
                  }
                })
            .defaultVariant("medium")
            .timeWindowStart(LocalDateTime.now().minusDays(2).toInstant(ZoneId.systemDefault().getRules().getOffset(Instant.now())))
            .timeWindowEnd(LocalDateTime.now().plusDays(1).toInstant(ZoneId.systemDefault().getRules().getOffset(Instant.now())))
            .build());

    OpenFeatureAPI openFeatureAPI = OpenFeatureAPI.getInstance();
    openFeatureAPI.setProvider(provider);

    Client client = openFeatureAPI.getClient();
    var value = client.getStringValue("font", "0");
    assertEquals("14", value);
  }

  @SneakyThrows
  @Test
  public void getStringValue_target() {
    eraseDB();
    provider.createFlag(
        Flag.builder()
            .flagKey("font")
            .enabled(true)
            .variants(
                new HashMap<>() {
                  {
                    put("small", "12");
                    put("medium", "14");
                    put("big", "18");
                  }
                })
            .defaultVariant("medium")
            .target(
                Collections.singletonList(
                    Target.builder().filter(".*@email.it").variant("big").build()))
            .build());

    OpenFeatureAPI openFeatureAPI = OpenFeatureAPI.getInstance();
    openFeatureAPI.setProvider(provider);

    Client client = openFeatureAPI.getClient();
    var value =
        client.getStringValue("font", "0", new MutableContext().setTargetingKey("jacopo@email.it"));
    assertEquals("18", value);
  }

  private void eraseDB() {
    try {
      provider.deleteFlag("font");
    } catch (InvalidFeatureFlagException e) {
      // ignored
    }
  }
}
