package com.joprelys.backend.file;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageService {

	private final Path rootLocation;
	private final long maxFileSize = 2 * 1024 * 1024; // 2 MB

	public FileStorageService(@Value("${joprelys.storage.upload-dir:storage/uploads}") String uploadDir) {
		this.rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
		try {
			Files.createDirectories(this.rootLocation);
		} catch (IOException e) {
			throw new RuntimeException("Impossible d'initialiser le stockage des fichiers.", e);
		}
	}

	public String storeFile(MultipartFile file, String subDirType) {
		if (file.isEmpty()) {
			throw new IllegalArgumentException("Le fichier est vide.");
		}

		// Augmenté à 10 Mo pour permettre le chargement de grandes photos de smartphones,
		// qui seront compressées immédiatement lors du stockage.
		long maxUploadSize = 10 * 1024 * 1024; // 10 MB
		if (file.getSize() > maxUploadSize) {
			throw new IllegalArgumentException("La taille du fichier dépasse la limite autorisée de 10 Mo.");
		}

		// Validations strictes de sécurité (Magic Numbers)
		validateImageHeader(file);

		// Nettoyer et normaliser le sous-dossier (ex: logo, photo, signature, stamp)
		String cleanSubDir = subDirType.replaceAll("[^a-zA-Z0-9_-]", "");
		Path targetFolder = this.rootLocation.resolve(cleanSubDir).normalize();

		try {
			Files.createDirectories(targetFolder);

			// Générer un nom unique pour éviter les conflits et effacer les noms originaux malveillants
			String originalFilename = file.getOriginalFilename();
			String extension = "png"; // default
			if (originalFilename != null && (originalFilename.toLowerCase().endsWith(".jpg") || originalFilename.toLowerCase().endsWith(".jpeg"))) {
				extension = "jpg";
			}
			String uniqueFilename = UUID.randomUUID().toString() + "." + extension;

			Path destinationFile = targetFolder.resolve(uniqueFilename).normalize();

			// Protection stricte contre le Path Traversal
			if (!destinationFile.getParent().startsWith(this.rootLocation)) {
				throw new SecurityException("Tentative d'écriture hors du répertoire autorisé.");
			}

			// Compression et redimensionnement de l'image
			try (InputStream inputStream = file.getInputStream()) {
				java.awt.image.BufferedImage originalImage = javax.imageio.ImageIO.read(inputStream);
				if (originalImage != null) {
					// Définir la dimension maximale autorisée (ex: 800px pour les photos, 500px pour les logos/signatures)
					int maxDimension = "photo".equalsIgnoreCase(cleanSubDir) ? 800 : 500;
					int originalWidth = originalImage.getWidth();
					int originalHeight = originalImage.getHeight();

					if (originalWidth > maxDimension || originalHeight > maxDimension) {
						// Calculer le ratio d'aspect
						double ratio = (double) originalWidth / originalHeight;
						int newWidth, newHeight;
						if (originalWidth > originalHeight) {
							newWidth = maxDimension;
							newHeight = (int) (maxDimension / ratio);
						} else {
							newHeight = maxDimension;
							newWidth = (int) (maxDimension * ratio);
						}

						// Redimensionner l'image
						int imageType = originalImage.getType() == 0 ? java.awt.image.BufferedImage.TYPE_INT_ARGB : originalImage.getType();
						if ("jpg".equals(extension)) {
							imageType = java.awt.image.BufferedImage.TYPE_INT_RGB;
						}
						java.awt.image.BufferedImage resizedImage = new java.awt.image.BufferedImage(newWidth, newHeight, imageType);
						java.awt.Graphics2D g = resizedImage.createGraphics();
						g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
						g.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
						g.dispose();
						
						originalImage = resizedImage;
					}

					// Enregistrer avec compression
					if ("jpg".equals(extension)) {
						java.util.Iterator<javax.imageio.ImageWriter> writers = javax.imageio.ImageIO.getImageWritersByFormatName("jpg");
						if (writers.hasNext()) {
							javax.imageio.ImageWriter writer = writers.next();
							javax.imageio.ImageWriteParam param = writer.getDefaultWriteParam();
							param.setCompressionMode(javax.imageio.ImageWriteParam.MODE_EXPLICIT);
							param.setCompressionQuality(0.75f); // 75% qualité
							
							try (javax.imageio.stream.ImageOutputStream ios = javax.imageio.ImageIO.createImageOutputStream(destinationFile.toFile())) {
								writer.setOutput(ios);
								writer.write(null, new javax.imageio.IIOImage(originalImage, null, null), param);
							} finally {
								writer.dispose();
							}
						} else {
							javax.imageio.ImageIO.write(originalImage, "jpg", destinationFile.toFile());
						}
					} else {
						// Sauvegarde en PNG (avec transparence)
						javax.imageio.ImageIO.write(originalImage, "png", destinationFile.toFile());
					}
				} else {
					// Fallback au fichier brut si la lecture de l'image échoue
					try (InputStream fallbackStream = file.getInputStream()) {
						Files.copy(fallbackStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
					}
				}
			}

			// Retourne le chemin d'accès relatif (ex: uploads/logo/filename.png)
			return "uploads/" + cleanSubDir + "/" + uniqueFilename;

		} catch (IOException e) {
			throw new RuntimeException("Erreur lors de l'enregistrement du fichier.", e);
		}
	}

	public byte[] loadFile(String relativePath) {
		// Le relativePath ressemble à "uploads/logo/uuid.png"
		// On retire le préfixe "uploads/" pour s'aligner sur rootLocation
		String pathWithoutUploads = relativePath;
		if (relativePath.startsWith("uploads/")) {
			pathWithoutUploads = relativePath.substring("uploads/".length());
		}

		Path file = this.rootLocation.resolve(pathWithoutUploads).normalize();

		// Protection contre le Path Traversal
		if (!file.startsWith(this.rootLocation)) {
			throw new SecurityException("Accès interdit au fichier.");
		}

		if (!Files.exists(file) || !Files.isReadable(file)) {
			throw new IllegalArgumentException("Le fichier n'existe pas ou n'est pas lisible : " + relativePath);
		}

		try {
			return Files.readAllBytes(file);
		} catch (IOException e) {
			throw new RuntimeException("Erreur lors de la lecture du fichier.", e);
		}
	}

	private void validateImageHeader(MultipartFile file) {
		try (InputStream is = file.getInputStream()) {
			byte[] header = new byte[8];
			int readBytes = is.read(header);
			if (readBytes < 3) {
				throw new IllegalArgumentException("Fichier trop court pour être une image valide.");
			}

			// Vérification PNG: 89 50 4E 47 0D 0A 1A 0A
			boolean isPng = (header[0] & 0xFF) == 0x89 &&
					(header[1] & 0xFF) == 0x50 &&
					(header[2] & 0xFF) == 0x4E &&
					(header[3] & 0xFF) == 0x47;

			// Vérification JPEG: FF D8 FF
			boolean isJpeg = (header[0] & 0xFF) == 0xFF &&
					(header[1] & 0xFF) == 0xD8 &&
					(header[2] & 0xFF) == 0xFF;

			if (!isPng && !isJpeg) {
				throw new IllegalArgumentException("Type de fichier non autorisé. Seuls les formats PNG et JPEG/JPG réels sont acceptés.");
			}
		} catch (IOException e) {
			throw new RuntimeException("Erreur lors de l'analyse du format du fichier.", e);
		}
	}
}
