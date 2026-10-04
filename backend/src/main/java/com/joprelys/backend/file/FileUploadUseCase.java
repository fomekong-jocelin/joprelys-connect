package com.joprelys.backend.file;

import org.springframework.web.multipart.MultipartFile;

public interface FileUploadUseCase {
    String upload(MultipartFile file, String type);
}
