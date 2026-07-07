package com.joprelys.backend.file;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class FileStorageServiceTest {

	private FileStorageService fileStorageService;

	@TempDir
	Path tempDir;

	@BeforeEach
	void setUp() {
		fileStorageService = new FileStorageService(tempDir.toString());
	}

	@Test
	void testStorePngSuccess() throws IOException {
		// En-tête PNG valide : 89 50 4E 47 0D 0A 1A 0A
		byte[] content = new byte[] {
				(byte) 0x89, (byte) 0x50, (byte) 0x4E, (byte) 0x47,
				0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0
		};
		MockMultipartFile file = new MockMultipartFile("file", "test.png", "image/png", content);

		String path = fileStorageService.storeFile(file, "photo");
		assertNotNull(path);
		assertTrue(path.startsWith("uploads/photo/"));
		assertTrue(path.endsWith(".png"));

		byte[] loaded = fileStorageService.loadFile(path);
		assertArrayEquals(content, loaded);
	}

	@Test
	void testStoreJpegSuccess() throws IOException {
		// En-tête JPEG valide : FF D8 FF
		byte[] content = new byte[] {
				(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0, 0, 0, 0, 0, 0, 0, 0, 0
		};
		MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", content);

		String path = fileStorageService.storeFile(file, "photo");
		assertNotNull(path);
		assertTrue(path.startsWith("uploads/photo/"));
		assertTrue(path.endsWith(".jpg"));

		byte[] loaded = fileStorageService.loadFile(path);
		assertArrayEquals(content, loaded);
	}

	@Test
	void testStoreWebpSuccess() throws IOException {
		// En-tête WebP valide : "RIFF" (0-3) et "WEBP" (8-11)
		byte[] content = new byte[] {
				(byte) 0x52, (byte) 0x49, (byte) 0x46, (byte) 0x46, // RIFF
				0, 0, 0, 0,
				(byte) 0x57, (byte) 0x45, (byte) 0x42, (byte) 0x50  // WEBP
		};
		MockMultipartFile file = new MockMultipartFile("file", "test.webp", "image/webp", content);

		String path = fileStorageService.storeFile(file, "photo");
		assertNotNull(path);
		assertTrue(path.startsWith("uploads/photo/"));
		assertTrue(path.endsWith(".webp"));

		byte[] loaded = fileStorageService.loadFile(path);
		assertArrayEquals(content, loaded);
	}

	@Test
	void testStoreInvalidHeaderThrowsException() {
		byte[] content = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12 };
		MockMultipartFile file = new MockMultipartFile("file", "test.png", "image/png", content);

		assertThrows(IllegalArgumentException.class, () -> {
			fileStorageService.storeFile(file, "photo");
		});
	}
}
