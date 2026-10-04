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

	@Test
	void signatureJpegIsReencodedAsRealPngRegardlessOfFilename() throws IOException {
		var image = new java.awt.image.BufferedImage(800, 160, java.awt.image.BufferedImage.TYPE_INT_RGB);
		String path = storeSignature(image, "jpg", "misleading.png");
		assertTrue(path.endsWith(".png"));
		byte[] stored = fileStorageService.loadFile(path);
		assertArrayEquals(new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 13, 10, 26, 10}, java.util.Arrays.copyOf(stored, 8));
		var decoded = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(stored));
		assertEquals(500, decoded.getWidth());
		assertEquals(100, decoded.getHeight());
	}

	@Test
	void transparentSignatureKeepsItsAlphaChannel() throws IOException {
		var image = new java.awt.image.BufferedImage(17, 7, java.awt.image.BufferedImage.TYPE_INT_ARGB);
		image.setRGB(4, 3, 0x7f010203);
		String path = storeSignature(image, "png", "signature.png");
		var decoded = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(fileStorageService.loadFile(path)));
		assertEquals(0, decoded.getRGB(0, 0) >>> 24);
		assertEquals(0x7f, decoded.getRGB(4, 3) >>> 24);
		assertEquals(17, decoded.getWidth());
	}

	@Test
	void veryThinSignatureStillHasNonzeroOutputDimensions() throws IOException {
		var image = new java.awt.image.BufferedImage(8192, 1, java.awt.image.BufferedImage.TYPE_INT_ARGB);
		String path = storeSignature(image, "png", "signature.png");
		var decoded = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(fileStorageService.loadFile(path)));
		assertEquals(500, decoded.getWidth());
		assertEquals(1, decoded.getHeight());
	}

	@Test
	void malformedSignatureIsRejectedWithoutStoringRawBytes() throws IOException {
		for (byte[] content : java.util.List.of(
				new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 13, 10, 26, 10, 0, 0, 0, 0},
				new byte[] {0x52, 0x49, 0x46, 0x46, 0, 0, 0, 0, 0x57, 0x45, 0x42, 0x50})) {
			assertThrows(IllegalArgumentException.class, () -> fileStorageService.storeFile(
					new MockMultipartFile("file", "signature.png", "image/png", content), "signature"));
		}
		try (var files = Files.list(tempDir.resolve("signature"))) { assertEquals(0, files.count()); }
	}

	@Test
	void excessiveDimensionsAreRejectedBeforeAllocatingTheImage() throws IOException {
		var image = new java.awt.image.BufferedImage(1, 1, java.awt.image.BufferedImage.TYPE_INT_ARGB);
		var output = new java.io.ByteArrayOutputStream();
		javax.imageio.ImageIO.write(image, "png", output);
		byte[] header = output.toByteArray();
		java.nio.ByteBuffer.wrap(header).putInt(16, 8193);
		var crc = new java.util.zip.CRC32(); crc.update(header, 12, 17);
		java.nio.ByteBuffer.wrap(header).putInt(29, (int) crc.getValue());
		assertThrows(IllegalArgumentException.class, () -> fileStorageService.storeFile(
				new MockMultipartFile("file", "large.png", "image/png", header), "signature"));
		try (var files = Files.list(tempDir.resolve("signature"))) { assertEquals(0, files.count()); }
	}

	private String storeSignature(java.awt.image.BufferedImage image, String format, String filename) throws IOException {
		var output = new java.io.ByteArrayOutputStream();
		javax.imageio.ImageIO.write(image, format, output);
		return fileStorageService.storeFile(new MockMultipartFile("file", filename, "image/" + format, output.toByteArray()), "signature");
	}
}
