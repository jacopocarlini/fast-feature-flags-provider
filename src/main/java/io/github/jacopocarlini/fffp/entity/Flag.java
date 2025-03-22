package io.github.jacopocarlini.fffp.entity;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Builder(toBuilder = true)
@Data
@AllArgsConstructor(access = AccessLevel.PUBLIC)
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@Document(collection = "flags")
public class Flag {

  @Id private String id;

  @Indexed(unique = true)
  @NotBlank
  private String flagKey;

  @NotNull private Boolean enabled;

  @Size(min = 1)
  private Map<String, Object> variants;

  @NotBlank private String defaultVariant;

  private List<Target> target;

  private Map<String, Integer> rolloutPercentage;

  private ZonedDateTime timeWindowStart;
  private ZonedDateTime timeWindowEnd;
}
