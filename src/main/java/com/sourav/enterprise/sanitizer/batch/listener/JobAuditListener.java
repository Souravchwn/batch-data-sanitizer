package com.sourav.enterprise.sanitizer.batch.listener;

import com.sourav.enterprise.sanitizer.domain.entity.JobAudit;
import com.sourav.enterprise.sanitizer.domain.enums.JobStatus;
import com.sourav.enterprise.sanitizer.repository.JobAuditRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Job Audit Listener - Records job execution details for audit and history.
 * 
 * Features:
 * - Creates audit record when job starts
 * - Updates audit with final statistics when job completes
 * - Calculates processing rate and duration
 * - Captures error messages for failed jobs
 */
@Component
public class JobAuditListener implements JobExecutionListener {
    private static final Logger log = LoggerFactory.getLogger(JobAuditListener.class);

    private final JobAuditRepository auditRepository;
    private final ObjectMapper objectMapper;

    public JobAuditListener(JobAuditRepository auditRepository, ObjectMapper objectMapper) {
        this.auditRepository = auditRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void beforeJob(JobExecution jobExecution) {
        String jobName = jobExecution.getJobInstance().getJobName();
        Long jobId = jobExecution.getId();

        log.info("🚀 Starting job: {} [ID: {}]", jobName, jobId);

        String inputFileName = jobExecution.getJobParameters().getString("inputFile");
        String rulesJson = jobExecution.getJobParameters().getString("rulesJson");
        String outputFileName = jobExecution.getJobParameters().getString("outputFile");
        String jobUuid = jobExecution.getJobParameters().getString("jobId");

        JobAudit audit = JobAudit.builder()
                .jobExecutionId(jobId)
                .jobUuid(jobUuid)
                .jobName(jobName)
                .inputFileName(inputFileName)
                .outputFileName(outputFileName)
                .rulesApplied(rulesJson)
                .startTime(jobExecution.getStartTime())
                .status(JobStatus.RUNNING)
                .build();

        // Parse column count from rules
        try {
            if (rulesJson != null) {
                var config = objectMapper.readTree(rulesJson);
                if (config.has("columns")) {
                    audit.setColumnsSanitized(config.get("columns").size());
                }
            }
        } catch (Exception e) {
            log.warn("Could not parse rules for column count");
        }

        auditRepository.save(audit);
        log.debug("Audit record created for job {}", jobId);
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        Long jobId = jobExecution.getId();
        JobStatus status = mapBatchStatus(jobExecution.getStatus());

        auditRepository.findByJobExecutionId(jobId).ifPresent(audit -> {
            LocalDateTime endTime = jobExecution.getEndTime();
            audit.setEndTime(endTime);
            audit.setStatus(status);

            // Aggregate statistics from all steps
            long totalRead = 0;
            long totalSkipped = 0;
            for (StepExecution step : jobExecution.getStepExecutions()) {
                totalRead += step.getReadCount();
                totalSkipped += step.getSkipCount();
            }
            audit.setRowsProcessed(totalRead);
            audit.setRowsSkipped(totalSkipped);

            // Calculate duration and processing rate
            if (audit.getStartTime() != null && endTime != null) {
                long durationMs = Duration.between(audit.getStartTime(), endTime).toMillis();
                audit.setDurationMs(durationMs);

                if (durationMs > 0 && totalRead > 0) {
                    double rate = (totalRead * 1000.0) / durationMs;
                    audit.setProcessingRate(rate);
                }
            }

            // Capture error message for failed jobs
            if (jobExecution.getStatus() == BatchStatus.FAILED) {
                StringBuilder errors = new StringBuilder();
                for (Throwable t : jobExecution.getAllFailureExceptions()) {
                    errors.append(t.getMessage()).append("\n");
                }
                audit.setErrorMessage(errors.toString().trim());
            }

            auditRepository.save(audit);

            // Log completion summary
            String icon = status == JobStatus.SUCCESS ? "✅" : "❌";
            log.info("{} Job {} completed: status={}, rows={}, duration={}ms, rate={} rows/sec",
                    icon, jobId, status, totalRead,
                    audit.getDurationMs(),
                    audit.getProcessingRate() != null ? String.format("%.1f", audit.getProcessingRate()) : "N/A");
        });
    }

    private JobStatus mapBatchStatus(BatchStatus status) {
        return switch (status) {
            case COMPLETED -> JobStatus.SUCCESS;
            case FAILED -> JobStatus.FAILED;
            case STOPPED -> JobStatus.STOPPED;
            case ABANDONED -> JobStatus.ABANDONED;
            default -> JobStatus.RUNNING;
        };
    }
}
