package com.sourav.enterprise.sanitizer.controller;

import com.sourav.enterprise.sanitizer.dto.CsvPreviewResponse;
import com.sourav.enterprise.sanitizer.dto.DiffResponse;
import com.sourav.enterprise.sanitizer.repository.JobAuditRepository;
import com.sourav.enterprise.sanitizer.service.CsvPreviewService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1")
public class PreviewController {
    private final CsvPreviewService previewService;
    private final JobAuditRepository auditRepository;

    public PreviewController(CsvPreviewService previewService, JobAuditRepository auditRepository) {
        this.previewService = previewService;
        this.auditRepository = auditRepository;
    }

    @PostMapping(value = "/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CsvPreviewResponse> previewUploadedFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "rows", defaultValue = "20") int rows) {
        try {
            CsvPreviewResponse preview = previewService.previewFile(file, rows);
            return ResponseEntity.ok(preview);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/jobs/{jobExecutionId}/preview/input")
    public ResponseEntity<CsvPreviewResponse> previewInputFile(
            @PathVariable Long jobExecutionId,
            @RequestParam(value = "rows", defaultValue = "20") int rows) {
        var auditOpt = auditRepository.findByJobExecutionId(jobExecutionId);
        if (auditOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        try {
            CsvPreviewResponse preview = previewService.previewStoredFile(auditOpt.get().getInputFileName(), rows);
            return ResponseEntity.ok(preview);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/jobs/{jobExecutionId}/preview/output")
    public ResponseEntity<CsvPreviewResponse> previewOutputFile(
            @PathVariable Long jobExecutionId,
            @RequestParam(value = "rows", defaultValue = "20") int rows) {
        var auditOpt = auditRepository.findByJobExecutionId(jobExecutionId);
        if (auditOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        try {
            CsvPreviewResponse preview = previewService.previewStoredFile(auditOpt.get().getOutputFileName(), rows);
            return ResponseEntity.ok(preview);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/jobs/{jobExecutionId}/diff")
    public ResponseEntity<DiffResponse> getDiff(
            @PathVariable Long jobExecutionId,
            @RequestParam(value = "rows", defaultValue = "20") int rows) {
        var auditOpt = auditRepository.findByJobExecutionId(jobExecutionId);
        if (auditOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        try {
            DiffResponse diff = previewService.generateDiff(
                    auditOpt.get().getInputFileName(),
                    auditOpt.get().getOutputFileName(),
                    rows);
            return ResponseEntity.ok(diff);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
