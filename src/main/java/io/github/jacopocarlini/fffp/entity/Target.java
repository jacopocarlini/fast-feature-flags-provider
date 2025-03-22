package io.github.jacopocarlini.fffp.entity;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Builder(toBuilder = true)
@Data
@AllArgsConstructor(access = AccessLevel.PUBLIC)
@NoArgsConstructor(access = AccessLevel.PUBLIC)
public class Target {

  @NotBlank
  private String filter;
  @NotBlank
  private String variant;
}
