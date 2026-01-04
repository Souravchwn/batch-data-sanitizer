package com.sourav.enterprise.sanitizer.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;

@Service
@ConditionalOnProperty(name = "sanitizer.storage.type", havingValue = "s3")
public class S3StorageService implements StorageService {
    private static final Logger log = LoggerFactory.getLogger(S3StorageService.class);

    private final String bucketName;
    private final String region;
    private final String accessKey;
    private final String secretKey;
    private final String inputPrefix;
    private final String outputPrefix;

    public S3StorageService(
            @Value("${sanitizer.storage.s3.bucket-name:}") String bucketName,
            @Value("${sanitizer.storage.s3.region:us-east-1}") String region,
            @Value("${sanitizer.storage.s3.access-key:}") String accessKey,
            @Value("${sanitizer.storage.s3.secret-key:}") String secretKey,
            @Value("${sanitizer.storage.s3.input-prefix:input/}") String inputPrefix,
            @Value("${sanitizer.storage.s3.output-prefix:output/}") String outputPrefix) {
        this.bucketName = bucketName;
        this.region = region;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.inputPrefix = inputPrefix;
        this.outputPrefix = outputPrefix;
        log.info("S3StorageService configured: bucket={}, region={}", bucketName, region);
    }

    @Override
    public String store(MultipartFile file, String directory) throws IOException {
        // TODO: Implement S3 upload using AWS SDK
        // For now, throw UnsupportedOperationException
        throw new UnsupportedOperationException("S3 storage not yet implemented. Set sanitizer.storage.type=local");
    }

    @Override
    public String store(InputStream inputStream, String filename, String directory) throws IOException {
        throw new UnsupportedOperationException("S3 storage not yet implemented");
    }

    @Override
    public InputStream retrieve(String filePath) throws IOException {
        throw new UnsupportedOperationException("S3 storage not yet implemented");
    }

    @Override
    public boolean exists(String filePath) {
        return false;
    }

    @Override
    public void delete(String filePath) throws IOException {
        throw new UnsupportedOperationException("S3 storage not yet implemented");
    }

    @Override
    public long getSize(String filePath) {
        return 0;
    }

    @Override
    public List<String> list(String directory) throws IOException {
        throw new UnsupportedOperationException("S3 storage not yet implemented");
    }

    @Override
    public String generateOutputPath(String inputPath) {
        String filename = inputPath.substring(inputPath.lastIndexOf('/') + 1);
        int dotIndex = filename.lastIndexOf('.');
        String baseName = dotIndex > 0 ? filename.substring(0, dotIndex) : filename;
        String extension = dotIndex > 0 ? filename.substring(dotIndex) : ".csv";
        return outputPrefix + baseName + "_sanitized" + extension;
    }

    @Override
    public Path getFullPath(String relativePath) {
        return Path.of("s3://" + bucketName + "/" + relativePath);
    }

    @Override
    public StorageType getType() {
        return StorageType.S3;
    }

    public String getBucketName() {
        return bucketName;
    }

    public String getRegion() {
        return region;
    }
}
