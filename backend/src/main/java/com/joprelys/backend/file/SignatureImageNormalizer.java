package com.joprelys.backend.file;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;

final class SignatureImageNormalizer {
    private static final int MAX_INPUT_DIMENSION = 8192;
    private static final long MAX_INPUT_PIXELS = 16_000_000;
    private static final int MAX_OUTPUT_DIMENSION = 500;

    private SignatureImageNormalizer() {}

    static byte[] toPng(byte[] source) {
        try (ImageInputStream input = new MemoryCacheImageInputStream(new ByteArrayInputStream(source))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw invalidImage();
            ImageReader reader = readers.next();
            try {
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                if (!"png".equals(format) && !"jpeg".equals(format) && !"jpg".equals(format)) throw invalidImage();
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width < 1 || height < 1 || width > MAX_INPUT_DIMENSION || height > MAX_INPUT_DIMENSION
                        || (long) width * height > MAX_INPUT_PIXELS) {
                    throw new IllegalArgumentException("Les dimensions de la signature dépassent la limite autorisée.");
                }
                return encodePng(reader.read(0));
            } finally {
                reader.dispose();
            }
        } catch (IOException error) {
            throw new IllegalArgumentException("La signature doit être une image PNG ou JPEG lisible.", error);
        }
    }

    private static byte[] encodePng(BufferedImage original) throws IOException {
        double scale = Math.min(1.0, (double) MAX_OUTPUT_DIMENSION / Math.max(original.getWidth(), original.getHeight()));
        int width = Math.max(1, (int) Math.round(original.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(original.getHeight() * scale));
        BufferedImage normalized = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = normalized.createGraphics();
        try {
            graphics.setComposite(AlphaComposite.Src);
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.drawImage(original, 0, 0, width, height, null);
        } finally {
            graphics.dispose();
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        if (!ImageIO.write(normalized, "png", output)) throw new IOException("Encodeur PNG indisponible.");
        return output.toByteArray();
    }

    private static IllegalArgumentException invalidImage() {
        return new IllegalArgumentException("La signature doit être une image PNG ou JPEG lisible.");
    }
}
