package com.sourav.enterprise.sanitizer.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * CSV Record - Represents a single row from a CSV file.
 * Uses LinkedHashMap to preserve column order.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CsvRecord {

    /** Line number in the original CSV file (1-indexed, excluding header) */
    private long lineNumber;

    /** Column name to value mapping, preserves order */
    @Builder.Default
    private Map<String, String> data = new LinkedHashMap<>();

    /**
     * Gets the value of a specific column.
     */
    public String getValue(String columnName) {
        return data.get(columnName);
    }

    /**
     * Sets the value of a specific column.
     */
    public void setValue(String columnName, String value) {
        data.put(columnName, value);
    }

    /**
     * Checks if this record has a specific column.
     */
    public boolean hasColumn(String columnName) {
        return data.containsKey(columnName);
    }
}
