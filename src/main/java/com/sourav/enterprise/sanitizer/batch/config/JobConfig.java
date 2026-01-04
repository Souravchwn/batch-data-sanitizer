package com.sourav.enterprise.sanitizer.batch.config;

import com.sourav.enterprise.sanitizer.domain.enums.SanitizationOperation;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Job Configuration Details - Immutable configuration for a sanitization job.
 * Contains all metadata about a job including unique identifiers and
 * parameters.
 */
@Data
@Builder
public class JobConfig {

    /** Unique job identifier (UUID) */
    private final String jobId;

    /** Human-readable job name */
    private final String jobName;

    /** Input file path */
    private final String inputFilePath;

    /** Output file path */
    private final String outputFilePath;

    /** Column sanitization rules (column name -> operation) */
    private final Map<String, SanitizationOperation> columnRules;

    /** JSON representation of the full configuration */
    private final String rulesJson;

    /** Timestamp when job was created */
    private final LocalDateTime createdAt;

    /** Chunk size for batch processing */
    private final int chunkSize;

    /** Maximum number of skippable errors */
    private final int skipLimit;

    /**
     * Creates a new JobConfig with auto-generated UUID and timestamp.
     */
    public static JobConfig create(String inputFilePath, String outputFilePath,
            Map<String, SanitizationOperation> columnRules, String rulesJson,
            int chunkSize, int skipLimit) {
        String uuid = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String jobName = "SANITIZE-" + uuid;

        return JobConfig.builder()
                .jobId(uuid)
                .jobName(jobName)
                .inputFilePath(inputFilePath)
                .outputFilePath(outputFilePath)
                .columnRules(columnRules)
                .rulesJson(rulesJson)
                .createdAt(LocalDateTime.now())
                .chunkSize(chunkSize)
                .skipLimit(skipLimit)
                .build();
    }

    /**
     * Gets a display-friendly description of the job.
     */
    public String getDescription() {
        String fileName = inputFilePath.contains("/")
                ? inputFilePath.substring(inputFilePath.lastIndexOf('/') + 1)
                : inputFilePath.substring(inputFilePath.lastIndexOf('\\') + 1);
        return String.format("Job %s: Sanitizing %s (%d columns)", jobId, fileName,
                columnRules != null ? columnRules.size() : 0);
    }
}
