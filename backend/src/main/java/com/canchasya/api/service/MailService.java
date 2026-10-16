package com.canchasya.api.service;

import com.canchasya.api.model.Reserva;
import com.canchasya.api.model.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final JavaMailSender mailSender;
    @org.springframework.beans.factory.annotation.Value("${app.mail.enabled:false}") private boolean enabled;
    @org.springframework.beans.factory.annotation.Value("${app.mail.from:}") private String from;

    public void enviarBienvenida(User user) {
        String html = """
                <h2>¡Bienvenido/a %s!</h2>
                <p>Tu cuenta en Somos Fútbol fue creada con éxito. Ya podés buscar canchas,
                reservar horarios y sumarte a la comunidad.</p>
                """.formatted(org.springframework.web.util.HtmlUtils.htmlEscape(user.getName()));

        enviar(user.getEmail(), "Bienvenido/a a Somos Fútbol", html);
    }

    public void enviarTicketReserva(Reserva reserva) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String html = """
                <h2>¡Reserva confirmada!</h2>
                <p>Hola %s, tu reserva fue confirmada:</p>
                <ul>
                  <li><b>Cancha:</b> %s</li>
                  <li><b>Fecha:</b> %s</li>
                  <li><b>Horario:</b> %s a %s</li>
                  <li><b>Monto:</b> $%s</li>
                </ul>
                <p>¡Nos vemos en la cancha!</p>
                """.formatted(
                org.springframework.web.util.HtmlUtils.htmlEscape(reserva.getUser().getName()),
                org.springframework.web.util.HtmlUtils.htmlEscape(reserva.getCancha().getNombre()),
                reserva.getFecha().format(fmt),
                reserva.getHoraInicio(),
                reserva.getHoraFin(),
                reserva.getMonto()
        );

        enviar(reserva.getUser().getEmail(), "Ticket de tu reserva - Somos Fútbol", html);
    }

    private void enviar(String destinatario, String asunto, String html) {
        if(!enabled)return;
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, "UTF-8");
            helper.setTo(destinatario);
            if(!from.isBlank())helper.setFrom(from);
            helper.setSubject(asunto);
            helper.setText(html, true);
            mailSender.send(mensaje);
        } catch (MessagingException | RuntimeException e) {
            // No interrumpimos el flujo principal (registro/pago) si falla el envío de mail
            log.warn("No se pudo enviar el correo: {}", e.getClass().getSimpleName());
        }
    }
}
