package com.canchasya.api.service;

import com.canchasya.api.dto.CuentaCobroRequest;
import com.canchasya.api.dto.CuentaCobroResponse;
import com.canchasya.api.exception.ApiException;
import com.canchasya.api.model.Role;
import com.canchasya.api.model.User;
import com.canchasya.api.repository.UserRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CuentaCobroServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final CuentaCobroService service = new CuentaCobroService(userRepository);

    @Test
    void normalizaYGuardaLosDatosDelDueno() {
        User dueno = new User();
        dueno.setRole(Role.DUENO);
        CuentaCobroRequest request = new CuentaCobroRequest();
        request.setCbuCvu("1234 5678 9012 3456 7890 12");
        request.setAliasMercadoPago("  Mi.Cancha.MP  ");

        CuentaCobroResponse response = service.actualizar(request, dueno);

        assertThat(response.getCbuCvu()).isEqualTo("1234567890123456789012");
        assertThat(response.getAliasMercadoPago()).isEqualTo("mi.cancha.mp");
        assertThat(response.isConfigurada()).isTrue();
        verify(userRepository).save(dueno);
    }

    @Test
    void impideQueUnJugadorAccedaALaCuentaDeCobro() {
        User jugador = new User();
        jugador.setRole(Role.USUARIO);

        assertThatThrownBy(() -> service.obtener(jugador))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("solo está disponible para dueños");
    }

    @Test
    void rechazaUnCbuCvuConLongitudIncorrecta() {
        assertThatThrownBy(() -> service.normalizarYValidar("1234", null))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("22 dígitos");
    }
}
