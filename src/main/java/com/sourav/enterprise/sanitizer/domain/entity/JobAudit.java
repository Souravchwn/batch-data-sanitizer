package com.sourav.enterprise.sanitizer.domain.entity;

import com.sourav.enterprise.sanitizer.domain.enums.JobStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "job_audit")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobAudit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long jobExecutionId;

    private String jobUuid;
    private String jobName;

    @Column(nullable = false)
    private String inputFileName;

    private String outputFileName;

    @Builder.Default
    private Long rowsProcessed = 0L;

    @Builder.Default
    private Long rowsSkipped = 0L;

    private Integer columnsSanitized;

    @Column(columnDefinition = "TEXT")
    private String rulesApplied;

    @Column(nullable = false)
    private LocalDateTime startTime;

    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    private Long durationMs;
    private Double processingRate;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        if (startTime != null && endTime != null) {
            durationMs = java.time.Duration.between(startTime, endTime).toMillis();
            if (durationMs > 0 && rowsProcessed != null && rowsProcessed > 0) {
                processingRate = (rowsProcessed * 1000.0) / durationMs;
            }
        }
    }
}
