package com.sourav.enterprise.sanitizer.batch.reader;

import com.sourav.enterprise.sanitizer.domain.model.CsvRecord;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;

import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * CSV Item Reader - Reads CSV files line by line for batch processing.
 * 
 * Features:
 * - Automatic header detection from first row
 * - Maintains column order using LinkedHashMap
 * - Tracks line numbers for error reporting
 * - Implements ItemStreamReader for state management
 */
public class CsvItemReader implements ItemStreamReader<CsvRecord> {
    private static final Logger log = LoggerFactory.getLogger(CsvItemReader.class);
    private static final String CURRENT_LINE_KEY = "current.line";

    private final String filePath;
    private CSVReader csvReader;
    private String[] headers;
    private long currentLine;
    private boolean initialized;

    public CsvItemReader(String filePath) {
        this.filePath = filePath;
        this.currentLine = 0;
        this.initialized = false;
    }

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        try {
            csvReader = new CSVReaderBuilder(new FileReader(filePath)).build();
            headers = csvReader.readNext();

            if (headers == null || headers.length == 0) {
                throw new ItemStreamException("Empty or invalid CSV file: " + filePath);
            }

            // Restore position if restarting
            currentLine = executionContext.containsKey(CURRENT_LINE_KEY)
                    ? executionContext.getLong(CURRENT_LINE_KEY)
                    : 0;

            // Skip to the restored position
            for (long i = 0; i < currentLine; i++) {
                csvReader.readNext();
            }

            initialized = true;
            log.info("📖 CSV Reader opened: {} ({} columns)",
                    filePath.substring(filePath.lastIndexOf('/') + 1), headers.length);

        } catch (IOException | CsvValidationException e) {
            throw new ItemStreamException("Failed to open CSV file: " + filePath, e);
        }
    }

    @Override
    public CsvRecord read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
        if (!initialized) {
            throw new IllegalStateException("Reader not initialized. Call open() first.");
        }

        String[] values;
        try {
            values = csvReader.readNext();
        } catch (CsvValidationException | IOException e) {
            throw new ParseException("Error reading CSV line " + (currentLine + 1), e);
        }

        if (values == null) {
            return null; // End of file
        }

        currentLine++;
        Map<String, String> data = new LinkedHashMap<>();

        for (int i = 0; i < headers.length; i++) {
            data.put(headers[i], i < values.length ? values[i] : "");
        }

        return CsvRecord.builder()
                .lineNumber(currentLine)
                .data(data)
                .build();
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
        executionContext.putLong(CURRENT_LINE_KEY, currentLine);
    }

    @Override
    public void close() throws ItemStreamException {
        if (csvReader != null) {
            try {
                csvReader.close();
                log.debug("CSV Reader closed after {} lines", currentLine);
            } catch (IOException e) {
                throw new ItemStreamException("Failed to close CSV reader", e);
            }
        }
        initialized = false;
    }

    /**
     * Gets the column headers from the CSV file.
     * Must be called after open().
     */
    public String[] getHeaders() {
        if (headers == null) {
            throw new IllegalStateException("Headers not available. Call open() first.");
        }
        return headers;
    }

    /**
     * Gets the current line number being processed.
     */
    public long getCurrentLine() {
        return currentLine;
    }
}
