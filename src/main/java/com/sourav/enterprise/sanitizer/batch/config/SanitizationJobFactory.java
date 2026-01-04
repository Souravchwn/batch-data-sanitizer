package com.sourav.enterprise.sanitizer.batch.config;

import com.sourav.enterprise.sanitizer.batch.listener.ChunkProgressListener;
import com.sourav.enterprise.sanitizer.batch.listener.JobAuditListener;
import com.sourav.enterprise.sanitizer.batch.listener.SkipRecordListener;
import com.sourav.enterprise.sanitizer.batch.processor.SanitizationProcessor;
import com.sourav.enterprise.sanitizer.batch.reader.CsvItemReader;
import com.sourav.enterprise.sanitizer.batch.writer.CsvItemWriter;
import com.sourav.enterprise.sanitizer.domain.model.CsvRecord;
import com.sourav.enterprise.sanitizer.domain.model.SanitizationConfig;
import com.sourav.enterprise.sanitizer.strategy.SanitizationStrategyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Factory for creating Spring Batch Job and Step configurations.
 * Centralizes all batch job creation logic for better maintainability.
 */
@Component
public class SanitizationJobFactory {
    private static final Logger log = LoggerFactory.getLogger(SanitizationJobFactory.class);

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final SanitizationStrategyFactory strategyFactory;
    private final JobAuditListener jobAuditListener;

    public SanitizationJobFactory(JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            SanitizationStrategyFactory strategyFactory,
            JobAuditListener jobAuditListener) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
        this.strategyFactory = strategyFactory;
        this.jobAuditListener = jobAuditListener;
    }

    /**
     * Creates a complete sanitization job with all components configured.
     * 
     * @param jobConfig          The job configuration containing all parameters
     * @param sanitizationConfig The column sanitization rules
     * @return Configured Spring Batch Job ready for execution
     */
    public Job createJob(JobConfig jobConfig, SanitizationConfig sanitizationConfig) {
        log.info("Creating job: {} [{}]", jobConfig.getJobName(), jobConfig.getJobId());

        Step sanitizeStep = createSanitizationStep(jobConfig, sanitizationConfig);

        return new JobBuilder(jobConfig.getJobName(), jobRepository)
                .listener(jobAuditListener)
                .start(sanitizeStep)
                .build();
    }

    /**
     * Creates the main sanitization step with reader, processor, and writer.
     */
    private Step createSanitizationStep(JobConfig jobConfig, SanitizationConfig sanitizationConfig) {
        // Read headers from input file
        String[] headers = readHeaders(jobConfig.getInputFilePath());

        // Create components
        CsvItemReader reader = createReader(jobConfig.getInputFilePath());
        SanitizationProcessor processor = createProcessor(sanitizationConfig);
        CsvItemWriter writer = createWriter(jobConfig.getOutputFilePath(), headers);

        log.debug("Step config: chunkSize={}, skipLimit={}, columns={}",
                jobConfig.getChunkSize(), jobConfig.getSkipLimit(), headers.length);

        return new StepBuilder("sanitizeStep-" + jobConfig.getJobId(), jobRepository)
                .<CsvRecord, CsvRecord>chunk(jobConfig.getChunkSize(), transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .faultTolerant()
                .skipLimit(jobConfig.getSkipLimit())
                .skip(Exception.class)
                .listener(new SkipRecordListener())
                .listener(new ChunkProgressListener())
                .build();
    }

    /**
     * Creates a CSV item reader for the input file.
     */
    private CsvItemReader createReader(String inputFilePath) {
        return new CsvItemReader(inputFilePath);
    }

    /**
     * Creates the sanitization processor with configured strategies.
     */
    private SanitizationProcessor createProcessor(SanitizationConfig config) {
        return new SanitizationProcessor(config, strategyFactory);
    }

    /**
     * Creates a CSV item writer for the output file.
     */
    private CsvItemWriter createWriter(String outputFilePath, String[] headers) {
        return new CsvItemWriter(outputFilePath, headers);
    }

    /**
     * Reads CSV headers from the input file.
     */
    private String[] readHeaders(String inputFilePath) {
        CsvItemReader tempReader = new CsvItemReader(inputFilePath);
        try {
            tempReader.open(new ExecutionContext());
            return tempReader.getHeaders();
        } finally {
            tempReader.close();
        }
    }
}
