package com.sourav.enterprise.sanitizer.service;

import com.sourav.enterprise.sanitizer.domain.model.SanitizationConfig;
import org.springframework.batch.core.JobExecution;

public interface SanitizationService {
    JobExecution startJob(String inputFilePath, String outputFilePath, SanitizationConfig config);

    JobExecution restartJob(Long jobExecutionId);

    void stopJob(Long jobExecutionId);

    JobExecution getJobStatus(Long jobExecutionId);
}
