package com.canchasya.api.service;

import com.canchasya.api.exception.ApiException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Set;
import java.util.UUID;

@Service
public class ArchivoService {

    private static final long MAX_IMAGE_BYTES = 5L * 1024L * 1024L;
    private static final int MAX_IMAGE_DIMENSION = 8000;
    private static final long MAX_PIXELS = 20_000_000L;
    private static final Set<String> CARPETAS_PERMITIDAS = Set.of("avatars", "canchas");

    private final Path root;
    private final String publicBaseUrl;

    public ArchivoService(
            @Value("${app.storage.location:./uploads}") String storageLocation,
            @Value("${app.public-base-url:}") String publicBaseUrl
    ) {
        this.root = Path.of(storageLocation).toAbsolutePath().normalize();
        this.publicBaseUrl = publicBaseUrl == null ? "" : publicBaseUrl.trim().replaceAll("/+$", "");
    }

    @PostConstruct
    void inicializar() {
        try {
            for (String carpeta : CARPETAS_PERMITIDAS) {
                Files.createDirectories(root.resolve(carpeta));
            }
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo inicializar el almacenamiento de imágenes", ex);
        }
    }

    public String guardarImagen(MultipartFile archivo, String carpeta) {
        validarCarpeta(carpeta);
        if (archivo == null || archivo.isEmpty()) {
            throw new ApiException("Seleccioná una imagen", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        if (archivo.getSize() > MAX_IMAGE_BYTES) {
            throw new ApiException("La imagen no puede superar los 5 MB", HttpStatus.PAYLOAD_TOO_LARGE);
        }

        try {
            byte[] contenido = archivo.getBytes();
            String extension = detectarExtension(contenido);
            validarDimensiones(contenido);

            String nombre = UUID.randomUUID() + extension;
            Path destino = root.resolve(carpeta).resolve(nombre).normalize();
            validarRuta(destino);
            Files.write(destino, contenido, StandardOpenOption.CREATE_NEW);

            return construirUrl(carpeta, nombre);
        } catch (ApiException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new ApiException("No se pudo guardar la imagen", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public Resource cargar(String carpeta, String nombre) {
        validarCarpeta(carpeta);
        if (!StringUtils.hasText(nombre) || nombre.contains("/") || nombre.contains("\\")) {
            throw new ApiException("Archivo inválido", HttpStatus.BAD_REQUEST);
        }

        Path archivo = root.resolve(carpeta).resolve(nombre).normalize();
        validarRuta(archivo);

        try {
            Resource resource = new UrlResource(archivo.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new ApiException("Imagen no encontrada", HttpStatus.NOT_FOUND);
            }
            return resource;
        } catch (MalformedURLException ex) {
            throw new ApiException("Imagen no encontrada", HttpStatus.NOT_FOUND);
        }
    }

    public void eliminarSiEsLocal(String url) {
        if (!StringUtils.hasText(url)) return;

        int indice = url.indexOf("/api/media/");
        if (indice < 0) return;

        String ruta = url.substring(indice + "/api/media/".length());
        String[] partes = ruta.split("/", 2);
        if (partes.length != 2 || !CARPETAS_PERMITIDAS.contains(partes[0])) return;

        Path archivo = root.resolve(partes[0]).resolve(partes[1]).normalize();
        validarRuta(archivo);
        try {
            Files.deleteIfExists(archivo);
        } catch (IOException ignored) {
            // La actualización principal no debe fallar si la limpieza del archivo anterior falla.
        }
    }

    private String detectarExtension(byte[] contenido) {
        if (contenido.length >= 8
                && (contenido[0] & 0xff) == 0x89
                && contenido[1] == 0x50
                && contenido[2] == 0x4e
                && contenido[3] == 0x47) {
            return ".png";
        }
        if (contenido.length >= 3
                && (contenido[0] & 0xff) == 0xff
                && (contenido[1] & 0xff) == 0xd8
                && (contenido[2] & 0xff) == 0xff) {
            return ".jpg";
        }
        throw new ApiException("Solo se permiten imágenes JPG o PNG", HttpStatus.UNPROCESSABLE_ENTITY);
    }

    private void validarDimensiones(byte[] contenido) throws IOException {
        try(var stream=ImageIO.createImageInputStream(new ByteArrayInputStream(contenido))) {
            var readers=ImageIO.getImageReaders(stream);
            if(!readers.hasNext())throw new ApiException("El archivo no contiene una imagen válida",HttpStatus.UNPROCESSABLE_ENTITY);
            var reader=readers.next();
            try {reader.setInput(stream);int width=reader.getWidth(0),height=reader.getHeight(0);
                if(width>MAX_IMAGE_DIMENSION||height>MAX_IMAGE_DIMENSION||(long)width*height>MAX_PIXELS)
                    throw new ApiException("La imagen es demasiado grande; reducí su resolución",HttpStatus.UNPROCESSABLE_ENTITY);
                if(reader.read(0)==null)throw new ApiException("La imagen no es válida",HttpStatus.UNPROCESSABLE_ENTITY);
            } finally {reader.dispose();}
        }
    }

    private String construirUrl(String carpeta, String nombre) {
        String path = "/api/media/" + carpeta + "/" + nombre;
        return path;
    }

    private void validarCarpeta(String carpeta) {
        if (!CARPETAS_PERMITIDAS.contains(carpeta)) {
            throw new ApiException("Categoría de archivo inválida", HttpStatus.BAD_REQUEST);
        }
    }

    private void validarRuta(Path ruta) {
        if (!ruta.startsWith(root)) {
            throw new ApiException("Ruta de archivo inválida", HttpStatus.BAD_REQUEST);
        }
    }
}
