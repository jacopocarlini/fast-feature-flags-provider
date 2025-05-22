package io.github.jacopocarlini.fffp;

import dev.openfeature.sdk.Client;
import dev.openfeature.sdk.MutableContext;
import dev.openfeature.sdk.OpenFeatureAPI;
import io.github.jacopocarlini.fffp.config.FlagEngineConfig;
import io.github.jacopocarlini.fffp.entity.Flag;
import io.github.jacopocarlini.fffp.providers.MongoDBFeatureFlagProvider;
import io.github.jacopocarlini.fffp.providers.MongoDBFeatureFlagProviderExtended;
import io.github.jacopocarlini.fffp.repository.FlagRepository;
import lombok.SneakyThrows;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.HashMap;

public class Main {
    @SneakyThrows
    public static void main(String[] args) {
        System.out.println("Hello world!");
        OpenFeatureAPI openFeatureAPI = OpenFeatureAPI.getInstance();
        MongoDBFeatureFlagProviderExtended provider =
                new MongoDBFeatureFlagProviderExtended(
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
//                .timeWindowStart(ZonedDateTime.of(LocalDateTime.now().minusDays(2), ZoneId.of("UTC")))
//                .timeWindowEnd(ZonedDateTime.of(LocalDateTime.now().minusDays(1), ZoneId.of("UTC")))
                        //            .target(
                        //                Collections.singletonList(
                        //                    Target.builder().filter(".*@email.it").variant("big").build()))
//                        .rolloutPercentage(
//                                new HashMap<>() {
//                                    {
//                                        put("small", 50);
//                                        put("medium", 50);
//                                        put("big", 0);
//                                    }
//                                })
                        .defaultVariant("medium")
                        .build();
        provider.createFlag(flag);
//        provider.updateFlag("font", flag.toBuilder().rolloutPercentage(null).build());
        Client client = openFeatureAPI.getClient();
        //    var value = client.getStringValue("font", "0");
        var value =
                client.getStringValue("font", "0", new MutableContext().setTargetingKey("user@email.it"));
        System.out.println(value);
    }
}
