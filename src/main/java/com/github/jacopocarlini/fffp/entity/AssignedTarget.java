package com.github.jacopocarlini.fffp.entity;

import lombok.*;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Builder(toBuilder = true)
@Data
@AllArgsConstructor(access = AccessLevel.PUBLIC)
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@Document(collection = "assignedTarget")
public class AssignedTarget {

  @Indexed(unique = true)
  private String flagKey;

  @Indexed(unique = true)
  private String targetKey;

  private String variant;
}
