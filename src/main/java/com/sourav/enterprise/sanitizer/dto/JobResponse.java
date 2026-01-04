package com.sourav.enterprise.sanitizer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobResponse {
    private Long jobExecutionId;
    private String jobName;
    private String status;
    private String inputFile;
    private String outputFile;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long rowsProcessed;
    private Long rowsSkipped;
    private String exitDescription;
}
