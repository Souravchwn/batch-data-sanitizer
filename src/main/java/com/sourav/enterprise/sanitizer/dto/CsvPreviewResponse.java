package com.sourav.enterprise.sanitizer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CsvPreviewResponse {
    private List<String> headers;
    private List<List<String>> rows;
    private long totalRows;
    private int previewRows;
    private String fileName;
    private long fileSize;
}
