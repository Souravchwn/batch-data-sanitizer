package com.sourav.enterprise.sanitizer.dto;

import com.sourav.enterprise.sanitizer.domain.enums.JobStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditResponse {
    private Long id;
    private Long jobExecutionId;
    private String jobUuid;
    private String jobName;
    private String inputFileName;
    private String outputFileName;
    private Long rowsProcessed;
    private Long rowsSkipped;
    private Integer columnsSanitized;
    private String rulesApplied;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private JobStatus status;
    private Long durationMs;
    private Double processingRate;
    private String errorMessage;
}
