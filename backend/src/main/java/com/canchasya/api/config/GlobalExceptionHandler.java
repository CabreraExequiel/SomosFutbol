package com.canchasya.api.config;

import com.canchasya.api.exception.ApiException;
import lombok.extern.slf4j.Slf4j;
import com.canchasya.api.service.BitacoraService;
import lombok.RequiredArgsConstructor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class GlobalExceptionHandler {
    private final BitacoraService bitacora;

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, String>> handleApiException(ApiException ex) {
        Map<String, String> body = new HashMap<>();
        body.put("message", ex.getMessage());
        return ResponseEntity.status(ex.getStatus()).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new HashMap<>();
        int errorGeneral = 0;
        for (ObjectError error : ex.getBindingResult().getAllErrors()) {
            String campo = error instanceof FieldError fieldError
                    ? fieldError.getField()
                    : "solicitud" + (++errorGeneral);
            errores.putIfAbsent(campo, error.getDefaultMessage());
        }
        errores.put("message", String.join(". ", errores.values()));
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(errores);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, String>> handleAuthentication(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "Credenciales inválidas"));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleUnreadableBody(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", "El cuerpo de la solicitud contiene datos inválidos"));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> handleMaxUpload(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(Map.of("message", "La imagen supera el tamaño máximo permitido"));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Conflicto de integridad de datos", ex);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message", "La operación entra en conflicto con datos existentes"));
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<Map<String,String>> handleForbidden(Exception ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message","No tenés permiso para realizar esta operación"));
    }
    @ExceptionHandler({org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
            org.springframework.web.bind.MissingServletRequestParameterException.class})
    public ResponseEntity<Map<String,String>> handleBadParameter(Exception ex) {
        return ResponseEntity.badRequest().body(Map.of("message","Revisá los parámetros de la solicitud"));
    }
    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<Map<String,String>> handleNotFound(Exception ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message","Recurso no encontrado"));
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneric(Exception ex, HttpServletRequest solicitud) {

        log.error("Error no controlado: {}", ex.getClass().getSimpleName());

        Map<String, String> body = new HashMap<>();
        body.put("message", "Ocurrió un error interno");
        // El método asignado a la URL es una pista: no implica que el error haya ocurrido dentro de él.
        Object asignado = solicitud.getAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE);
        String claseRuta = null;
        String metodoRuta = null;
        if (asignado instanceof HandlerMethod handler) {
            claseRuta = handler.getBeanType().getName();
            metodoRuta = handler.getMethod().getName();
        }
        body.put("codigo", bitacora.errorServidor(ex, solicitud.getRequestURI(), claseRuta, metodoRuta));

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(body);
    }
}
