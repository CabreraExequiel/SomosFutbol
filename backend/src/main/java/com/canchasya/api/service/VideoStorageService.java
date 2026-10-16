
package com.canchasya.api.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
public class VideoStorageService {

    private static final long MAX_SIZE =
            100L * 1024 * 1024;

    private final Path directorio;

    public VideoStorageService(
            @Value("${app.storage.location:./uploads}")
            String storageLocation
    ) {
        this.directorio = Path.of(storageLocation)
                .toAbsolutePath()
                .normalize()
                .resolve("videos");
    }

    // ==========================================
    // GUARDAR VIDEO
    // ==========================================

    public VideoGuardado guardar(MultipartFile video) {

        if (video == null || video.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Debés seleccionar un video"
            );
        }

        if (video.getSize() > MAX_SIZE) {
            throw new ResponseStatusException(
                    HttpStatus.PAYLOAD_TOO_LARGE,
                    "El video no puede superar los 100 MB"
            );
        }

        if (!"video/mp4".equalsIgnoreCase(
                video.getContentType())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Solo se permiten videos MP4"
            );
        }

        String publicId = UUID.randomUUID().toString();
        Path destino = directorio.resolve(publicId + ".mp4");

        try {
            // Comprobar la cabecera básica MP4
            try (InputStream in = video.getInputStream()) {
                byte[] cabecera = in.readNBytes(12);

                if (cabecera.length < 12 ||
                        !"ftyp".equals(new String(
                                cabecera,
                                4,
                                4,
                                StandardCharsets.US_ASCII))) {

                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "El archivo no parece ser un MP4 válido"
                    );
                }
            }

            Files.createDirectories(directorio);

            try (InputStream in = video.getInputStream()) {
                Files.copy(in, destino);
            }

            String url = "/api/publicaciones/videos/archivo/"
                    + publicId;

            return new VideoGuardado(publicId, url);

        } catch (IOException e) {

            try {
                Files.deleteIfExists(destino);
            } catch (IOException ignored) {
            }

            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "No se pudo guardar el video",
                    e
            );
        }
    }

    // ==========================================
    // OBTENER VIDEO
    // ==========================================

    public Resource obtener(String publicId) {

        if (publicId == null ||
                !publicId.matches(
                        "[0-9a-fA-F-]{36}")) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Identificador de video inválido"
            );
        }

        Path archivo = directorio.resolve(
                publicId + ".mp4"
        ).normalize();

        if (!archivo.startsWith(directorio)
                || !Files.isRegularFile(archivo)) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Video no encontrado"
            );
        }

        return new FileSystemResource(archivo);
    }

    // ==========================================
    // ELIMINAR VIDEO
    // ==========================================

    public void eliminar(String publicId) {

        if (publicId == null ||
                !publicId.matches(
                        "[0-9a-fA-F-]{36}")) {
            return;
        }

        Path archivo = directorio.resolve(
                publicId + ".mp4"
        ).normalize();

        if (!archivo.startsWith(directorio)) {
            return;
        }

        try {
            Files.deleteIfExists(archivo);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "No se pudo eliminar el video", e
            );
        }
    }

    // ==========================================
    // DATOS DEL VIDEO
    // ==========================================

    public record VideoGuardado(
            String publicId,
            String url
    ) {
    }
}
