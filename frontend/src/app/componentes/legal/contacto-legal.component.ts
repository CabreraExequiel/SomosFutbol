import { Component, inject, ChangeDetectionStrategy } from '@angular/core';
import { AsyncPipe } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { catchError, of } from 'rxjs';
import { environment } from '../../../environments/environment';
@Component({selector:'app-contacto-legal',imports:[AsyncPipe],changeDetection:ChangeDetectionStrategy.Eager,template:`
 @if(datos$|async;as datos){
  @if(datos.operador){<dl><dt>Operador</dt><dd>{{datos.operador}}</dd><dt>CUIT</dt><dd>{{datos.cuit}}</dd><dt>Domicilio</dt><dd>{{datos.domicilio}}</dd><dt>Soporte y privacidad</dt><dd><a [href]="'mailto:'+datos.contacto">{{datos.contacto}}</a></dd></dl>}
  @else{<p>Los datos de contacto del operador no están configurados en este entorno.</p>}
 }`,styles:[`dl{padding:20px;background:#f8fafc;border-radius:12px}dt{font-weight:700;margin-top:12px}dt:first-child{margin-top:0}dd{margin:5px 0;color:#475569}a{color:#c2410c}`]})
export class ContactoLegalComponent {
 datos$=inject(HttpClient).get<any>(environment.apiUrl+'/config').pipe(catchError(()=>of({operador:''})));
}
