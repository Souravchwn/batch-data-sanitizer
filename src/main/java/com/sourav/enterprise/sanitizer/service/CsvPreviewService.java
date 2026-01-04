package com.sourav.enterprise.sanitizer.service;

import com.sourav.enterprise.sanitizer.dto.CsvPreviewResponse;
import com.sourav.enterprise.sanitizer.dto.DiffResponse;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.*;
import java.util.*;

@Service
public class CsvPreviewService {
    private final int defaultPreviewRows;

    public CsvPreviewService(@Value("${sanitizer.preview.max-rows:20}") int defaultPreviewRows) {
        this.defaultPreviewRows = defaultPreviewRows;
    }

    public CsvPreviewResponse previewFile(MultipartFile file, int maxRows) throws IOException {
        int rows = maxRows > 0 ? Math.min(maxRows, 100) : defaultPreviewRows;

        try (CSVReader reader = new CSVReaderBuilder(
                new InputStreamReader(file.getInputStream(), java.nio.charset.StandardCharsets.UTF_8)).build()) {
            return parsePreview(reader, rows, file.getOriginalFilename(), file.getSize());
        } catch (CsvValidationException e) {
            throw new IOException("CSV parsing error: " + e.getMessage(), e);
        }
    }

    public CsvPreviewResponse previewStoredFile(String filePath, int maxRows) throws IOException {
        int rows = maxRows > 0 ? Math.min(maxRows, 100) : defaultPreviewRows;
        File file = new File(filePath);

        try (CSVReader reader = new CSVReaderBuilder(new FileReader(file)).build()) {
            return parsePreview(reader, rows, file.getName(), file.length());
        } catch (CsvValidationException e) {
            throw new IOException("CSV parsing error: " + e.getMessage(), e);
        }
    }

    private CsvPreviewResponse parsePreview(CSVReader reader, int maxRows, String fileName, long fileSize)
            throws IOException, CsvValidationException {
        String[] headers = reader.readNext();
        if (headers == null) {
            return CsvPreviewResponse.builder()
                    .headers(List.of())
                    .rows(List.of())
                    .totalRows(0)
                    .previewRows(0)
                    .fileName(fileName)
                    .fileSize(fileSize)
                    .build();
        }

        List<List<String>> previewRows = new ArrayList<>();
        String[] line;
        int count = 0;
        long totalLines = 0;

        while ((line = reader.readNext()) != null) {
            totalLines++;
            if (count < maxRows) {
                previewRows.add(Arrays.asList(line));
                count++;
            }
        }

        return CsvPreviewResponse.builder()
                .headers(Arrays.asList(headers))
                .rows(previewRows)
                .totalRows(totalLines)
                .previewRows(count)
                .fileName(fileName)
                .fileSize(fileSize)
                .build();
    }

    public DiffResponse generateDiff(String inputFilePath, String outputFilePath, int maxRows) throws IOException {
        int rows = maxRows > 0 ? Math.min(maxRows, 50) : 20;

        try (CSVReader inputReader = new CSVReaderBuilder(new FileReader(inputFilePath)).build();
                CSVReader outputReader = new CSVReaderBuilder(new FileReader(outputFilePath)).build()) {

            String[] inputHeaders = inputReader.readNext();
            String[] outputHeaders = outputReader.readNext();

            if (inputHeaders == null || outputHeaders == null) {
                return DiffResponse.builder()
                        .headers(List.of())
                        .rows(List.of())
                        .totalChanges(0)
                        .changesByColumn(Map.of())
                        .build();
            }

            List<DiffResponse.DiffRow> diffRows = new ArrayList<>();
            Map<String, Integer> changesByColumn = new HashMap<>();
            int totalChanges = 0;

            String[] inputLine;
            String[] outputLine;
            long rowNum = 0;

            while ((inputLine = inputReader.readNext()) != null &&
                    (outputLine = outputReader.readNext()) != null &&
                    rowNum < rows) {
                rowNum++;
                List<DiffResponse.DiffCell> cells = new ArrayList<>();
                boolean hasChanges = false;

                for (int i = 0; i < inputHeaders.length; i++) {
                    String original = i < inputLine.length ? inputLine[i] : "";
                    String sanitized = i < outputLine.length ? outputLine[i] : "";
                    boolean changed = !original.equals(sanitized);

                    if (changed) {
                        hasChanges = true;
                        totalChanges++;
                        changesByColumn.compute(inputHeaders[i], (k, v) -> v == null ? 1 : v + 1);
                    }

                    cells.add(DiffResponse.DiffCell.builder()
                            .column(inputHeaders[i])
                            .originalValue(original)
                            .sanitizedValue(sanitized)
                            .changed(changed)
                            .build());
                }

                if (hasChanges) {
                    diffRows.add(DiffResponse.DiffRow.builder()
                            .rowNumber(rowNum)
                            .cells(cells)
                            .build());
                }
            }

            return DiffResponse.builder()
                    .headers(Arrays.asList(inputHeaders))
                    .rows(diffRows)
                    .totalChanges(totalChanges)
                    .changesByColumn(changesByColumn)
                    .build();
        } catch (CsvValidationException e) {
            throw new IOException("CSV parsing error: " + e.getMessage(), e);
        }
    }
}
