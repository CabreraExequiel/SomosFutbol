import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ChatService {
  private apiUrl = `${environment.apiUrl}`;

  constructor(private http: HttpClient) {}

  listarConversaciones(): Observable<any> {
    return this.http.get(`${this.apiUrl}/chat/conversaciones`);
  }

  iniciarConversacion(userId: number): Observable<any> {
    return this.http.post(`${this.apiUrl}/chat/conversaciones`, { otroUserId: userId });
  }

  obtenerMensajes(conversacionId: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/chat/conversaciones/${conversacionId}/mensajes`);
  }

  enviarMensaje(conversacionId: number, contenido: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/chat/conversaciones/${conversacionId}/mensajes`, { contenido });
  }
}
