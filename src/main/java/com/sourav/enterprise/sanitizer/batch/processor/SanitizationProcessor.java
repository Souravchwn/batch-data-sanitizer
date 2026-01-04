package com.sourav.enterprise.sanitizer.batch.processor;

import com.sourav.enterprise.sanitizer.domain.model.CsvRecord;
import com.sourav.enterprise.sanitizer.domain.model.SanitizationConfig;

import com.sourav.enterprise.sanitizer.strategy.SanitizationStrategy;
import com.sourav.enterprise.sanitizer.strategy.SanitizationStrategyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Sanitization Processor - Applies sanitization rules to CSV records.
 * 
 * Features:
 * - Strategy pattern for different sanitization operations
 * - Caches strategies for performance
 * - Preserves column order in output
 * - Tracks sanitization statistics
 */
public class SanitizationProcessor implements ItemProcessor<CsvRecord, CsvRecord> {
    private static final Logger log = LoggerFactory.getLogger(SanitizationProcessor.class);

    private final SanitizationConfig config;
    private final SanitizationStrategyFactory strategyFactory;
    private final Map<String, SanitizationStrategy> strategyCache;
    private long processedCount;
    private long sanitizedFieldCount;

    public SanitizationProcessor(SanitizationConfig config, SanitizationStrategyFactory strategyFactory) {
        this.config = config;
        this.strategyFactory = strategyFactory;
        this.strategyCache = new HashMap<>();
        this.processedCount = 0;
        this.sanitizedFieldCount = 0;

        // Pre-cache strategies for configured columns
        initializeStrategies();
    }

    private void initializeStrategies() {
        config.getColumns().forEach((column, operation) -> {
            strategyCache.put(column, strategyFactory.getStrategy(operation));
        });
        log.debug("🔧 Initialized {} sanitization strategies", strategyCache.size());
    }

    @Override
    public CsvRecord process(CsvRecord item) throws Exception {
        Map<String, String> sanitizedData = new LinkedHashMap<>();

        for (Map.Entry<String, String> entry : item.getData().entrySet()) {
            String column = entry.getKey();
            String value = entry.getValue();

            if (strategyCache.containsKey(column) && value != null && !value.isEmpty()) {
                // Apply sanitization strategy
                SanitizationStrategy strategy = strategyCache.get(column);
                String sanitizedValue = strategy.apply(value);
                sanitizedData.put(column, sanitizedValue);
                sanitizedFieldCount++;
            } else {
                // Keep original value for non-configured columns
                sanitizedData.put(column, value);
            }
        }

        processedCount++;

        return CsvRecord.builder()
                .lineNumber(item.getLineNumber())
                .data(sanitizedData)
                .build();
    }

    /**
     * Gets the total number of records processed.
     */
    public long getProcessedCount() {
        return processedCount;
    }

    /**
     * Gets the total number of fields sanitized.
     */
    public long getSanitizedFieldCount() {
        return sanitizedFieldCount;
    }
}
