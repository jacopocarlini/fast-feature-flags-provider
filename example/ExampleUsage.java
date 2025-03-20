package it.jacopocarlini.fffp;

import dev.openfeature.sdk.Client;
import dev.openfeature.sdk.MutableContext;
import dev.openfeature.sdk.OpenFeatureAPI;
import it.jacopocarlini.fffp.entity.Flag;
import it.jacopocarlini.fffp.providers.MongoDBFeatureFlagProvider;
import java.util.HashMap;

public class ExampleUsage {

  public static void main(String[] args) {
    OpenFeatureAPI openFeatureAPI = OpenFeatureAPI.getInstance();
    MongoDBFeatureFlagProvider provider =
        new MongoDBFeatureFlagProvider(
            "mongodb://admin:adminpassword@localhost:27017/openfeature?authSource=admin");
    openFeatureAPI.setProvider(provider);
    provider.deleteFlag("font");
    Flag flag =
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
//                        .timeWindowStart(ZonedDateTime.of(2025, 1, 1, 0, 0))
            //            .timeWindowEnd(ZonedDateTime.of(2026, 1, 2, 0, 0))
            //            .target(
            //                Collections.singletonList(
            //                    Target.builder().filter(".*@email.it").variant("big").build()))
            .rolloutPercentage(
                new HashMap<>() {
                  {
                    put("small", 50);
                    put("medium", 50);
                    put("big", 0);
                  }
                })
            .defaultVariant("medium")
            .build();
    provider.crateFlag(flag);
    provider.updateFlag("font", flag.toBuilder().rolloutPercentage(null).build());
    Client client = openFeatureAPI.getClient();
    //    var value = client.getStringValue("font", "0");
    var value =
        client.getStringValue("font", "0", new MutableContext().setTargetingKey("jacopo@email.it"));
    System.out.println(value);
  }
}
