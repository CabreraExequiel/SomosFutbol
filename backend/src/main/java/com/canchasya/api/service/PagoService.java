package com.canchasya.api.service;

import com.canchasya.api.dto.PagoResponse;
import com.canchasya.api.exception.ApiException;
import com.canchasya.api.model.EstadoPago;
import com.canchasya.api.model.EstadoReserva;
import com.canchasya.api.model.Pago;
import com.canchasya.api.model.Reserva;
import com.canchasya.api.model.Role;
import com.canchasya.api.model.TipoNotificacion;
import com.canchasya.api.model.User;
import com.canchasya.api.repository.PagoRepository;
import com.canchasya.api.repository.ReservaRepository;
import com.mercadopago.exceptions.MPInvalidWebhookSignatureException;
import com.mercadopago.webhook.WebhookSignatureValidator;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.payments.enabled", havingValue = "true")
public class PagoService {

    private static final String MERCADO_PAGO_API = "https://api.mercadopago.com";
    private static final Set<String> RESULTADOS_RETORNO =
            Set.of("aprobado", "pendiente", "rechazado");

    private final PagoRepository pagoRepository;
    private final ReservaRepository reservaRepository;
    private final WebClient webClient;
    private final MailService mailService;
    private final NotificacionService notificacionService;

    private final MercadoPagoCuentaService cuentasMp;
    private final java.time.Clock clock;
    private final com.canchasya.api.repository.CanchaRepository canchaRepository;

    @Value("${app.mercadopago.webhook-secret:}")
    private String webhookSecret;

    @Value("${app.mercadopago.test-mode:true}")
    private boolean testMode;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    @Value("${app.backend-public-url:}")
    private String backendPublicUrl;

    @PostConstruct
    void validarConfiguracion() {
        if (esVacio(webhookSecret)) {
            throw new IllegalStateException(
                    "PAYMENTS_ENABLED=true requiere MERCADOPAGO_WEBHOOK_SECRET"
            );
        }

        backendPublicUrl = normalizarUrlBase(backendPublicUrl, "BACKEND_PUBLIC_URL");
        frontendUrl = normalizarUrlBase(frontendUrl, "FRONTEND_URL");

        URI backendUri = URI.create(backendPublicUrl);
        if (!"https".equalsIgnoreCase(backendUri.getScheme())) {
            throw new IllegalStateException(
                    "BACKEND_PUBLIC_URL debe ser una URL HTTPS pública para Mercado Pago"
            );
        }

        log.info(
                "Mercado Pago configurado. testMode={}, backendPublicUrl={}",
                testMode,
                backendPublicUrl
        );
    }

