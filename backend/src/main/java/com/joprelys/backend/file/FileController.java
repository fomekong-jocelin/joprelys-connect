package com.joprelys.backend.file;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class FileController {

	private final FileStorageService fileStorageService;

	public FileController(FileStorageService fileStorageService) {
		this.fileStorageService = fileStorageService;
	}

	@PostMapping("/api/files/upload")
	public ResponseEntity<UploadResponse> uploadFile(
			@RequestParam("file") MultipartFile file,
			@RequestParam(value = "type", defaultValue = "photo") String type) {
		
		if (!"logo".equalsIgnoreCase(type) && !"photo".equalsIgnoreCase(type) &&
				!"signature".equalsIgnoreCase(type) && !"stamp".equalsIgnoreCase(type)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Type de téléversement invalide.");
		}

		try {
			String path = fileStorageService.storeFile(file, type.toLowerCase());
			String viewUrl = "/api/public/files/view?path=" + path;
			return ResponseEntity.status(HttpStatus.CREATED).body(new UploadResponse(path, viewUrl));
		} catch (IllegalArgumentException | SecurityException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
		}
	}

	@GetMapping("/api/public/files/view")
	public ResponseEntity<byte[]> viewFile(@RequestParam("path") String path) {
		if (path == null || path.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le chemin du fichier est requis.");
		}

		try {
			byte[] data = fileStorageService.loadFile(path);
			
			MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
			if (path.toLowerCase().endsWith(".png")) {
				mediaType = MediaType.IMAGE_PNG;
			} else if (path.toLowerCase().endsWith(".jpg") || path.toLowerCase().endsWith(".jpeg")) {
				mediaType = MediaType.IMAGE_JPEG;
			}

			return ResponseEntity.ok()
					.contentType(mediaType)
					.body(data);
		} catch (IllegalArgumentException e) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
		} catch (SecurityException e) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
		}
	}

	public record UploadResponse(String filePath, String viewUrl) {
	}
}
