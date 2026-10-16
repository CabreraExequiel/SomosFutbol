import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';
export interface EstadoMp {habilitado:boolean;vinculada:boolean;cuenta:string|null;pruebas:boolean;}
@Injectable({providedIn:'root'})
export class MercadoPagoCuentaService {
 constructor(private http:HttpClient){}
 estado(){return this.http.get<EstadoMp>(`${environment.apiUrl}/mercadopago/estado`);}
 vincular(){return this.http.post<{url:string}>(`${environment.apiUrl}/mercadopago/vincular`,{});}
}
