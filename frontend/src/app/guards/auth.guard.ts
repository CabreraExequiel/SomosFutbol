import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
export const authGuard:CanActivateFn=(_route,state)=>inject(AuthService).estaLogueado()?true:inject(Router).createUrlTree(['/login'],{queryParams:{volver:state.url}});