    @Transactional
    public PagoResponse crearPreferencia(Long reservaId, User user) {
        Reserva reserva = reservaRepository.findByIdForUpdate(reservaId)
                .orElseThrow(() ->
                        new ApiException("Reserva no encontrada", HttpStatus.NOT_FOUND)
                );

        if (!reserva.getUser().getId().equals(user.getId())) {
            throw new ApiException(
                    "Solo quien realizó la reserva puede pagarla",
                    HttpStatus.FORBIDDEN
            );
        }

        if (!reserva.isPagoRequerido()) throw new ApiException("Esta reserva usa transferencia manual",HttpStatus.CONFLICT);
        if (reserva.getExpiresAt()!=null && !reserva.getExpiresAt().isAfter(clock.instant())) throw new ApiException("La reserva venció. Creá una nueva",HttpStatus.CONFLICT);
        if (reserva.getEstado() == EstadoReserva.CANCELADA) {
            throw new ApiException(
                    "La reserva está cancelada",
                    HttpStatus.CONFLICT
            );
        }

        if (reserva.getEstado() == EstadoReserva.CONFIRMADA) {
            throw new ApiException(
                    "La reserva ya está confirmada",
                    HttpStatus.CONFLICT
            );
        }

        Pago pago = pagoRepository.findByReservaId(reservaId).orElseGet(() -> {
            Pago nuevo = new Pago();
            nuevo.setReserva(reserva);
            nuevo.setMonto(reserva.getMonto());
            nuevo.setEstado(EstadoPago.PENDIENTE);
            return pagoRepository.save(nuevo);
        });

        if (pago.getEstado() == EstadoPago.APROBADO) {
            throw new ApiException(
                    "La reserva ya fue pagada",
                    HttpStatus.CONFLICT
            );
        }

        if (pago.getEstado() == EstadoPago.REEMBOLSADO) {
            throw new ApiException(
                    "El pago de esta reserva fue reembolsado",
                    HttpStatus.CONFLICT
            );
        }

        if (!esVacio(pago.getCheckoutUrl()) && !esVacio(pago.getMpPreferenceId())) {
            pago.setEstado(EstadoPago.PENDIENTE);

            log.info(
                    "Reutilizando preferencia MP {} para pago local {} / reserva {}",
                    pago.getMpPreferenceId(),
                    pago.getId(),
                    reserva.getId()
            );

            return PagoResponse.fromEntity(
                    pagoRepository.save(pago),
                    pago.getCheckoutUrl()
            );
        }

        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", "reserva-" + reserva.getId());
        item.put("title", "Reserva de " + reserva.getCancha().getNombre());
        item.put("description", descripcionReserva(reserva));
        item.put("quantity", 1);
        item.put("unit_price", reserva.getMonto());
        item.put("currency_id", "ARS");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("items", List.of(item));
        body.put("payer", Map.of("email", reserva.getUser().getEmail()));
        body.put("external_reference", pago.getId().toString());
        body.put("metadata", Map.of(
                "pago_id", pago.getId(),
                "reserva_id", reserva.getId()
        ));
        body.put("back_urls", Map.of(
                "success", urlRetorno("aprobado"),
                "failure", urlRetorno("rechazado"),
                "pending", urlRetorno("pendiente")
        ));
        body.put("auto_return", "approved");
        body.put("binary_mode", true);

        body.put("notification_url",backendPublicUrl+"/api/pagos/webhook?seller_id="+reserva.getCancha().getDueno().getId());
        if(reserva.getExpiresAt()!=null){body.put("expires",true);body.put("expiration_date_from",clock.instant().atOffset(java.time.ZoneOffset.UTC).toString());body.put("expiration_date_to",reserva.getExpiresAt().atOffset(java.time.ZoneOffset.UTC).toString());}
        Map<String, Object> response = postPreferencia(body,cuentasMp.accessToken(reserva.getCancha().getDueno().getId()));
        String preferenceId = texto(response.get("id"));
        String initPoint = primerTexto(
                response.get(testMode?"sandbox_init_point":"init_point"),
                response.get("init_point")
        );

        if (esVacio(preferenceId) || !esUrlHttps(initPoint)) {
            log.error(
                    "Mercado Pago devolvió una preferencia incompleta para el pago local {}",
                    pago.getId()
            );
            throw new ApiException(
                    "Mercado Pago devolvió una respuesta inválida",
                    HttpStatus.BAD_GATEWAY
            );
        }

        pago.setMpPreferenceId(preferenceId);
        pago.setCheckoutUrl(initPoint);
        pago.setEstado(EstadoPago.PENDIENTE);
        pagoRepository.save(pago);

        log.info(
                "Preferencia MP creada. pagoLocal={}, reserva={}, preferenceId={}, testMode={}",
                pago.getId(),
                reserva.getId(),
                preferenceId,
                testMode
        );

        return PagoResponse.fromEntity(pago, initPoint);
    }

