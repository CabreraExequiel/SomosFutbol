package com.canchasya.api.controller;

import com.canchasya.api.dto.PagoResponse;
import com.canchasya.api.model.User;
import com.canchasya.api.service.PagoService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/api/pagos")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.payments.enabled", havingValue = "true")
public class PagoController {

    private final PagoService pagoService;
    private final com.canchasya.api.service.MercadoPagoCuentaService cuentasMp;

    @PostMapping("/reservas/{reservaId}/preferencia")
    public ResponseEntity<PagoResponse> crearPreferencia(
            @PathVariable Long reservaId,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(pagoService.crearPreferencia(reservaId, user));
    }

    @GetMapping("/reservas/{reservaId}")
    public ResponseEntity<PagoResponse> obtenerPorReserva(
            @PathVariable Long reservaId,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(pagoService.obtenerPorReserva(reservaId, user));
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(
            @RequestBody(required = false) Map<String, Object> body,
            @RequestParam(required = false) String topic,
            @RequestParam(required = false, name="seller_id") Long sellerId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false, name = "data.id") String dataId,
            @RequestHeader(required = false, name = "x-signature") String xSignature,
            @RequestHeader(required = false, name = "x-request-id") String xRequestId
    ) {
        String tipoEvento = type != null ? type : topic;
        if (tipoEvento != null && !"payment".equalsIgnoreCase(tipoEvento)) {
            return ResponseEntity.ok().build();
        }

        String paymentId = dataId != null ? dataId : extraerPaymentId(body);
        pagoService.validarFirmaWebhook(xSignature, xRequestId, dataId);
        if (paymentId != null) {
            Long dueno=sellerId;
            if(dueno==null && body!=null && body.get("user_id")!=null) dueno=cuentasMp.duenoPorCuenta(String.valueOf(body.get("user_id")));
            pagoService.procesarWebhook(paymentId,dueno);
        }
        return ResponseEntity.ok().build();
    }

    @GetMapping("/retorno/{resultado}")
    public ResponseEntity<Void> retorno(
            @PathVariable String resultado,
            @RequestParam(required = false, name = "payment_id") String paymentId,
            @RequestParam(required = false, name = "preference_id") String preferenceId
    ) {
        URI destino = pagoService.construirRetornoFrontend(resultado, paymentId, preferenceId);
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .location(destino)
                .build();
    }

    private String extraerPaymentId(Map<String, Object> body) {
        if (body == null) return null;
        Object data = body.get("data");
        if (data instanceof Map<?, ?> dataMap && dataMap.get("id") != null) {
            return String.valueOf(dataMap.get("id"));
        }
        return null;
    }
}
