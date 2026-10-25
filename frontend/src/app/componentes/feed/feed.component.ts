import {
  Component,
  OnInit,
  OnDestroy,
  ChangeDetectionStrategy
} from '@angular/core';

import { CommonModule } from '@angular/common';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { environment } from '../../../environments/environment';
import { FormsModule } from '@angular/forms';

import { Subscription } from 'rxjs';

import { FeedService, HashtagDestacado } from '../../services/feed.service';
import { AuthService } from '../../services/auth.service';
import {
  Amistad,
  AmistadService
} from '../../services/amistad.service';

type ModoFeed = 'para-vos' | 'recientes' | 'amigos' | 'cerca';

@Component({
  selector: 'app-feed',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule
  ],
  templateUrl: './feed.component.html',
  styleUrls: [
    './feed.component.css',
    './feed.component.media.css',
    './feed.component.discovery.css'
  ],
  changeDetection: ChangeDetectionStrategy.Default
})
export class FeedComponent implements OnInit, OnDestroy {

  private solicitudFeed = 0;
  private suscripciones = new Subscription();

  private readonly TAMANIO_PAGINA = 10;

  modoFeed: ModoFeed = 'para-vos';
  textoHashtag = '';
  hashtagActivo: string | null = null;
  diasHashtag: 0 | 7 | 30 = 0;
  hashtagsDestacados: HashtagDestacado[] = [];

  publicaciones: any[] = [];

  pagina = 0;
  hayMas = false;
  cargandoMas = false;
  cargando = false;

  ocupados = new Set<number>();

  nuevoContenido = '';
  videoSeleccionado: File | null = null;
  videoPreviewUrl: string | null = null;
  youtubeUrl = '';
  mostrarCampoYoutube = false;
  private embedsYoutube = new Map<string, SafeResourceUrl>();
  comentarioTexto: Record<number, string> = {};

  usuarioActual: any;

  amigos: Amistad[] = [];
  mencionadosIds: number[] = [];
  mostrarEtiquetas = false;

  publicando = false;

  error = '';
  errorMas = '';
  errorBusquedaHashtag = '';
  errorPublicacion = '';

  constructor(
    private feedService: FeedService,
    private authService: AuthService,
    private amistadService: AmistadService,
    private sanitizer: DomSanitizer
  ) {
    this.usuarioActual = this.authService.getUsuario();
  }

  ngOnInit(): void {
    this.cargarFeed();
    this.cargarHashtagsDestacados();
    const youtubeCompartido = sessionStorage.getItem(
      'somosFutbolYoutubeCompartido'
    );

    if (youtubeCompartido) {
      this.quitarVideo();
      this.youtubeUrl = youtubeCompartido;
      this.mostrarCampoYoutube = true;

      sessionStorage.removeItem(
        'somosFutbolYoutubeCompartido'
      );
    }

    const sub = this.amistadService.listarAmigos().subscribe({
      next: amigos => {
        this.amigos = amigos;
      },
      error: () => {
        this.amigos = [];
      }
    });

    this.suscripciones.add(sub);
  }

  ngOnDestroy(): void {
    this.solicitudFeed++;
    this.suscripciones.unsubscribe();
    this.quitarVideo();
  }


seleccionarVideo(event: Event): void {

  const input = event.target as HTMLInputElement;
  const archivo = input.files?.[0];

  if (!archivo) return;

  this.errorPublicacion = '';

  if (archivo.type !== 'video/mp4') {
    this.errorPublicacion = 'Solo se permiten videos MP4.';
    input.value = '';
    return;
  }

  const maximo = 100 * 1024 * 1024;

  if (archivo.size > maximo) {
    this.errorPublicacion =
      'El video no puede superar los 100 MB.';
    input.value = '';
    return;
  }

  this.quitarVideo();

  this.youtubeUrl = '';
  this.mostrarCampoYoutube = false;
  this.videoSeleccionado = archivo;
  this.videoPreviewUrl = URL.createObjectURL(archivo);
  input.value = '';
}

quitarVideo(): void {

  if (this.videoPreviewUrl) {
    URL.revokeObjectURL(this.videoPreviewUrl);
  }

  this.videoSeleccionado = null;
  this.videoPreviewUrl = null;
}





