import { Component, ChangeDetectionStrategy } from '@angular/core';

import { FormsModule } from '@angular/forms';
import { Router, RouterLink, ActivatedRoute } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
    selector: 'app-login',
    imports: [FormsModule, RouterLink],
    templateUrl: './login.component.html',
    changeDetection: ChangeDetectionStrategy.Eager,
    styleUrl: './login.component.css'
})
export class LoginComponent {
  mostrarClave=false;
  aviso='';
  email = '';
  password = '';
  error = '';
  cargando = false;

  constructor(private authService: AuthService, private router: Router,private route:ActivatedRoute) {if(route.snapshot.queryParamMap.get('sesion')==='vencida')this.aviso='Tu sesión venció. Ingresá nuevamente para continuar.';}

  onSubmit(): void {
    if(this.cargando)return;
    this.error = '';
    this.cargando = true;
    this.authService.login(this.email, this.password).subscribe({
      next: () => {
        this.cargando = false;
        const volver=this.route.snapshot.queryParamMap.get('volver');
        void this.router.navigateByUrl(volver?.startsWith('/')&&!volver.startsWith('//')?volver:'/inicio');
      },
      error: (err) => {
        this.cargando = false;
        this.error = err?.error?.message || 'No se pudo iniciar sesión';
      },
    });
  }
}
