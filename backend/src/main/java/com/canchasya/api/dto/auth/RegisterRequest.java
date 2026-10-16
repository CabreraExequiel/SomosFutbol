
package com.canchasya.api.dto.auth;

import com.canchasya.api.model.Role;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 100,
            message = "El nombre debe tener entre 2 y 100 caracteres")
    private String name;

    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "Ingresá un correo electrónico válido")
    @Size(max = 150,
            message = "El correo electrónico no puede superar los 150 caracteres")
    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, max = 72,
            message = "La contraseña debe tener entre 8 y 72 caracteres")
    private String password;

    @NotBlank(message = "El teléfono es obligatorio")
    @Size(min = 7, max = 30,
            message = "El teléfono debe tener entre 7 y 30 caracteres")
    @Pattern(
            regexp = "^[0-9+()\\- ]+$",
            message = "El teléfono contiene caracteres inválidos"
    )
    private String telefono;

    @Size(max = 100,
            message = "La ciudad no puede superar los 100 caracteres")
    private String ciudad;

    private Role role = Role.USUARIO;

    // CBU/CVU OPCIONAL
    @Size(max = 30,
            message = "El CBU/CVU no puede superar los 30 caracteres")
    @Pattern(
            regexp = "^\\s*$|^(?:[0-9][ -]?){21}[0-9]$",
            message = "El CBU/CVU debe contener exactamente 22 dígitos"
    )
    private String cbuCvu;

    // ALIAS MERCADO PAGO OPCIONAL
    @Pattern(
            regexp = "^\\s*$|^[A-Za-z0-9.-]{6,20}$",
            message = "El alias debe tener entre 6 y 20 caracteres y usar solo letras, números, puntos o guiones"
    )
    private String aliasMercadoPago;

    // ACEPTACIONES LEGALES OBLIGATORIAS
    @AssertTrue(message = "Debés aceptar los Términos y Condiciones")
    private boolean aceptaTerminos;

    @AssertTrue(message = "Debés aceptar la Política de Privacidad")
    private boolean aceptaPrivacidad;

    // Se eliminó isCuentaCobroInformada().
    // Los dueños pueden registrarse sin CBU/CVU ni alias.
}
