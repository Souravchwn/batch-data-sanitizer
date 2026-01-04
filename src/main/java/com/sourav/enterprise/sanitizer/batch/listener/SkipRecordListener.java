package com.sourav.enterprise.sanitizer.batch.listener;

import com.sourav.enterprise.sanitizer.domain.model.CsvRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.SkipListener;

/**
 * Skip Record Listener - Logs information about skipped records.
 * 
 * Features:
 * - Logs read failures (malformed CSV lines)
 * - Logs processing failures (sanitization errors)
 * - Logs write failures (I/O errors)
 * - Helps with debugging and monitoring
 */
public class SkipRecordListener implements SkipListener<CsvRecord, CsvRecord> {
    private static final Logger log = LoggerFactory.getLogger(SkipRecordListener.class);

    private long readSkipCount = 0;
    private long processSkipCount = 0;
    private long writeSkipCount = 0;

    @Override
    public void onSkipInRead(Throwable t) {
        readSkipCount++;
        log.warn("⚠️ Skipped on read (count: {}): {}", readSkipCount, t.getMessage());
    }

    @Override
    public void onSkipInProcess(CsvRecord item, Throwable t) {
        processSkipCount++;
        log.warn("⚠️ Skipped on process - line {} (count: {}): {}",
                item.getLineNumber(), processSkipCount, t.getMessage());
    }

    @Override
    public void onSkipInWrite(CsvRecord item, Throwable t) {
        writeSkipCount++;
        log.warn("⚠️ Skipped on write - line {} (count: {}): {}",
                item.getLineNumber(), writeSkipCount, t.getMessage());
    }

    /**
     * Gets total skip count across all phases.
     */
    public long getTotalSkipCount() {
        return readSkipCount + processSkipCount + writeSkipCount;
    }
}