  // Solo acepta identificadores seguros de YouTube: jamás URLs arbitrarias en iframes.
  private extraerIdYoutube(url: string): string | null {
    try {
      const u = new URL(url.trim());
      if (u.protocol !== 'https:' || u.port || u.username || u.password) return null;
      const host = u.hostname.toLowerCase();
      let id: string | null = null;
      if (host === 'youtu.be') {
        id = /^\/([A-Za-z0-9_-]{11})\/?$/.exec(u.pathname)?.[1] ?? null;
      } else if (['youtube.com', 'www.youtube.com', 'm.youtube.com',
                  'music.youtube.com', 'www.youtube-nocookie.com',
                  'youtube-nocookie.com'].includes(host)) {
        if (u.pathname === '/watch') {
          id = u.searchParams.get('v');
        } else {
          id = /^\/(?:shorts|live|embed)\/([A-Za-z0-9_-]{11})\/?$/.exec(u.pathname)?.[1] ?? null;
        }
      }
      return id && /^[A-Za-z0-9_-]{11}$/.test(id) ? id : null;
    } catch {
      return null;
    }
  }

  get youtubePreviewId(): string | null {
    return this.extraerIdYoutube(this.youtubeUrl);
  }

  activarYoutube(): void {
    this.mostrarCampoYoutube = !this.mostrarCampoYoutube;
    if (this.mostrarCampoYoutube) this.quitarVideo();
    else this.youtubeUrl = '';
    this.errorPublicacion = '';
  }

  quitarYoutube(): void {
    this.youtubeUrl = '';
    this.mostrarCampoYoutube = false;
  }

  youtubeEmbedUrl(id: string | null | undefined): SafeResourceUrl | null {
    if (!id || !/^[A-Za-z0-9_-]{11}$/.test(id)) return null;
    let url = this.embedsYoutube.get(id);
    if (!url) {
      url = this.sanitizer.bypassSecurityTrustResourceUrl(
        `https://www.youtube-nocookie.com/embed/${id}`
      );
      this.embedsYoutube.set(id, url);
    }
    return url;
  }

