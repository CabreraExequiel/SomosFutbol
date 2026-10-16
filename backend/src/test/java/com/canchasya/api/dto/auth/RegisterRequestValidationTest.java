package com.canchasya.api.dto.auth;

import com.canchasya.api.model.Role;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RegisterRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void configurarValidador() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void rechazaRegistroSinTelefono() {
        RegisterRequest request = registroValido(Role.USUARIO);
        request.setTelefono(" ");

        Set<ConstraintViolation<RegisterRequest>> errores = validator.validate(request);

        assertThat(errores)
                .extracting(error -> error.getPropertyPath().toString())
                .contains("telefono");
    }

    @Test
    void rechazaDuenoSinCuentaDeCobro() {
        RegisterRequest request = registroValido(Role.DUENO);

        Set<ConstraintViolation<RegisterRequest>> errores = validator.validate(request);

        assertThat(errores)
                .extracting(error -> error.getPropertyPath().toString())
                .contains("cuentaCobroInformada");
    }

    @Test
    void aceptaDuenoConAliasValido() {
        RegisterRequest request = registroValido(Role.DUENO);
        request.setAliasMercadoPago("cancha.centro.mp");

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void exigeAmbasAceptacionesLegales() {
        RegisterRequest request = registroValido(Role.USUARIO);
        request.setAceptaTerminos(false);
        request.setAceptaPrivacidad(false);

        assertThat(validator.validate(request))
                .extracting(error -> error.getPropertyPath().toString())
                .contains("aceptaTerminos", "aceptaPrivacidad");
    }

    private RegisterRequest registroValido(Role role) {
        RegisterRequest request = new RegisterRequest();
        request.setName("Persona de Prueba");
        request.setEmail("prueba@example.com");
        request.setPassword("clave-segura-123");
        request.setTelefono("+54 9 297 400-0000");
        request.setCiudad("Comodoro Rivadavia");
        request.setRole(role);
        request.setAceptaTerminos(true);
        request.setAceptaPrivacidad(true);
        return request;
    }
}
