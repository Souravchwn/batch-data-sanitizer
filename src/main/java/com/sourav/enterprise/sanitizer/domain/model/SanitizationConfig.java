package com.sourav.enterprise.sanitizer.domain.model;

import com.sourav.enterprise.sanitizer.domain.enums.SanitizationOperation;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SanitizationConfig {
    @NotEmpty(message = "At least one column rule must be specified")
    private Map<String, SanitizationOperation> columns;
}
