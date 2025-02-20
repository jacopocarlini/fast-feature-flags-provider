package it.jacopocarlini.fffp.entity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Builder(toBuilder = true)
@Data
@AllArgsConstructor
@ToString
@Document(collection = "flags")
public class Flag {

  @Id private String id;

  @Indexed(unique = true)
  private String flagKey;

  private Boolean enabled;

  private Map<String, Object> variants;
  private String defaultVariant;

  private List<Target> target;

  private Map<String, Integer> rolloutPercentage;

  private LocalDateTime timeWindowStart;
  private LocalDateTime timeWindowEnd;
}
