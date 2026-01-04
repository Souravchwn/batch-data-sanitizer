package com.sourav.enterprise.sanitizer.storage;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;

public interface StorageService {
    String store(MultipartFile file, String directory) throws IOException;

    String store(InputStream inputStream, String filename, String directory) throws IOException;

    InputStream retrieve(String filePath) throws IOException;

    boolean exists(String filePath);

    void delete(String filePath) throws IOException;

    long getSize(String filePath);

    List<String> list(String directory) throws IOException;

    String generateOutputPath(String inputPath);

    Path getFullPath(String relativePath);

    StorageType getType();
}
