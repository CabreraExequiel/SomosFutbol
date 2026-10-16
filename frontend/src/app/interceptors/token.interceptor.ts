import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { environment } from '../../environments/environment';
export const tokenInterceptor: HttpInterceptorFn=(req,next)=>{
 const auth=inject(AuthService),router=inject(Router);
 const base=new URL(environment.apiUrl,window.location.origin);
 const url=new URL(req.url,window.location.origin);
 const ours=url.origin===base.origin&&(url.pathname===base.pathname||url.pathname.startsWith(base.pathname.replace(/\/$/,'')+'/'));
 const token=ours?auth.getToken():null;
 const request=token?req.clone({setHeaders:{Authorization:`Bearer ${token}`}}):req;
 return next(request).pipe(catchError(err=>{
  if(ours && err.status===401 && !url.pathname.includes('/auth/')){
   auth.logout();void router.navigate(['/login'],{queryParams:{sesion:'vencida',volver:router.url}});
  }
  return throwError(()=>err);
 }));
};
