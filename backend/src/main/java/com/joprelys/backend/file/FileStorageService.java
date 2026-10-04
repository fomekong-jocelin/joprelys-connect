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

		// Charger les octets en mémoire dès le début pour éviter les accès concurrents ou les flux fermés (notamment sur Windows)
		byte[] fileBytes;
		try {
			fileBytes = file.getBytes();
		} catch (IOException e) {
			throw new RuntimeException("Impossible de lire les données du fichier.", e);
		}

		// Validations strictes de sécurité (Magic Numbers)
		validateImageHeader(fileBytes);

		// Nettoyer et normaliser le sous-dossier (ex: logo, photo, signature, stamp)
		String cleanSubDir = subDirType.replaceAll("[^a-zA-Z0-9_-]", "");
		Path targetFolder = this.rootLocation.resolve(cleanSubDir).normalize();

		try {
			Files.createDirectories(targetFolder);

			if ("signature".equalsIgnoreCase(cleanSubDir)) {
				return persistImage(SignatureImageNormalizer.toPng(fileBytes), cleanSubDir, targetFolder, "png");
			}

			// Détecter l'extension d'origine
			String originalFilename = file.getOriginalFilename();
			String extension = "png"; // default
			if (originalFilename != null && (originalFilename.toLowerCase().endsWith(".jpg") || originalFilename.toLowerCase().endsWith(".jpeg"))) {
				extension = "jpg";
			} else if (originalFilename != null && originalFilename.toLowerCase().endsWith(".webp")) {
				extension = "webp";
			}

			// Traiter l'image en mémoire
			boolean isRedimensionne = false;
			byte[] finalBytesToSave = fileBytes;
			String finalExtension = extension;

			try (InputStream inputStream = new java.io.ByteArrayInputStream(fileBytes)) {
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
						isRedimensionne = true;
					}

					if (isRedimensionne) {
						// Si redimensionné, on compresse et sauvegarde
						java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
						if ("jpg".equals(extension)) {
							java.util.Iterator<javax.imageio.ImageWriter> writers = javax.imageio.ImageIO.getImageWritersByFormatName("jpg");
							if (writers.hasNext()) {
								javax.imageio.ImageWriter writer = writers.next();
								javax.imageio.ImageWriteParam param = writer.getDefaultWriteParam();
								param.setCompressionMode(javax.imageio.ImageWriteParam.MODE_EXPLICIT);
								param.setCompressionQuality(0.75f); // 75% qualité
								
								try (javax.imageio.stream.ImageOutputStream ios = javax.imageio.ImageIO.createImageOutputStream(baos)) {
									writer.setOutput(ios);
									writer.write(null, new javax.imageio.IIOImage(originalImage, null, null), param);
								} finally {
									writer.dispose();
								}
							} else {
								javax.imageio.ImageIO.write(originalImage, "jpg", baos);
							}
						} else {
							// Si c'est un WebP redimensionné, on doit le sauvegarder au format PNG en forçant l'extension à png
							// car le JDK standard ne sait pas écrire du WebP.
							if ("webp".equals(extension)) {
								finalExtension = "png";
							}
							javax.imageio.ImageIO.write(originalImage, "png", baos);
						}
						finalBytesToSave = baos.toByteArray();
					}
				}
			} catch (Exception e) {
				// En cas d'erreur de lecture d'image (ex: format WebP non géré par ImageIO),
				// on garde le fichier brut d'origine sans modification.
			}

			return persistImage(finalBytesToSave, cleanSubDir, targetFolder, finalExtension);

		} catch (IOException e) {
			throw new RuntimeException("Erreur lors de l'enregistrement du fichier.", e);
		}
	}

	private String persistImage(byte[] bytes, String subDir, Path folder, String extension) throws IOException {
		String filename = UUID.randomUUID() + "." + extension;
		Path destination = folder.resolve(filename).normalize();
		if (!destination.getParent().startsWith(this.rootLocation)) {
			throw new SecurityException("Tentative d'écriture hors du répertoire autorisé.");
		}
		Files.write(destination, bytes);
		return "uploads/" + subDir + "/" + filename;
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

	private void validateImageHeader(byte[] fileBytes) {
		int readBytes = Math.min(fileBytes.length, 12);
		if (readBytes < 4) {
			throw new IllegalArgumentException("Fichier trop court pour être une image valide.");
		}

		byte[] header = new byte[12];
		System.arraycopy(fileBytes, 0, header, 0, readBytes);

		// Vérification PNG: 89 50 4E 47 0D 0A 1A 0A
		boolean isPng = readBytes >= 4 &&
				(header[0] & 0xFF) == 0x89 &&
				(header[1] & 0xFF) == 0x50 &&
				(header[2] & 0xFF) == 0x4E &&
				(header[3] & 0xFF) == 0x47;

		// Vérification JPEG: FF D8 FF
		boolean isJpeg = readBytes >= 3 &&
				(header[0] & 0xFF) == 0xFF &&
				(header[1] & 0xFF) == 0xD8 &&
				(header[2] & 0xFF) == 0xFF;

		// Vérification WebP: "RIFF" .... "WEBP"
		boolean isWebp = readBytes >= 12 &&
				(header[0] & 0xFF) == 0x52 && // 'R'
				(header[1] & 0xFF) == 0x49 && // 'I'
				(header[2] & 0xFF) == 0x46 && // 'F'
				(header[3] & 0xFF) == 0x46 && // 'F'
				(header[8] & 0xFF) == 0x57 && // 'W'
				(header[9] & 0xFF) == 0x45 && // 'E'
				(header[10] & 0xFF) == 0x42 && // 'B'
				(header[11] & 0xFF) == 0x50;   // 'P'

		if (!isPng && !isJpeg && !isWebp) {
			throw new IllegalArgumentException("Type de fichier non autorisé. Seuls les formats PNG, JPEG/JPG et WEBP réels sont acceptés.");
		}
	}
}
