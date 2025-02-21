# Fast Feature-Flags Provider
An [open feature](https://openfeature.dev/) provider to operate with MongoDB

---

# Pre-requisites
- MongoDB running (see for an example in `examples/docker-compose.yml`) )
- import the dependency in your `pom.xml` and add the GitHub repository. You need a PAT token with read packages scope.
``` xml
    <dependency>
        <groupId>com.github.jacopocarlini</groupId>
        <artifactId>fast-feature-flags-provider</artifactId>
        <version>1.0.0</version>
    </dependency>

...
<repositories>
    <repository>
        <id>github</id>
        <url>https://public:${env.YOUR_GITHUB_TOKEN_READ_PACKAGES}@maven.pkg.github.com/jacopocarlini/fast-feature-flags-provider</url>
    </repository>
</repositories>
```

# How To Use

In your Java class:
``` java
  import it.jacopocarlini.fffp.providers.MongoDBFeatureFlagProvider;
  ...
  
  public static void main(String[] args) {
    OpenFeatureAPI openFeatureAPI = OpenFeatureAPI.getInstance();
    
    // set the connection string
    MongoDBFeatureFlagProvider provider =
        new MongoDBFeatureFlagProvider(
            "mongodb://user:adminpassword@localhost:27017/openfeature?authSource=admin");
    openFeatureAPI.setProvider(provider);
    
    // create a flag
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
            .defaultVariant("medium")
            .build();
    provider.crateFlag(flag);
    
    // evaluate
    Client client = openFeatureAPI.getClient();
    var value =
        client.getStringValue("font", "10");
    
    System.out.println(value);
  }
}
```

If the flag is not enabled the value will be the `defaultValue` passed in `client.getStringValue("font", "10")`. 

## Set a Time Window
You can set a time window to evaluate the flag. 
If the current time is outside the time window the flag will always be  disabled.
``` java
Flag.builder()
    ...
    .timeWindowStart(LocalDateTime.of(2025, 1, 1, 0, 0))
    .timeWindowEnd(LocalDateTime.of(2026, 1, 2, 0, 0))
    .build();
```

## Set a Target

You can force a variant specifying a regex to match the targeting key.

``` java
Flag.builder()
    ...
    .defaultVariant("medium")
    .target(
        Collections.singletonList(
            Target.builder()
                .filter(".*@email.it")
                .variant("big")
                .build()
        )
    )
    .build();
    
 client.getStringValue("font", "0", new MutableContext().setTargetingKey("nickname@email.it"));
))
```

## Set a Rollout Percentage
You can set a rollout percentage foreach variant.

``` java
Flag.builder()
  ...
  .rolloutPercentage(
            new HashMap<>() {
              {
                put("small", 50);
                put("medium", 50);
                put("big", 0);
              }
            })
    .build();
```

> **_NOTE:_** When a target key is provided in the context, 
> the random assigned variant is persisted and used on subsequent evaluations, overriding the rollout percentage.
> When the rollout percentage is modified or deleted, the assigned variant is also removed.
