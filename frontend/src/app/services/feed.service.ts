import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class FeedService {
  private apiUrl = `${environment.apiUrl}`;

  constructor(private http: HttpClient) {}

  listar(page=0, modo: 'para-vos' | 'recientes' | 'amigos' | 'cerca' = 'para-vos'): Observable<any> {
    return this.http.get(`${this.apiUrl}/publicaciones`, {params: {page, size: 10, modo}});
  }

  buscarHashtag(tag: string, page = 0): Observable<any> {
    return this.http.get(`${this.apiUrl}/publicaciones/hashtags`, {
      params: { tag, page, size: 10 }
    });
  }

  publicar(data: { contenido: string; imagen?: string; youtubeUrl?: string; mencionadosIds?: number[] }): Observable<any> {
    return this.http.post(`${this.apiUrl}/publicaciones`, data);
  }


  publicarVideo(
    contenido: string,
    video: File,
    mencionadosIds: number[] = []
  ): Observable<any> {
    const formData = new FormData();
    formData.append('contenido', contenido);
    formData.append('video', video, video.name);
    mencionadosIds.forEach(id => formData.append('mencionadosIds', String(id)));
    // Content-Type multipart/form-data lo define el navegador.
    return this.http.post(`${this.apiUrl}/publicaciones/videos`, formData);
  }

  eliminar(id: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/publicaciones/${id}`);
  }

  comentar(publicacionId: number, contenido: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/publicaciones/${publicacionId}/comentarios`, { contenido });
  }

  darLike(publicacionId: number): Observable<any> {
    return this.http.post(`${this.apiUrl}/publicaciones/${publicacionId}/like`, {});
  }
}
