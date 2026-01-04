package com.sourav.enterprise.sanitizer.batch.writer;

import com.sourav.enterprise.sanitizer.domain.model.CsvRecord;
import com.opencsv.CSVWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * CSV Item Writer - Writes sanitized records to CSV output file.
 * 
 * Features:
 * - Automatically writes header row on first chunk
 * - Maintains column order from input
 * - Supports job restart (append mode detection)
 * - Tracks total rows written
 */
public class CsvItemWriter implements ItemStreamWriter<CsvRecord> {
    private static final Logger log = LoggerFactory.getLogger(CsvItemWriter.class);
    private static final String HEADER_WRITTEN_KEY = "header.written";

    private final String outputPath;
    private final String[] headers;
    private CSVWriter csvWriter;
    private boolean headerWritten;
    private long writtenCount;

    public CsvItemWriter(String outputPath, String[] headers) {
        this.outputPath = outputPath;
        this.headers = headers;
        this.headerWritten = false;
        this.writtenCount = 0;
    }

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        try {
            // Check if we're restarting
            headerWritten = Boolean.parseBoolean(
                    executionContext.getString(HEADER_WRITTEN_KEY, "false"));

            File outputFile = new File(outputPath);

            // Ensure parent directories exist
            if (outputFile.getParentFile() != null && !outputFile.getParentFile().exists()) {
                outputFile.getParentFile().mkdirs();
            }

            // Append mode if restarting, otherwise create new file
            boolean append = headerWritten && outputFile.exists();
            csvWriter = new CSVWriter(new FileWriter(outputFile, append));

            if (!headerWritten) {
                csvWriter.writeNext(headers);
                headerWritten = true;
                log.info("📝 CSV Writer opened: {} ({} columns)",
                        outputFile.getName(), headers.length);
            } else {
                log.info("📝 CSV Writer resumed: {} (appending)", outputFile.getName());
            }

        } catch (IOException e) {
            throw new ItemStreamException("Failed to open output file: " + outputPath, e);
        }
    }

    @Override
    public void write(Chunk<? extends CsvRecord> chunk) throws Exception {
        for (CsvRecord record : chunk) {
            String[] values = new String[headers.length];
            for (int i = 0; i < headers.length; i++) {
                values[i] = record.getData().getOrDefault(headers[i], "");
            }
            csvWriter.writeNext(values);
            writtenCount++;
        }
        csvWriter.flush();

        log.trace("Wrote chunk of {} records (total: {})", chunk.size(), writtenCount);
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
        executionContext.putString(HEADER_WRITTEN_KEY, String.valueOf(headerWritten));
    }

    @Override
    public void close() throws ItemStreamException {
        if (csvWriter != null) {
            try {
                csvWriter.close();
                log.info("✅ CSV Writer closed: {} rows written", writtenCount);
            } catch (IOException e) {
                throw new ItemStreamException("Failed to close CSV writer", e);
            }
        }
    }

    /**
     * Gets the total number of rows written.
     */
    public long getWrittenCount() {
        return writtenCount;
    }
}
