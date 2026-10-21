import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

// UX: impide navegar al panel sin rol. Spring Boot también valida cada solicitud.
export const adminGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (auth.estaLogueado() && auth.esAdmin()) return true;
  return router.createUrlTree(['/inicio']);
};
