package com.sourav.enterprise.sanitizer.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@ConditionalOnProperty(name = "sanitizer.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalStorageService implements StorageService {
    private static final Logger log = LoggerFactory.getLogger(LocalStorageService.class);

    private final Path basePath;
    private final Path inputDir;
    private final Path outputDir;

    public LocalStorageService(
            @Value("${sanitizer.storage.base-path:./data}") String basePath,
            @Value("${sanitizer.storage.input-dir:input}") String inputDir,
            @Value("${sanitizer.storage.output-dir:output}") String outputDir) throws IOException {
        this.basePath = Paths.get(basePath).toAbsolutePath().normalize();
        this.inputDir = this.basePath.resolve(inputDir);
        this.outputDir = this.basePath.resolve(outputDir);

        Files.createDirectories(this.inputDir);
        Files.createDirectories(this.outputDir);
        log.info("LocalStorageService initialized: base={}", this.basePath);
    }

    @Override
    public String store(MultipartFile file, String directory) throws IOException {
        String filename = generateUniqueFilename(file.getOriginalFilename());
        Path targetDir = "output".equals(directory) ? outputDir : inputDir;
        Path targetPath = targetDir.resolve(filename);
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        log.info("Stored file: {}", targetPath);
        return targetPath.toString();
    }

    @Override
    public String store(InputStream inputStream, String filename, String directory) throws IOException {
        Path targetDir = "output".equals(directory) ? outputDir : inputDir;
        Path targetPath = targetDir.resolve(filename);
        Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
        return targetPath.toString();
    }

    @Override
    public InputStream retrieve(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw new FileNotFoundException("File not found: " + filePath);
        }
        return new BufferedInputStream(Files.newInputStream(path));
    }

    @Override
    public boolean exists(String filePath) {
        return Files.exists(Paths.get(filePath));
    }

    @Override
    public void delete(String filePath) throws IOException {
        Files.deleteIfExists(Paths.get(filePath));
    }

    @Override
    public long getSize(String filePath) {
        try {
            return Files.size(Paths.get(filePath));
        } catch (IOException e) {
            return 0;
        }
    }

    @Override
    public List<String> list(String directory) throws IOException {
        Path dir = basePath.resolve(directory);
        if (!Files.exists(dir))
            return List.of();
        try (Stream<Path> stream = Files.list(dir)) {
            return stream.map(Path::toString).collect(Collectors.toList());
        }
    }

    @Override
    public String generateOutputPath(String inputPath) {
        Path inputFile = Paths.get(inputPath);
        String filename = inputFile.getFileName().toString();
        int dotIndex = filename.lastIndexOf('.');
        String baseName = dotIndex > 0 ? filename.substring(0, dotIndex) : filename;
        String extension = dotIndex > 0 ? filename.substring(dotIndex) : ".csv";
        return outputDir.resolve(baseName + "_sanitized" + extension).toString();
    }

    @Override
    public Path getFullPath(String relativePath) {
        return basePath.resolve(relativePath);
    }

    @Override
    public StorageType getType() {
        return StorageType.LOCAL;
    }

    private String generateUniqueFilename(String originalFilename) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        String baseName = "file";
        String extension = ".csv";

        if (originalFilename != null && !originalFilename.isEmpty()) {
            int dotIndex = originalFilename.lastIndexOf('.');
            if (dotIndex > 0) {
                baseName = originalFilename.substring(0, dotIndex);
                extension = originalFilename.substring(dotIndex);
            } else {
                baseName = originalFilename;
            }
        }
        return String.format("%s_%s_%s%s", baseName, timestamp, uniqueId, extension);
    }
}