    @Transactional(readOnly = true)
    public PagoResponse obtenerPorReserva(Long reservaId, User user) {
        Reserva reserva = reservaRepository.findById(reservaId)
                .orElseThrow(() ->
                        new ApiException("Reserva no encontrada", HttpStatus.NOT_FOUND)
                );

        validarAcceso(reserva, user);

        Pago pago = pagoRepository.findByReservaId(reservaId)
                .orElseThrow(() ->
                        new ApiException(
                                "La reserva todavía no tiene un pago",
                                HttpStatus.NOT_FOUND
                        )
                );

        boolean puedeContinuarPago =
                reserva.getUser().getId().equals(user.getId())
                        && pago.getEstado() != EstadoPago.APROBADO
                        && pago.getEstado() != EstadoPago.REEMBOLSADO;

        return PagoResponse.fromEntity(
                pago,
                puedeContinuarPago ? pago.getCheckoutUrl() : null
        );
    }

    public void validarFirmaWebhook(
            String xSignature,
            String xRequestId,
            String dataId
    ) {
        if (esVacio(xSignature) || esVacio(xRequestId) || esVacio(dataId)) {
            log.warn("Webhook de Mercado Pago sin encabezados de firma completos");
            throw new ApiException(
                    "Firma de webhook inválida",
                    HttpStatus.UNAUTHORIZED
            );
        }

        try {
            WebhookSignatureValidator.validate(
                    xSignature,
                    xRequestId,
                    dataId,
                    webhookSecret
            );
        } catch (MPInvalidWebhookSignatureException | IllegalArgumentException ex) {
            log.warn(
                    "Webhook de Mercado Pago con firma inválida. requestId={}",
                    xRequestId
            );
            throw new ApiException(
                    "Firma de webhook inválida",
                    HttpStatus.UNAUTHORIZED
            );
        }
    }

    @Transactional
    public void procesarWebhook(String paymentId, Long duenoId) {
        if (paymentId == null || !paymentId.matches("\\d+")) {
            log.warn("Webhook de Mercado Pago con identificador no válido");
            return;
        }

        if(duenoId==null) return;
        String token=cuentasMp.accessToken(duenoId);
        Map<String, Object> pagoMp = obtenerPagoMercadoPago(paymentId,token);
        if(!paymentId.equals(texto(pagoMp.get("id"))) || !cuentasMp.collector(duenoId).equals(texto(pagoMp.get("collector_id")))) {
            log.warn("Pago con identificador o vendedor diferente al esperado");return;
        }

        String externalReference = texto(pagoMp.get("external_reference"));
        if (esVacio(externalReference) || !externalReference.matches("\\d+")) {
            log.warn(
                    "Pago de Mercado Pago {} sin referencia local válida",
                    paymentId
            );
            return;
        }

        Long pagoId;
        try {
            pagoId = Long.valueOf(externalReference);
        } catch (NumberFormatException ex) {
            log.warn(
                    "Pago de Mercado Pago {} con external_reference inválida: {}",
                    paymentId,
                    externalReference
            );
            return;
        }

        Pago referenciaPago = pagoRepository.findById(pagoId).orElse(null);

        if (referenciaPago == null) {
            log.warn(
                    "No existe el pago local {} informado por Mercado Pago",
                    pagoId
            );
            return;
        }

        canchaRepository.findByIdForUpdate(referenciaPago.getReserva().getCancha().getId())
            .orElseThrow(() -> new ApiException("Cancha no encontrada",HttpStatus.NOT_FOUND));
        Reserva reservaBloqueada = reservaRepository.findByIdForUpdate(
                referenciaPago.getReserva().getId()
        ).orElse(null);

        Pago pago = pagoRepository.findByIdForUpdate(pagoId).orElse(null);

        if (reservaBloqueada == null || pago == null) {
            log.warn(
                    "La reserva o el pago local {} dejaron de existir durante el procesamiento",
                    pagoId
            );
            return;
        }

        if(!pago.getReserva().getCancha().getDueno().getId().equals(duenoId)) {log.warn("Pago de otro propietario ignorado");return;}
        if(esVacio(texto(pagoMp.get("preference_id"))) && pagoMp.get("order") instanceof Map<?,?> order && texto(order.get("id"))!=null && texto(order.get("id")).matches("\\d+")) {
            var merchant=obtenerRecurso("/merchant_orders/"+order.get("id"),token);
            pagoMp=new LinkedHashMap<>(pagoMp);pagoMp.put("preference_id",merchant.get("preference_id"));
        }
        if (!validarDatosPago(pago, pagoMp, paymentId)) {
            return;
        }

        String estadoRemoto=texto(pagoMp.get("status"));
        if(pago.getEstado()==EstadoPago.REEMBOLSADO) return;
        if(pago.getEstado()==EstadoPago.APROBADO && !(paymentId.equals(pago.getMpPaymentId()) && Set.of("refunded","charged_back").contains(estadoRemoto==null?"":estadoRemoto))) return;

        Pago pagoConMismoId =
                pagoRepository.findByMpPaymentId(paymentId).orElse(null);

        if (pagoConMismoId != null
                && !pagoConMismoId.getId().equals(pago.getId())) {

            log.error(
                    "El pago Mercado Pago {} ya está vinculado al pago local {}",
                    paymentId,
                    pagoConMismoId.getId()
            );
            return;
        }

        String status = texto(pagoMp.get("status"));

        pago.setMpPaymentId(paymentId);
        pago.setMetodo(texto(pagoMp.get("payment_method_id")));

        log.info(
                "Procesando pago MP. paymentId={}, pagoLocal={}, reserva={}, status={}",
                paymentId,
                pago.getId(),
                reservaBloqueada.getId(),
                status
        );

        switch (status == null ? "" : status.toLowerCase(Locale.ROOT)) {
            case "approved" -> aprobar(pago, pagoMp);
            case "rejected", "cancelled" -> pago.setEstado(EstadoPago.RECHAZADO);
            case "refunded", "charged_back" -> reembolsar(pago);
            default -> pago.setEstado(EstadoPago.PENDIENTE);
        }

        pagoRepository.save(pago);

        log.info(
                "Pago MP procesado. paymentId={}, estadoPago={}, estadoReserva={}",
                paymentId,
                pago.getEstado(),
                reservaBloqueada.getEstado()
        );
    }

