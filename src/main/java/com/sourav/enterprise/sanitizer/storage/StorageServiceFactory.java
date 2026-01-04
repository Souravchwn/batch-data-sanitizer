package com.sourav.enterprise.sanitizer.storage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class StorageServiceFactory {
    private final Map<StorageType, StorageService> storageServices;
    private final StorageService defaultService;

    @Autowired
    public StorageServiceFactory(List<StorageService> services) {
        this.storageServices = services.stream()
                .collect(Collectors.toMap(StorageService::getType, Function.identity()));
        this.defaultService = services.stream()
                .filter(s -> s.getType() == StorageType.LOCAL)
                .findFirst()
                .orElse(services.isEmpty() ? null : services.get(0));
    }

    public StorageService getService(StorageType type) {
        return Optional.ofNullable(storageServices.get(type))
                .orElseThrow(() -> new IllegalArgumentException("Storage type not available: " + type));
    }

    public StorageService getDefaultService() {
        if (defaultService == null) {
            throw new IllegalStateException("No storage service available");
        }
        return defaultService;
    }

    public boolean isAvailable(StorageType type) {
        return storageServices.containsKey(type);
    }
}
