package com.canchasya.api.service;

import com.canchasya.api.model.BitacoraEvento;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.sql.SQLException;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BitacoraService {
    private final BitacoraWriter writer;

    // Establecer APP_ENVIRONMENT=production en Render y APP_ENVIRONMENT=local en desarrollo.
    @Value("${APP_ENVIRONMENT:local}")
    private String ambiente;

    /** Errores inesperados: guardar únicamente categoría, ruta sin query y código de referencia. */
    public String errorServidor(Exception exception, String ruta) {
        return errorServidor(exception, ruta, null, null);
    }

    /**
     * Los marcos de la excepción tienen prioridad. Si no hay marco propio, se guarda
     * el controlador que recibió la petición, como CONTEXTO y no como origen probado.
     */
    public String errorServidor(Exception exception, String ruta,
                                String claseRuta, String metodoRuta) {
        String codigo = codigo();
        BitacoraEvento evento = nuevo(codigo, "ERROR", "BACKEND", "EXCEPCION_NO_CONTROLADA",
                "Error interno: " + claseSegura(exception.getClass().getSimpleName()));
        evento.setRuta(rutaSegura(ruta));
        evento.setClaseOrigen(claseSeguraNombre(exception.getClass().getName(), 180));
        StackTraceElement marco = primerMarcoDeAplicacion(exception);
        boolean metodoSoloAsociado = false;
        if (marco != null) {
            evento.setClaseOrigen(claseSeguraNombre(marco.getClassName(), 180));
            evento.setFuncionOrigen(claseSeguraNombre(marco.getMethodName(), 120));
            evento.setArchivoOrigen(claseSeguraNombre(marco.getFileName(), 150));
            if (marco.getLineNumber() > 0) evento.setLineaOrigen(marco.getLineNumber());
        } else if (claseRuta != null && claseRuta.startsWith("com.canchasya.api.controller.")
                && metodoRuta != null) {
            evento.setClaseOrigen(claseSeguraNombre(claseRuta, 180));
            evento.setFuncionOrigen(claseSeguraNombre(metodoRuta, 120));
            metodoSoloAsociado = true;
        }
        SQLException sql = causaSql(exception);
        if (sql != null) {
            String estado = sql.getSQLState();
            if (estado != null && estado.matches("[A-Z0-9]{5}")) evento.setSqlState(estado);
            if (sql.getErrorCode() > 0) evento.setCodigoSql(sql.getErrorCode());
        }
        String orientacion = causaConocida(exception, sql);
        if (exception instanceof org.springframework.web.context.request.async.AsyncRequestNotUsableException) {
            orientacion = "Respuesta HTTP inutilizable durante el envío: posible desconexión/cancelación del cliente. "
                    + "No confirma un defecto del video ni del controlador.";
        }
        if (metodoSoloAsociado) {
            orientacion += " La función mostrada atendió la ruta, NO es el lugar probado de la excepción.";
        }
        evento.setCausaProbable(orientacion);
        intentarGuardar(evento);
        return codigo;
    }

    /** Error del navegador: sólo campos estructurados, nunca stack ni mensaje arbitrario. */
    public String errorFrontend(String nombreExcepcion, String ruta, String funcionInformada,
                                String claseInformada, String archivoInformado, Integer lineaInformada,
                                Long actorId) {
        String codigo = codigo();
        BitacoraEvento evento = nuevo(codigo, "ERROR", "FRONTEND", "ERROR_INTERFAZ",
                "Excepción de interfaz: " + claseSegura(nombreExcepcion));
        evento.setActorId(actorId);
        evento.setRuta(rutaSegura(ruta));
        // El navegador puede manipular estos campos: son pistas orientativas, no prueba fiable.
        evento.setFuncionOrigen(simboloFrontend(funcionInformada, 120));
        evento.setClaseOrigen(simboloFrontend(claseInformada, 180));
        if (archivoInformado != null && archivoInformado.matches("[A-Za-z0-9_.-]{1,150}\\.ts")) {
            evento.setArchivoOrigen(archivoInformado);
        }
        if (lineaInformada != null && lineaInformada > 0 && lineaInformada <= 1_000_000) {
            evento.setLineaOrigen(lineaInformada);
        }
        evento.setCausaProbable("Error JavaScript no controlado; revisar consola y mapa de fuentes (source maps) de Angular.");
        intentarGuardar(evento);
        return codigo;
    }

    private static String simboloFrontend(String entrada, int max) {
        if (entrada == null || entrada.isBlank() || entrada.length() > max) return null;
        return entrada.matches("[A-Za-z_$][A-Za-z0-9_$.]{0,179}") ? entrada : null;
    }

    /** Se guarda DESPUÉS de confirmar la transacción de negocio, para no auditar rollbacks. */
    public void evento(String tipo, String referenciaTipo, Long referenciaId, Long actorId) {
        Runnable accion = () -> {
            BitacoraEvento registro = nuevo(codigo(), "INFO", "AUDITORIA", tipo,
                    "Acción registrada: " + tipo.replace('_', ' ').toLowerCase(Locale.ROOT));
            registro.setActorId(actorId);
            registro.setReferenciaTipo(referenciaTipo);
            registro.setReferenciaId(referenciaId);
            intentarGuardar(registro);
        };
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { accion.run(); }
            });
        } else {
            accion.run();
        }
    }

    /** Busca el primer método de la aplicación dentro de la cadena de excepciones.
     *  No se muestra como certeza absoluta del origen lógico del fallo.
     */
    private static StackTraceElement primerMarcoDeAplicacion(Throwable error) {
        Throwable actual = error;
        int profundidad = 0;
        while (actual != null && profundidad++ < 20) {
            for (StackTraceElement marco : actual.getStackTrace()) {
                String nombre = marco.getClassName();
                if ((nombre.startsWith("com.canchasya.api.service.")
                        || nombre.startsWith("com.canchasya.api.repository.")
                        || nombre.startsWith("com.canchasya.api.controller."))
                        && !nombre.contains("BitacoraService") && !nombre.contains("BitacoraWriter")) {
                    return marco;
                }
            }
            actual = actual.getCause();
        }
        return null;
    }

    private static SQLException causaSql(Throwable error) {
        Throwable actual = error;
        int profundidad = 0;
        while (actual != null && profundidad++ < 20) {
            if (actual instanceof SQLException sql) return sql;
            actual = actual.getCause();
        }
        return null;
    }

    /** Texto predefinido y seguro, nunca usar getMessage() de SQL ni stack completo. */
    private static String causaConocida(Throwable error, SQLException sql) {
        if (sql != null) {
            return switch (sql.getErrorCode()) {
                case 1064 -> "Sintaxis SQL incompatible. Revisar consulta generada, dialecto de Hibernate y versión de MariaDB.";
                case 1146 -> "No existe una tabla requerida. Verificar migraciones y base de datos configurada.";
                case 1054 -> "Columna SQL inexistente. Revisar entidades JPA y migraciones pendientes.";
                case 1213 -> "Transacción bloqueada por deadlock; revisar concurrencia y reintentos.";
                case 1205 -> "Tiempo de espera agotado por bloqueo de base de datos.";
                default -> "Error de acceso a base de datos. Revisar código SQL y logs del servidor usando el identificador.";
            };
        }
        if (error instanceof org.springframework.dao.DataAccessException)
            return "Fallo de acceso a datos. Revisar repositorios y conexión SQL.";
        return "Excepción inesperada; revisar el método señalado y los logs de Spring Boot.";
    }

    private static String claseSeguraNombre(String entrada, int longitud) {
        if (entrada == null) return null;
        String filtrada = entrada.replaceAll("[^a-zA-Z0-9_.$-]", "");
        return filtrada.isBlank() ? null : filtrada.substring(0, Math.min(filtrada.length(), longitud));
    }

    private BitacoraEvento nuevo(String codigo, String nivel, String origen, String tipo, String descripcion) {
        BitacoraEvento e = new BitacoraEvento();
        e.setFecha(Instant.now());
        e.setCodigo(codigo);
        e.setNivel(nivel);
        e.setOrigen(origen);
        e.setTipo(tipo);
        e.setDescripcion(descripcion);
        e.setAmbiente(ambiente != null && ambiente.equalsIgnoreCase("production") ? "production" : "local");
        return e;
    }

    private void intentarGuardar(BitacoraEvento e) {
        try {
            // Un fallo de auditoría nunca debe impedir reservas ni enmascarar el error original.
            writer.guardar(e);
        } catch (Exception guardadoFallido) {
            log.warn("Bitácora no disponible para código {}. Revisar la tabla y la conexión MySQL.", e.getCodigo());
        }
    }

    private static String codigo() {
        return "SF-" + UUID.randomUUID().toString().toUpperCase(Locale.ROOT);
    }
    private static String claseSegura(String nombre) {
        if (nombre == null || nombre.isBlank()) return "Error";
        String seguro = nombre.replaceAll("[^a-zA-Z0-9_-]", "");
        return seguro.substring(0, Math.min(60, seguro.length()));
    }
    private static String rutaSegura(String ruta) {
        if (ruta == null || ruta.isBlank()) return null;
        String sinParametros = ruta.split("[?#]", 2)[0];
        String limpia = sinParametros.replaceAll("[^a-zA-Z0-9/_\\.-]", "_");
        return limpia.substring(0, Math.min(180, limpia.length()));
    }
}