    public URI construirRetornoFrontend(
            String resultado,
            String paymentId,
            String preferenceId
    ) {
        String resultadoSeguro =
                RESULTADOS_RETORNO.contains(resultado)
                        ? resultado
                        : "pendiente";

        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(frontendUrl)
                .path("/reservas")
                .queryParam("pago", resultadoSeguro);

        if (paymentId != null && paymentId.matches("\\d+")) {
            builder.queryParam("paymentId", paymentId);
        }

        if (preferenceId != null
                && preferenceId.matches("[A-Za-z0-9-]+")) {
            builder.queryParam("preferenceId", preferenceId);
        }

        return builder.build().encode().toUri();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> postPreferencia(Map<String, Object> body, String accessToken) {
        try {
            Map<String, Object> response = webClient.post()
                    .uri(MERCADO_PAGO_API + "/checkout/preferences")
                    .headers(headers -> {
                        headers.setBearerAuth(accessToken);
                        headers.set(
                                "X-Idempotency-Key",
                                "somos-futbol-pago-" + body.get("external_reference")
                        );
                    })
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block(Duration.ofSeconds(12));

            if (response == null) {
                throw new ApiException(
                        "Mercado Pago devolvió una respuesta vacía",
                        HttpStatus.BAD_GATEWAY
                );
            }

            return response;

        } catch (WebClientResponseException ex) {
            log.warn(
                    "Mercado Pago rechazó la preferencia. status={}, body={}",
                    ex.getStatusCode().value(),
                    "respuesta omitida"
            );
            throw new ApiException(
                    "No se pudo iniciar el pago en Mercado Pago",
                    HttpStatus.BAD_GATEWAY
            );

        } catch (ApiException ex) {
            throw ex;

        } catch (RuntimeException ex) {
            log.warn(
                    "No se pudo conectar con Mercado Pago: {}",
                    ex.getClass().getSimpleName()
            );
            throw new ApiException(
                    "Mercado Pago no está disponible temporalmente",
                    HttpStatus.BAD_GATEWAY
            );
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> obtenerPagoMercadoPago(String paymentId, String accessToken) {
        try {
            Map<String, Object> response = webClient.get()
                    .uri(MERCADO_PAGO_API + "/v1/payments/" + paymentId)
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block(Duration.ofSeconds(12));

            if (response == null) {
                throw new ApiException(
                        "Mercado Pago devolvió una respuesta vacía",
                        HttpStatus.BAD_GATEWAY
                );
            }

            return response;

        } catch (WebClientResponseException ex) {
            log.warn(
                    "No se pudo consultar el pago {}. status={}, body={}",
                    paymentId,
                    ex.getStatusCode().value(),
                    "respuesta omitida"
            );
            throw new ApiException(
                    "No se pudo verificar el pago",
                    HttpStatus.BAD_GATEWAY
            );

        } catch (ApiException ex) {
            throw ex;

        } catch (RuntimeException ex) {
            log.warn(
                    "Error consultando el pago {}: {}",
                    paymentId,
                    ex.getClass().getSimpleName()
            );
            throw new ApiException(
                    "Mercado Pago no está disponible temporalmente",
                    HttpStatus.BAD_GATEWAY
            );
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String,Object> obtenerRecurso(String path,String token) {
        try {var r=webClient.get().uri(MERCADO_PAGO_API+path).headers(h->h.setBearerAuth(token)).retrieve().bodyToMono(Map.class).block(Duration.ofSeconds(10));
            if(r==null)throw new IllegalStateException();return r;
        } catch(RuntimeException e){throw new ApiException("No se pudo verificar la preferencia del pago",HttpStatus.BAD_GATEWAY);}
    }

    private boolean validarDatosPago(
            Pago pago,
            Map<String, Object> pagoMp,
            String paymentId
    ) {
        String currency = texto(pagoMp.get("currency_id"));
        BigDecimal montoMp = decimal(pagoMp.get("transaction_amount"));
        String preferenceId = texto(pagoMp.get("preference_id"));
        Boolean liveMode = booleano(pagoMp.get("live_mode"));

        boolean montoValido =
                montoMp != null
                        && pago.getMonto() != null
                        && montoMp.compareTo(pago.getMonto()) == 0;

        boolean monedaValida = "ARS".equalsIgnoreCase(currency);

        boolean preferenceExacta =
                !esVacio(preferenceId)
                        && !esVacio(pago.getMpPreferenceId())
                        && preferenceId.equals(pago.getMpPreferenceId());

        boolean metadataValida = validarMetadata(pago, pagoMp);

        boolean preferenciaValida = preferenceExacta;
        boolean entornoValido = liveMode != null && liveMode == !testMode;

        log.info(
                "Validación MP paymentId={}, pagoLocal={}, mpPreference={}, localPreference={}, " +
                        "liveMode={}, testMode={}, monto={}, moneda={}, metadata={}, preferencia={}, entorno={}",
                paymentId,
                pago.getId(),
                preferenceId,
                pago.getMpPreferenceId(),
                liveMode,
                testMode,
                montoValido,
                monedaValida,
                metadataValida,
                preferenciaValida,
                entornoValido
        );

        if (!montoValido
                || !monedaValida
                || !metadataValida
                || !preferenciaValida
                || !entornoValido) {

            log.error(
                    "Pago Mercado Pago {} rechazado por validación. " +
                            "monto={}, moneda={}, metadata={}, preferencia={}, entorno={}",
                    paymentId,
                    montoValido,
                    monedaValida,
                    metadataValida,
                    preferenciaValida,
                    entornoValido
            );
            return false;
        }

        return true;
    }

    private boolean validarMetadata(
            Pago pago,
            Map<String, Object> pagoMp
    ) {
        Object metadataObj = pagoMp.get("metadata");

        if (!(metadataObj instanceof Map<?, ?> metadata)) {
            return false;
        }

        String pagoIdMetadata = texto(metadata.get("pago_id"));
        String reservaIdMetadata = texto(metadata.get("reserva_id"));

        boolean pagoIdValido =
                pago.getId().toString().equals(pagoIdMetadata);

        boolean reservaIdValido =
                (
                        pago.getReserva() != null
                                && pago.getReserva().getId() != null
                                && pago.getReserva().getId().toString()
                                .equals(reservaIdMetadata)
                );

        return pagoIdValido && reservaIdValido;
    }

    private void aprobar(Pago pago, Map<String, Object> pagoMp) {
        boolean primeraAprobacion =
                pago.getEstado() != EstadoPago.APROBADO;

        Reserva reserva = pago.getReserva();

        pago.setEstado(EstadoPago.APROBADO);
        pago.setFechaPago(
                fechaAprobacion(pagoMp.get("date_approved"))
        );

        if (reserva.getEstado() == EstadoReserva.CANCELADA ||
            (reserva.getExpiresAt()!=null && !reserva.getExpiresAt().isAfter(clock.instant()))) {
            reserva.setEstado(EstadoReserva.CANCELADA);reservaRepository.save(reserva);
            if (primeraAprobacion) notificacionService.crear(reserva.getCancha().getDueno(),null,TipoNotificacion.PAGO,
                "Reintegro pendiente","Llegó un pago para una reserva cancelada o vencida. Revisá el cobro y gestioná el reintegro desde Mercado Pago.","/mis-canchas",reserva.getId());
            if (primeraAprobacion) {
                notificarSeguro(
                        () -> notificacionService.crear(
                                reserva.getUser(),
                                null,
                                TipoNotificacion.PAGO,
                                "Pago recibido en una reserva cancelada",
                                "Recibimos el pago de "
                                        + reserva.getCancha().getNombre()
                                        + ". El equipo deberá revisar el reintegro.",
                                "/reservas",
                                reserva.getId()
                        ),
                        "notificación de pago en reserva cancelada"
                );
            }

            log.error(
                    "Se aprobó el pago {} para la reserva cancelada {}",
                    pago.getMpPaymentId(),
                    reserva.getId()
            );
            return;
        }

        reserva.setEstado(EstadoReserva.CONFIRMADA);
        reservaRepository.save(reserva);

        log.info(
                "Reserva {} confirmada por pago MP {}",
                reserva.getId(),
                pago.getMpPaymentId()
        );

        if (!primeraAprobacion) {
            return;
        }

        notificarSeguro(
                () -> mailService.enviarTicketReserva(reserva),
                "envío del ticket por correo"
        );

        notificarSeguro(
                () -> notificacionService.crear(
                        reserva.getUser(),
                        null,
                        TipoNotificacion.PAGO,
                        "Pago aprobado",
                        "Tu reserva de "
                                + reserva.getCancha().getNombre()
                                + " quedó confirmada.",
                        "/reservas",
                        reserva.getId()
                ),
                "notificación al jugador"
        );

        notificarSeguro(
                () -> notificacionService.crear(
                        reserva.getCancha().getDueno(),
                        reserva.getUser(),
                        TipoNotificacion.PAGO,
                        "Reserva pagada",
                        reserva.getUser().getName()
                                + " pagó la reserva de "
                                + reserva.getCancha().getNombre()
                                + ".",
                        "/mis-canchas",
                        reserva.getId()
                ),
                "notificación al dueño"
        );
    }

    private void reembolsar(Pago pago) {
        boolean cambio =
                pago.getEstado() != EstadoPago.REEMBOLSADO;

        pago.setEstado(EstadoPago.REEMBOLSADO);

        Reserva reserva = pago.getReserva();

        if (reserva.getEstado() != EstadoReserva.CANCELADA) {
            reserva.setEstado(EstadoReserva.CANCELADA);
            reservaRepository.save(reserva);
        }

        if (cambio) {
            notificarSeguro(
                    () -> notificacionService.crear(
                            reserva.getUser(),
                            null,
                            TipoNotificacion.PAGO,
                            "Pago reembolsado",
                            "El pago de la reserva de "
                                    + reserva.getCancha().getNombre()
                                    + " fue reembolsado.",
                            "/reservas",
                            reserva.getId()
                    ),
                    "notificación de reembolso"
            );
        }
    }

    private void validarAcceso(Reserva reserva, User user) {
        boolean esDueno =
                reserva.getCancha()
                        .getDueno()
                        .getId()
                        .equals(user.getId());

        boolean esPropia =
                reserva.getUser()
                        .getId()
                        .equals(user.getId());

        if (!esDueno
                && !esPropia
                && user.getRole() != Role.ADMINISTRADOR) {

            throw new ApiException(
                    "No tenés permiso para consultar este pago",
                    HttpStatus.FORBIDDEN
            );
        }
    }

    private String urlRetorno(String resultado) {
        return backendPublicUrl
                + "/api/pagos/retorno/"
                + resultado;
    }

    private String descripcionReserva(Reserva reserva) {
        return "%s - %s de %s a %s".formatted(
                reserva.getFecha(),
                reserva.getCancha().getNombre(),
                reserva.getHoraInicio(),
                reserva.getHoraFin()
        );
    }

    private LocalDateTime fechaAprobacion(Object valor) {
        String fecha = texto(valor);

        if (fecha != null) {
            try {
                return OffsetDateTime
                        .parse(fecha)
                        .toLocalDateTime();
            } catch (RuntimeException ignored) {
                // Si el formato cambia, usamos la hora local.
            }
        }

        return LocalDateTime.now();
    }

    private BigDecimal decimal(Object valor) {
        if (valor == null) {
            return null;
        }

        try {
            return new BigDecimal(String.valueOf(valor));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Boolean booleano(Object valor) {
        if (valor instanceof Boolean booleanValue) {
            return booleanValue;
        }

        if (valor == null) {
            return null;
        }

        return Boolean.valueOf(String.valueOf(valor));
    }

    private String primerTexto(Object primero, Object segundo) {
        String valor = texto(primero);
        return esVacio(valor)
                ? texto(segundo)
                : valor;
    }

    private String texto(Object valor) {
        if (valor == null) {
            return null;
        }

        String texto = String.valueOf(valor).trim();

        return texto.isEmpty()
                || "null".equalsIgnoreCase(texto)
                ? null
                : texto;
    }

    private boolean esUrlHttps(String valor) {
        if (esVacio(valor)) {
            return false;
        }

        try {
            URI uri = URI.create(valor);

            return "https".equalsIgnoreCase(uri.getScheme())
                    && uri.getHost() != null;

        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private String normalizarUrlBase(
            String valor,
            String variable
    ) {
        if (esVacio(valor)) {
            throw new IllegalStateException(
                    variable
                            + " es obligatoria cuando los pagos están habilitados"
            );
        }

        String limpia =
                valor.trim().replaceAll("/+$", "");

        try {
            URI uri = URI.create(limpia);

            if (uri.getScheme() == null
                    || uri.getHost() == null) {
                throw new IllegalArgumentException();
            }

            return limpia;

        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException(
                    variable + " no contiene una URL válida"
            );
        }
    }

    private void notificarSeguro(
            Runnable accion,
            String descripcion
    ) {
        try {
            accion.run();
        } catch (RuntimeException ex) {
            log.error(
                    "El pago quedó procesado, pero falló {}: {}",
                    descripcion,
                    ex.getClass().getSimpleName()
            );
        }
    }

    private String limitarTexto(String valor) {
        if (valor == null) {
            return "";
        }

        String limpio = valor.replaceAll("[\\r\\n]+", " ").trim();

        if (limpio.length() <= 500) {
            return limpio;
        }

        return limpio.substring(0, 500) + "...";
    }

    private boolean esVacio(String valor) {
        return valor == null || valor.isBlank();
    }
}
