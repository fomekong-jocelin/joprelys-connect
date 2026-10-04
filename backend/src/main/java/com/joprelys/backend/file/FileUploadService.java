package com.joprelys.backend.file;

import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileUploadService implements FileUploadUseCase {
    private final FileStorageService storage;

    public FileUploadService(FileStorageService storage) {
        this.storage = storage;
    }

    @Override
    public String upload(MultipartFile file, String type) {
        String normalizedType = type == null ? "" : type.toLowerCase(Locale.ROOT);
        if (!Set.of("logo", "photo", "signature", "stamp").contains(normalizedType)) {
            throw new IllegalArgumentException("Type de téléversement invalide.");
        }
        return storage.storeFile(file, normalizedType);
    }
}
