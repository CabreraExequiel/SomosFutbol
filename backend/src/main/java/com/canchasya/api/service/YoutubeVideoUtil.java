package com.canchasya.api.service;

import com.canchasya.api.exception.ApiException;
import org.springframework.http.HttpStatus;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Valida enlaces de YouTube y guarda solo el identificador seguro de 11 caracteres. */
public final class YoutubeVideoUtil {
    private static final Pattern ID = Pattern.compile("[A-Za-z0-9_-]{11}");
    private static final Pattern RUTA = Pattern.compile("/(?:shorts|live|embed)/([A-Za-z0-9_-]{11})/?");
    private static final Pattern CORTA = Pattern.compile("/([A-Za-z0-9_-]{11})/?");
    private static final Set<String> HOSTS = Set.of(
            "youtube.com", "www.youtube.com", "m.youtube.com", "music.youtube.com",
            "youtube-nocookie.com", "www.youtube-nocookie.com"
    );

    private YoutubeVideoUtil() { }

    public static String extraerId(String enlace) {
        if (enlace == null || enlace.isBlank()) return null;
        if (enlace.length() > 500) throw invalido();
        try {
            URI uri = URI.create(enlace.strip());
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getUserInfo() != null
                    || uri.getPort() != -1) throw invalido();
            String host = uri.getHost();
            if (host == null) throw invalido();
            host = host.toLowerCase(java.util.Locale.ROOT);
            String ruta = uri.getPath();
            String id = null;
            if ("youtu.be".equals(host)) {
                Matcher match = CORTA.matcher(ruta);
                if (match.matches()) id = match.group(1);
            } else if (HOSTS.contains(host)) {
                if ("/watch".equals(ruta)) {
                    String query = uri.getRawQuery();
                    if (query != null) {
                        for (String parametro : query.split("&")) {
                            String[] pareja = parametro.split("=", 2);
                            if (pareja.length == 2 && "v".equals(pareja[0])) {
                                id = URLDecoder.decode(pareja[1], StandardCharsets.UTF_8);
                                break;
                            }
                        }
                    }
                } else {
                    Matcher match = RUTA.matcher(ruta);
                    if (match.matches()) id = match.group(1);
                }
            }
            if (id == null || !ID.matcher(id).matches()) throw invalido();
            return id;
        } catch (IllegalArgumentException ex) {
            throw invalido();
        }
    }

    private static ApiException invalido() {
        return new ApiException("Enlace de YouTube inválido", HttpStatus.BAD_REQUEST);
    }
}