  // Resuelve URLs de medios del backend (también cuando Angular corre en otro puerto).
  urlMedia(url: string | null | undefined): string | null {
    if (!url) return null;
    if (/^https?:\/\//i.test(url)) return url;

    const api = new URL(environment.apiUrl, window.location.origin);
    return new URL(url.startsWith('/') ? url : '/' + url, api.origin).toString();
  }

  // ==========================================
  // FILTROS DEL FEED
  // ==========================================

  cambiarModo(modo: ModoFeed): void {

    if (this.modoFeed === modo && !this.hashtagActivo) {
      return;
    }

    this.modoFeed = modo;
    this.hashtagActivo = null;
    this.textoHashtag = '';
    this.diasHashtag = 0;
    this.errorBusquedaHashtag = '';
    this.cargarFeed();
  }

  cargarHashtagsDestacados(): void {
    const sub = this.feedService.listarHashtagsDestacados().subscribe({
      next: etiquetas => { this.hashtagsDestacados = etiquetas; },
      error: () => { this.hashtagsDestacados = []; }
    });
    this.suscripciones.add(sub);
  }

  seleccionarHashtagDestacado(etiqueta: string): void {
    this.textoHashtag = `#${etiqueta}`;
    this.buscarHashtag();
  }

  cambiarPeriodoHashtag(): void {
    if (this.hashtagActivo) this.cargarFeed();
  }

  buscarHashtag(): void {
    const valor = this.textoHashtag.trim().replace(/^#/, '').toLowerCase();
    if (!/^[A-Za-z0-9_]{2,30}$/.test(valor)) {
      this.errorBusquedaHashtag = 'Escribí un hashtag válido (2 a 30 caracteres, sin espacios).';
      return;
    }
    this.errorBusquedaHashtag = '';
    this.hashtagActivo = valor;
    this.cargarFeed();
  }

  limpiarHashtag(): void {
    this.errorBusquedaHashtag = '';
    this.hashtagActivo = null;
    this.textoHashtag = '';
    this.diasHashtag = 0;
    this.cargarFeed();
  }

  // ==========================================
  // NORMALIZAR RESPUESTA DEL BACKEND
  // ==========================================

  private obtenerPublicaciones(respuesta: any): any[] {

    if (Array.isArray(respuesta)) {
      return respuesta;
    }

    if (Array.isArray(respuesta?.data)) {
      return respuesta.data;
    }

    if (Array.isArray(respuesta?.content)) {
      return respuesta.content;
    }

    return [];
  }

  private agregarSinDuplicados(
    actuales: any[],
    nuevas: any[]
  ): any[] {

    const ids = new Set(
      actuales.map(publicacion => publicacion.id)
    );

    const resultado = [...actuales];

    for (const publicacion of nuevas) {

      if (!ids.has(publicacion.id)) {
        resultado.push(publicacion);
        ids.add(publicacion.id);
      }
    }

    return resultado;
  }

  // ==========================================
  // CARGAR FEED
  // ==========================================

  cargarFeed(): void {

    const solicitud = ++this.solicitudFeed;
    const modo = this.modoFeed;

    this.cargando = true;
    this.cargandoMas = false;

    this.error = '';
    this.errorMas = '';
    this.publicaciones = [];

    this.pagina = 0;
    this.hayMas = false;

    const consulta = this.hashtagActivo
      ? this.feedService.buscarHashtag(this.hashtagActivo, 0, this.diasHashtag)
      : this.feedService.listar(0, modo);
    const sub = consulta.subscribe({

      next: respuesta => {

        if (solicitud !== this.solicitudFeed) {
          return;
        }

        const publicaciones =
          this.obtenerPublicaciones(respuesta);

        this.publicaciones = publicaciones;

        this.pagina = 0;

        this.hayMas =
          publicaciones.length === this.TAMANIO_PAGINA;

        this.cargando = false;
      },

      error: error => {

        if (solicitud !== this.solicitudFeed) {
          return;
        }

        console.error(
          'Error al cargar el feed:',
          error
        );

        this.error =
          'No pudimos cargar las publicaciones. Intentá nuevamente.';

        this.cargando = false;
        this.hayMas = false;
      }
    });

    this.suscripciones.add(sub);
  }

  // ==========================================
  // CARGAR MÁS PUBLICACIONES
  // ==========================================

  cargarMas(): void {

    if (
      this.cargando ||
      this.cargandoMas ||
      !this.hayMas
    ) {
      return;
    }

    const solicitud = this.solicitudFeed;
    const modo = this.modoFeed;
    const tag = this.hashtagActivo;
    const dias = this.diasHashtag;
    const siguientePagina = this.pagina + 1;

    this.cargandoMas = true;
    this.errorMas = '';

    const consulta = tag
      ? this.feedService.buscarHashtag(tag, siguientePagina, dias)
      : this.feedService.listar(siguientePagina, modo);
    const sub = consulta.subscribe({

        next: respuesta => {

          if (
            solicitud !== this.solicitudFeed ||
            modo !== this.modoFeed ||
            tag !== this.hashtagActivo ||
            dias !== this.diasHashtag
          ) {
            return;
          }

          const nuevas =
            this.obtenerPublicaciones(respuesta);

          this.publicaciones =
            this.agregarSinDuplicados(
              this.publicaciones,
              nuevas
            );

          this.pagina = siguientePagina;

          this.hayMas =
            nuevas.length === this.TAMANIO_PAGINA;

          this.cargandoMas = false;
        },

        error: error => {

          if (solicitud !== this.solicitudFeed) {
            return;
          }

          console.error(
            'Error al cargar más publicaciones:',
            error
          );

          this.errorMas =
            'No se pudieron cargar más publicaciones. Reintentá sin perder los resultados anteriores.';

          this.cargandoMas = false;
        }
      });

    this.suscripciones.add(sub);
  }

  // ==========================================
  // PUBLICAR
  // ==========================================


publicar(): void {

  if (this.publicando || !this.nuevoContenido.trim()) {
    return;
  }

  const enlaceYoutube = this.youtubeUrl.trim();
  if (enlaceYoutube && !this.extraerIdYoutube(enlaceYoutube)) {
    this.errorPublicacion = 'Pegá un enlace válido de YouTube (watch, Shorts o youtu.be).';
    return;
  }
  if (this.videoSeleccionado && enlaceYoutube) {
    this.errorPublicacion = 'Elegí un video MP4 o un enlace de YouTube, no ambos.';
    return;
  }

  this.publicando = true;
  this.errorPublicacion = '';

  const contenido = this.nuevoContenido.trim();

  const peticion = this.videoSeleccionado
    ? this.feedService.publicarVideo(
        contenido,
        this.videoSeleccionado,
        this.mencionadosIds
      )
    : this.feedService.publicar({
        contenido,
        youtubeUrl: enlaceYoutube || undefined,
        mencionadosIds: this.mencionadosIds
      });

  const sub = peticion.subscribe({

    next: () => {

      this.nuevoContenido = '';
      this.mencionadosIds = [];
      this.mostrarEtiquetas = false;

      this.quitarVideo();
      this.quitarYoutube();

      this.publicando = false;

      this.cargarFeed();
      this.cargarHashtagsDestacados();
    },

    error: (error) => {

      console.error('Error al publicar:', error);

      this.publicando = false;

      this.errorPublicacion =
        error?.error?.message ||
        'No se pudo crear la publicación.';
    }
  });

  this.suscripciones.add(sub);
}


  // ==========================================
  // ETIQUETAR AMIGOS
  // ==========================================

  alternarEtiqueta(userId: number): void {

    this.mencionadosIds =
      this.mencionadosIds.includes(userId)
        ? this.mencionadosIds.filter(
            id => id !== userId
          )
        : [...this.mencionadosIds, userId];
  }

  estaEtiquetado(userId: number): boolean {
    return this.mencionadosIds.includes(userId);
  }

  // ==========================================
  // ME GUSTA
  // ==========================================

  darLike(pub: any): void {

    if (this.ocupados.has(pub.id)) {
      return;
    }

    this.ocupados.add(pub.id);
    this.error = '';

    const sub = this.feedService
      .darLike(pub.id)
      .subscribe({

        next: (respuesta: any) => {

          const activo =
            typeof respuesta === 'boolean'
              ? respuesta
              : respuesta?.likeado;

          if (typeof activo === 'boolean') {

            const anterior = !!pub.likeadoPorMi;

            if (activo !== anterior) {

              pub.cantLikes = Math.max(
                0,
                (pub.cantLikes || 0) +
                (activo ? 1 : -1)
              );
            }

            pub.likeadoPorMi = activo;
          }

          this.ocupados.delete(pub.id);
        },

        error: error => {

          console.error(
            'Error al dar Me gusta:',
            error
          );

          this.ocupados.delete(pub.id);

          this.error =
            'No se pudo actualizar el Me gusta.';
        }
      });

    this.suscripciones.add(sub);
  }

  // ==========================================
  // COMENTAR
  // ==========================================

  comentar(pub: any): void {

    const texto =
      this.comentarioTexto[pub.id]?.trim();

    if (
      !texto ||
      this.ocupados.has(pub.id)
    ) {
      return;
    }

    this.ocupados.add(pub.id);
    this.error = '';

    const sub = this.feedService
      .comentar(pub.id, texto)
      .subscribe({

        next: comentario => {

          pub.comentarios = [
            ...(pub.comentarios || []),
            comentario
          ];

          this.comentarioTexto[pub.id] = '';

          this.ocupados.delete(pub.id);
        },

        error: error => {

          console.error(
            'Error al comentar:',
            error
          );

          this.ocupados.delete(pub.id);

          this.error =
            error?.error?.message ||
            'No se pudo enviar el comentario.';
        }
      });

    this.suscripciones.add(sub);
  }

  // ==========================================
  // ELIMINAR PUBLICACIÓN
  // ==========================================

  eliminar(pub: any): void {

    if (this.ocupados.has(pub.id)) {
      return;
    }

    if (
      !window.confirm(
        '¿Querés eliminar esta publicación?'
      )
    ) {
      return;
    }

    this.ocupados.add(pub.id);
    this.error = '';

    const sub = this.feedService
      .eliminar(pub.id)
      .subscribe({

        next: () => {

          this.publicaciones =
            this.publicaciones.filter(
              publicacion =>
                publicacion.id !== pub.id
            );

          this.ocupados.delete(pub.id);
          this.cargarHashtagsDestacados();
        },

        error: error => {

          console.error(
            'Error al eliminar:',
            error
          );

          this.ocupados.delete(pub.id);

          this.error =
            'No se pudo eliminar la publicación.';
        }
      });

    this.suscripciones.add(sub);
  }

  // ==========================================
  // VERIFICAR PROPIETARIO
  // ==========================================

  esMia(publicacion: any): boolean {

    return (
      Number(this.usuarioActual?.id) ===
      Number(publicacion.userId)
    );
  }
}
