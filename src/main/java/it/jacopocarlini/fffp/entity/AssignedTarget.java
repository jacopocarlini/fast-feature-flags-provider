package it.jacopocarlini.fffp.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Builder
@Data
@ToString
@AllArgsConstructor
@Document(collection = "assignedTarget")
public class AssignedTarget {

  @Indexed(unique = true)
  private String flagKey;

  @Indexed(unique = true)
  private String targetKey;

  private String variant;
}
