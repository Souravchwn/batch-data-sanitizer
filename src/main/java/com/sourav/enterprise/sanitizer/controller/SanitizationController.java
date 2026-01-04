package com.sourav.enterprise.sanitizer.controller;

import com.sourav.enterprise.sanitizer.domain.model.SanitizationConfig;
import com.sourav.enterprise.sanitizer.dto.JobResponse;
import com.sourav.enterprise.sanitizer.dto.SanitizationRequest;
import com.sourav.enterprise.sanitizer.service.FileStorageService;
import com.sourav.enterprise.sanitizer.service.SanitizationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/v1")
public class SanitizationController {
    private static final Logger log = LoggerFactory.getLogger(SanitizationController.class);

    private final SanitizationService sanitizationService;
    private final FileStorageService fileStorageService;
    private final ObjectMapper objectMapper;

    public SanitizationController(SanitizationService sanitizationService,
            FileStorageService fileStorageService, ObjectMapper objectMapper) {
        this.sanitizationService = sanitizationService;
        this.fileStorageService = fileStorageService;
        this.objectMapper = objectMapper;
    }

    @PostMapping(value = "/sanitize", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<JobResponse> startSanitization(
            @RequestParam("file") MultipartFile file,
            @RequestParam("config") String configJson) throws Exception {
        log.info("Received request: file={}, size={}", file.getOriginalFilename(), file.getSize());

        SanitizationRequest request = objectMapper.readValue(configJson, SanitizationRequest.class);
        SanitizationConfig config = SanitizationConfig.builder().columns(request.getColumns()).build();

        String inputPath = fileStorageService.saveInputFile(file);
        String outputPath = fileStorageService.generateOutputPath(inputPath);

        JobExecution execution = sanitizationService.startJob(inputPath, outputPath, config);
        return ResponseEntity.accepted().body(mapToResponse(execution, inputPath, outputPath));
    }

    @GetMapping("/jobs/{jobExecutionId}")
    public ResponseEntity<JobResponse> getJobStatus(@PathVariable Long jobExecutionId) {
        JobExecution execution = sanitizationService.getJobStatus(jobExecutionId);
        if (execution == null)
            return ResponseEntity.notFound().build();
        String inputFile = execution.getJobParameters().getString("inputFile");
        String outputFile = execution.getJobParameters().getString("outputFile");
        return ResponseEntity.ok(mapToResponse(execution, inputFile, outputFile));
    }

    @PostMapping("/jobs/{jobExecutionId}/restart")
    public ResponseEntity<JobResponse> restartJob(@PathVariable Long jobExecutionId) {
        JobExecution execution = sanitizationService.restartJob(jobExecutionId);
        String inputFile = execution.getJobParameters().getString("inputFile");
        String outputFile = execution.getJobParameters().getString("outputFile");
        return ResponseEntity.accepted().body(mapToResponse(execution, inputFile, outputFile));
    }

    @PostMapping("/jobs/{jobExecutionId}/stop")
    public ResponseEntity<Void> stopJob(@PathVariable Long jobExecutionId) {
        sanitizationService.stopJob(jobExecutionId);
        return ResponseEntity.accepted().build();
    }

    private JobResponse mapToResponse(JobExecution execution, String inputFile, String outputFile) {
        long readCount = 0, skipCount = 0;
        for (StepExecution step : execution.getStepExecutions()) {
            readCount += step.getReadCount();
            skipCount += step.getSkipCount();
        }
        return JobResponse.builder()
                .jobExecutionId(execution.getId())
                .jobName(execution.getJobInstance().getJobName())
                .status(execution.getStatus().toString())
                .inputFile(inputFile)
                .outputFile(outputFile)
                .startTime(execution.getStartTime())
                .endTime(execution.getEndTime())
                .rowsProcessed(readCount)
                .rowsSkipped(skipCount)
                .exitDescription(execution.getExitStatus().getExitDescription())
                .build();
    }

    @GetMapping("/jobs/{jobExecutionId}/download")
    public ResponseEntity<Resource> downloadResult(@PathVariable Long jobExecutionId) {
        JobExecution execution = sanitizationService.getJobStatus(jobExecutionId);
        if (execution == null) {
            return ResponseEntity.notFound().build();
        }

        String outputFilePath = execution.getJobParameters().getString("outputFile");
        if (outputFilePath == null) {
            return ResponseEntity.notFound().build();
        }

        File file = new File(outputFilePath);
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }

        String filename = Paths.get(outputFilePath).getFileName().toString();
        Resource resource = new FileSystemResource(file);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .contentLength(file.length())
                .body(resource);
    }
}
