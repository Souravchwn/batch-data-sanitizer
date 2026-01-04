package com.sourav.enterprise.sanitizer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiffResponse {
    private List<String> headers;
    private List<DiffRow> rows;
    private int totalChanges;
    private Map<String, Integer> changesByColumn;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DiffRow {
        private long rowNumber;
        private List<DiffCell> cells;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DiffCell {
        private String column;
        private String originalValue;
        private String sanitizedValue;
        private boolean changed;
        private String operation;
    }
}
