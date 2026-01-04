package com.sourav.enterprise.sanitizer.batch.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.scope.context.ChunkContext;

/**
 * Chunk Progress Listener - Logs processing progress at chunk boundaries.
 * 
 * Features:
 * - Logs progress every chunk
 * - Shows running totals
 * - Useful for monitoring long-running jobs
 */
public class ChunkProgressListener implements ChunkListener {
    private static final Logger log = LoggerFactory.getLogger(ChunkProgressListener.class);

    private long totalProcessed = 0;
    private long startTime = 0;

    @Override
    public void beforeChunk(ChunkContext context) {
        if (startTime == 0) {
            startTime = System.currentTimeMillis();
        }
    }

    @Override
    public void afterChunk(ChunkContext context) {
        long readCount = context.getStepContext().getStepExecution().getReadCount();
        totalProcessed = readCount;

        long elapsed = System.currentTimeMillis() - startTime;
        double rate = elapsed > 0 ? (totalProcessed * 1000.0 / elapsed) : 0;

        // Log every 10 chunks or every 10000 rows
        if (totalProcessed % 10000 == 0 || totalProcessed < 1000) {
            log.info("📊 Progress: {} rows processed ({} rows/sec)",
                    totalProcessed, String.format("%.1f", rate));
        }
    }

    @Override
    public void afterChunkError(ChunkContext context) {
        log.warn("⚠️ Chunk error at row {}", totalProcessed);
    }
}
