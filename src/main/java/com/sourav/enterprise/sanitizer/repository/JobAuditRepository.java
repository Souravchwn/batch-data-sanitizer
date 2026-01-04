package com.sourav.enterprise.sanitizer.repository;

import com.sourav.enterprise.sanitizer.domain.entity.JobAudit;
import com.sourav.enterprise.sanitizer.domain.enums.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface JobAuditRepository extends JpaRepository<JobAudit, Long> {
    Optional<JobAudit> findByJobExecutionId(Long jobExecutionId);

    List<JobAudit> findByInputFileNameOrderByStartTimeDesc(String inputFileName);

    List<JobAudit> findByStatusOrderByStartTimeDesc(JobStatus status);

    List<JobAudit> findByStartTimeBetweenOrderByStartTimeDesc(LocalDateTime start, LocalDateTime end);

    List<JobAudit> findTop20ByOrderByStartTimeDesc();

    List<JobAudit> findAllByOrderByStartTimeDesc();

    long countByStatus(JobStatus status);

    @Query("SELECT COALESCE(SUM(j.rowsProcessed), 0) FROM JobAudit j WHERE j.status = com.sourav.enterprise.sanitizer.domain.enums.JobStatus.SUCCESS")
    Long getTotalRowsProcessed();

    @Query("SELECT COALESCE(AVG(j.processingRate), 0.0) FROM JobAudit j WHERE j.status = com.sourav.enterprise.sanitizer.domain.enums.JobStatus.SUCCESS AND j.processingRate IS NOT NULL")
    Double getAverageProcessingRate();
}
