package com.sourav.enterprise.sanitizer.dto;

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
public class SanitizationRequest {
    @NotEmpty(message = "At least one column rule is required")
    private Map<String, SanitizationOperation> columns;
}
