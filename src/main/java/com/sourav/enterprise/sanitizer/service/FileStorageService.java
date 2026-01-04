package com.sourav.enterprise.sanitizer.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class FileStorageService {
    private final Path inputDir;
    private final Path outputDir;

    public FileStorageService(
            @Value("${sanitizer.storage.input-dir:./data/input}") String inputDir,
            @Value("${sanitizer.storage.output-dir:./data/output}") String outputDir) throws IOException {
        this.inputDir = Paths.get(inputDir).toAbsolutePath().normalize();
        this.outputDir = Paths.get(outputDir).toAbsolutePath().normalize();
        Files.createDirectories(this.inputDir);
        Files.createDirectories(this.outputDir);
    }

    public String saveInputFile(MultipartFile file) throws IOException {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        String originalFilename = file.getOriginalFilename();
        String baseName = "input", extension = ".csv";

        if (originalFilename != null && !originalFilename.isEmpty()) {
            int dotIndex = originalFilename.lastIndexOf('.');
            if (dotIndex > 0) {
                baseName = originalFilename.substring(0, dotIndex);
                extension = originalFilename.substring(dotIndex);
            } else {
                baseName = originalFilename;
            }
        }

        String filename = String.format("%s_%s_%s%s", baseName, timestamp, uniqueId, extension);
        Path targetPath = inputDir.resolve(filename);
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        return targetPath.toString();
    }

    public String generateOutputPath(String inputFilePath) {
        Path inputPath = Paths.get(inputFilePath);
        String filename = inputPath.getFileName().toString();
        int dotIndex = filename.lastIndexOf('.');
        String baseName = dotIndex > 0 ? filename.substring(0, dotIndex) : filename;
        String extension = dotIndex > 0 ? filename.substring(dotIndex) : ".csv";
        return outputDir.resolve(baseName + "_sanitized" + extension).toString();
    }

    public Path getInputDir() {
        return inputDir;
    }

    public Path getOutputDir() {
        return outputDir;
    }
}
