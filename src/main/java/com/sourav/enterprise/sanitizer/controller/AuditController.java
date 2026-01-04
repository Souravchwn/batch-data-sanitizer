package com.sourav.enterprise.sanitizer.controller;

import com.sourav.enterprise.sanitizer.domain.entity.JobAudit;
import com.sourav.enterprise.sanitizer.dto.AuditResponse;
import com.sourav.enterprise.sanitizer.repository.JobAuditRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/audits")
public class AuditController {
    private final JobAuditRepository auditRepository;

    public AuditController(JobAuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    @GetMapping
    public ResponseEntity<List<AuditResponse>> listAudits() {
        List<JobAudit> audits = auditRepository.findTop20ByOrderByStartTimeDesc();
        return ResponseEntity.ok(audits.stream().map(this::mapToResponse).collect(Collectors.toList()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AuditResponse> getAudit(@PathVariable Long id) {
        return auditRepository.findById(id).map(this::mapToResponse)
                .map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/job/{jobExecutionId}")
    public ResponseEntity<AuditResponse> getAuditByJobId(@PathVariable Long jobExecutionId) {
        return auditRepository.findByJobExecutionId(jobExecutionId).map(this::mapToResponse)
                .map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();
        try {
            stats.put("totalJobs", auditRepository.count());
            stats.put("successfulJobs", auditRepository.countByStatus(
                    com.sourav.enterprise.sanitizer.domain.enums.JobStatus.SUCCESS));
            stats.put("failedJobs", auditRepository.countByStatus(
                    com.sourav.enterprise.sanitizer.domain.enums.JobStatus.FAILED));
            Long totalRows = auditRepository.getTotalRowsProcessed();
            stats.put("totalRowsProcessed", totalRows != null ? totalRows : 0L);
            Double avgRate = auditRepository.getAverageProcessingRate();
            stats.put("averageProcessingRate", avgRate != null ? avgRate : 0.0);
        } catch (Exception e) {
            stats.put("totalJobs", 0L);
            stats.put("successfulJobs", 0L);
            stats.put("failedJobs", 0L);
            stats.put("totalRowsProcessed", 0L);
            stats.put("averageProcessingRate", 0.0);
        }
        return ResponseEntity.ok(stats);
    }

    private AuditResponse mapToResponse(JobAudit audit) {
        return AuditResponse.builder()
                .id(audit.getId())
                .jobExecutionId(audit.getJobExecutionId())
                .jobUuid(audit.getJobUuid())
                .jobName(audit.getJobName())
                .inputFileName(audit.getInputFileName())
                .outputFileName(audit.getOutputFileName())
                .rowsProcessed(audit.getRowsProcessed())
                .rowsSkipped(audit.getRowsSkipped())
                .columnsSanitized(audit.getColumnsSanitized())
                .rulesApplied(audit.getRulesApplied())
                .startTime(audit.getStartTime())
                .endTime(audit.getEndTime())
                .status(audit.getStatus())
                .durationMs(audit.getDurationMs())
                .processingRate(audit.getProcessingRate())
                .errorMessage(audit.getErrorMessage())
                .build();
    }
}
