package com.sourav.enterprise.sanitizer.service.impl;

import com.sourav.enterprise.sanitizer.batch.config.JobConfig;
import com.sourav.enterprise.sanitizer.batch.config.SanitizationJobFactory;
import com.sourav.enterprise.sanitizer.domain.model.SanitizationConfig;
import com.sourav.enterprise.sanitizer.exception.InvalidConfigurationException;
import com.sourav.enterprise.sanitizer.exception.JobExecutionException;
import com.sourav.enterprise.sanitizer.service.SanitizationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.*;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Sanitization Service Implementation - Orchestrates batch sanitization jobs.
 * 
 * Responsibilities:
 * - Validates job configurations
 * - Creates and launches jobs using the factory
 * - Handles job restart and stop operations
 * - Provides job status queries
 */
@Service
public class SanitizationServiceImpl implements SanitizationService {
    private static final Logger log = LoggerFactory.getLogger(SanitizationServiceImpl.class);

    private final JobLauncher jobLauncher;
    private final JobExplorer jobExplorer;
    private final JobOperator jobOperator;
    private final SanitizationJobFactory jobFactory;
    private final ObjectMapper objectMapper;

    @Value("${sanitizer.batch.chunk-size:1000}")
    private int chunkSize;

    @Value("${sanitizer.batch.skip-limit:100}")
    private int skipLimit;

    public SanitizationServiceImpl(JobLauncher jobLauncher, JobExplorer jobExplorer,
            JobOperator jobOperator, SanitizationJobFactory jobFactory,
            ObjectMapper objectMapper) {
        this.jobLauncher = jobLauncher;
        this.jobExplorer = jobExplorer;
        this.jobOperator = jobOperator;
        this.jobFactory = jobFactory;
        this.objectMapper = objectMapper;
    }

    @Override
    public JobExecution startJob(String inputFilePath, String outputFilePath, SanitizationConfig config) {
        // Validate inputs
        validateConfig(config);
        validateInputFile(inputFilePath);

        try {
            // Create job configuration with UUID
            String rulesJson = objectMapper.writeValueAsString(config);
            JobConfig jobConfig = JobConfig.create(
                    inputFilePath, outputFilePath,
                    config.getColumns(), rulesJson,
                    chunkSize, skipLimit);

            log.info("📋 {}", jobConfig.getDescription());

            // Build job parameters
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("inputFile", inputFilePath)
                    .addString("outputFile", outputFilePath)
                    .addString("rulesJson", rulesJson)
                    .addString("jobId", jobConfig.getJobId())
                    .addString("timestamp", LocalDateTime.now().format(
                            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")))
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();

            // Create and launch job
            Job job = jobFactory.createJob(jobConfig, config);
            return jobLauncher.run(job, jobParameters);

        } catch (Exception e) {
            log.error("❌ Failed to start job: {}", e.getMessage());
            throw new JobExecutionException("Failed to start job", e);
        }
    }

    @Override
    public JobExecution restartJob(Long jobExecutionId) {
        try {
            JobExecution original = jobExplorer.getJobExecution(jobExecutionId);
            if (original == null) {
                throw new JobExecutionException("Job not found: " + jobExecutionId);
            }

            if (original.getStatus() != BatchStatus.FAILED &&
                    original.getStatus() != BatchStatus.STOPPED) {
                throw new JobExecutionException("Cannot restart job with status: " + original.getStatus());
            }

            JobParameters params = original.getJobParameters();
            SanitizationConfig config = objectMapper.readValue(
                    params.getString("rulesJson"), SanitizationConfig.class);

            // Create new job config for restart
            JobConfig jobConfig = JobConfig.create(
                    params.getString("inputFile"),
                    params.getString("outputFile"),
                    config.getColumns(),
                    params.getString("rulesJson"),
                    chunkSize, skipLimit);

            JobParameters newParams = new JobParametersBuilder(params)
                    .addLong("restart.time", System.currentTimeMillis())
                    .toJobParameters();

            Job job = jobFactory.createJob(jobConfig, config);
            log.info("🔄 Restarting job {} as {}", jobExecutionId, jobConfig.getJobName());

            return jobLauncher.run(job, newParams);

        } catch (Exception e) {
            throw new JobExecutionException("Failed to restart job", e);
        }
    }

    @Override
    public void stopJob(Long jobExecutionId) {
        try {
            JobExecution execution = jobExplorer.getJobExecution(jobExecutionId);
            if (execution == null) {
                throw new JobExecutionException("Job not found: " + jobExecutionId);
            }
            if (!execution.isRunning()) {
                throw new JobExecutionException("Job is not running");
            }

            log.info("⏹️ Stopping job {}", jobExecutionId);
            jobOperator.stop(jobExecutionId);

        } catch (Exception e) {
            throw new JobExecutionException("Failed to stop job", e);
        }
    }

    @Override
    public JobExecution getJobStatus(Long jobExecutionId) {
        return jobExplorer.getJobExecution(jobExecutionId);
    }

    private void validateConfig(SanitizationConfig config) {
        if (config == null || config.getColumns() == null || config.getColumns().isEmpty()) {
            throw new InvalidConfigurationException("At least one column rule is required");
        }
    }

    private void validateInputFile(String inputFilePath) {
        File inputFile = new File(inputFilePath);
        if (!inputFile.exists()) {
            throw new InvalidConfigurationException("Input file not found: " + inputFilePath);
        }
        if (!inputFile.canRead()) {
            throw new InvalidConfigurationException("Cannot read input file: " + inputFilePath);
        }
    }
}
