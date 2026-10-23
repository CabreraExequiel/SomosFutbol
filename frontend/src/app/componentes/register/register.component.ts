
import { Component, ChangeDetectionStrategy } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink, ActivatedRoute } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { GoogleLoginButtonComponent } from '../google-login-button/google-login-button.component';

@Component({
  selector: 'app-register',
  imports: [FormsModule, RouterLink, GoogleLoginButtonComponent],
  templateUrl: './register.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './register.component.css'
})
export class RegisterComponent {

  name = '';
  email = '';
  password = '';
  telefono = '';
  ciudad = '';

  role: 'usuario' | 'dueno' = 'usuario';

  // Datos de cobro opcionales
  cbuCvu = '';
  aliasMercadoPago = '';

  aceptaTerminos = false;
  aceptaPrivacidad = false;

  error = '';
  cargando = false;

  constructor(
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute
  ) {
    if (this.route.snapshot.queryParamMap.get('rol') === 'dueno') {
      this.role = 'dueno';
    }
  }

  onGoogleCredential(credential: string): void {
    if (this.cargando) return;
    this.error = '';
    if (!this.telefono.trim()) {
      this.error = 'Para registrarte con Google, completá tu teléfono.';
      return;
    }
    if (!this.aceptaTerminos || !this.aceptaPrivacidad) {
      this.error = 'Aceptá Términos y Privacidad para registrarte con Google.';
      return;
    }
    this.cargando = true;
    this.authService.loginGoogle(credential, {
      telefono: this.telefono.trim(), ciudad: this.ciudad.trim(), role: this.role,
      aceptaTerminos: this.aceptaTerminos, aceptaPrivacidad: this.aceptaPrivacidad
    }).subscribe({
      next: () => {
        this.cargando = false;
        const role = this.authService.getUsuario()?.role;
        void this.router.navigateByUrl(role === 'dueno' ? '/mis-canchas' : '/inicio');
      },
      error: err => {
        this.cargando = false;
        this.error = err?.error?.message || 'No se pudo registrar con Google.';
      }
    });
  }

  onSubmit(): void {
    if (this.cargando) return;

    this.error = '';

    // Validar teléfono
    if (!this.telefono.trim()) {
      this.error = 'Ingresá un número de teléfono.';
      return;
    }

    // Ya no exigimos CBU/CVU ni alias para los dueños.

    // Validar términos y privacidad
    if (!this.aceptaTerminos || !this.aceptaPrivacidad) {
      this.error = 'Debés aceptar los Términos y la Política de Privacidad.';
      return;
    }

    this.cargando = true;

    this.authService.register({
      name: this.name.trim(),
      email: this.email.trim(),
      password: this.password,
      telefono: this.telefono.trim(),
      ciudad: this.ciudad.trim(),
      role: this.role,

      // Si el dueño no proporciona los datos,
      // enviamos undefined en lugar de cadenas vacías.
      cbuCvu: this.role === 'dueno'
        ? (this.cbuCvu.trim() || undefined)
        : undefined,

      aliasMercadoPago: this.role === 'dueno'
        ? (this.aliasMercadoPago.trim() || undefined)
        : undefined,

      aceptaTerminos: this.aceptaTerminos,
      aceptaPrivacidad: this.aceptaPrivacidad
    }).subscribe({
      next: () => {
        this.cargando = false;

        this.router.navigate([
          this.role === 'dueno' ? '/mis-canchas' : '/inicio'
        ]);
      },

      error: (err) => {
        this.cargando = false;

        const respuesta = err?.error;

        this.error = respuesta?.message
          || (respuesta && typeof respuesta === 'object'
            ? Object.values(respuesta).join(' ')
            : '')
          || 'No se pudo completar el registro';
      }
    });
  }
}
