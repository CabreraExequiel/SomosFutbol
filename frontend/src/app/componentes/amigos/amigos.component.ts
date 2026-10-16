import { Component, OnInit, OnDestroy, ChangeDetectionStrategy } from '@angular/core';

import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { forkJoin, Subscription } from 'rxjs';
import { Amistad, AmistadService, UsuarioSocial } from '../../services/amistad.service';
import { ChatService } from '../../services/chat.service';

type SeccionAmigos = 'amigos' | 'solicitudes' | 'buscar';

@Component({
    selector: 'app-amigos',
    imports: [FormsModule],
    templateUrl: './amigos.component.html',
    changeDetection: ChangeDetectionStrategy.Eager,
    styleUrl: './amigos.component.css'
})
export class AmigosComponent implements OnInit,OnDestroy {
  private busquedaSub?:Subscription;
  ngOnDestroy():void{this.busquedaSub?.unsubscribe();}
  seccion: SeccionAmigos = 'amigos';
  amigos: Amistad[] = [];
  recibidas: Amistad[] = [];
  enviadas: Amistad[] = [];
  resultados: UsuarioSocial[] = [];
  busqueda = '';
  cargando = true;
  buscando = false;
  procesandoId: number | null = null;
  error = '';
  mensaje = '';

  constructor(
    private amistadService: AmistadService,
    private chatService: ChatService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {
    const seccion = this.route.snapshot.queryParamMap.get('seccion');
    if (seccion === 'solicitudes' || seccion === 'buscar') this.seccion = seccion;
    this.cargarTodo();
  }

  cambiarSeccion(seccion: SeccionAmigos): void {
    this.seccion = seccion;
    this.mensaje = '';
    this.error = '';
    if (seccion === 'buscar' && this.resultados.length === 0) this.buscar();
  }

  cargarTodo(): void {
    this.cargando = true;
    this.error = '';
    forkJoin({
      amigos: this.amistadService.listarAmigos(),
      recibidas: this.amistadService.solicitudesRecibidas(),
      enviadas: this.amistadService.solicitudesEnviadas(),
    }).subscribe({
      next: ({ amigos, recibidas, enviadas }) => {
        this.amigos = amigos;
        this.recibidas = recibidas;
        this.enviadas = enviadas;
        this.cargando = false;
      },
      error: () => {
        this.error = 'No pudimos cargar tu comunidad. Revisá la conexión e intentá nuevamente.';
        this.cargando = false;
      },
    });
  }

  buscar(): void {
    this.buscando = true;
    this.error = '';
    this.busquedaSub?.unsubscribe();
    this.busquedaSub=this.amistadService.buscarUsuarios(this.busqueda).subscribe({
      next: (usuarios) => {
        this.resultados = usuarios;
        this.buscando = false;
      },
      error: () => {
        this.error = 'No se pudo realizar la búsqueda.';
        this.buscando = false;
      },
    });
  }

  enviarSolicitud(usuario: UsuarioSocial): void {
    this.procesandoId = usuario.id;
    this.limpiarMensajes();
    this.amistadService.enviarSolicitud(usuario.id).subscribe({
      next: (solicitud) => {
        usuario.relacion = 'PENDIENTE_ENVIADA';
        usuario.solicitudId = solicitud.id;
        this.enviadas = [solicitud, ...this.enviadas];
        this.procesandoId = null;
        this.mensaje = `Solicitud enviada a ${usuario.name}.`;
      },
      error: (err) => this.manejarError(err, 'No se pudo enviar la solicitud.'),
    });
  }

  aceptar(solicitud: Amistad): void {
    this.procesandoId = solicitud.id;
    this.limpiarMensajes();
    this.amistadService.aceptar(solicitud.id).subscribe({
      next: (amistad) => {
        this.recibidas = this.recibidas.filter((item) => item.id !== solicitud.id);
        this.amigos = [amistad, ...this.amigos];
        this.resultados=this.resultados.map(u=>u.id===amistad.userId?{...u,relacion:'AMIGOS'}:u);
        this.procesandoId = null;
        this.mensaje = `${amistad.nombre} ahora forma parte de tus amigos.`;
      },
      error: (err) => this.manejarError(err, 'No se pudo aceptar la solicitud.'),
    });
  }

  aceptarUsuario(usuario: UsuarioSocial): void {
    if (!usuario.solicitudId) return;
    const solicitud = this.recibidas.find((item) => item.id === usuario.solicitudId);
    if (solicitud) this.aceptar(solicitud);
  }

  rechazar(solicitud: Amistad): void {
    this.procesandoId = solicitud.id;
    this.limpiarMensajes();
    this.amistadService.rechazar(solicitud.id).subscribe({
      next: () => {
        this.recibidas = this.recibidas.filter((item) => item.id !== solicitud.id);
        this.resultados=this.resultados.map(u=>u.id===solicitud.userId?{...u,relacion:'NINGUNA',solicitudId:undefined}:u);
        this.procesandoId = null;
      },
      error: (err) => this.manejarError(err, 'No se pudo rechazar la solicitud.'),
    });
  }

  eliminar(amigo: Amistad): void {
    if (!window.confirm(`¿Querés eliminar a ${amigo.nombre} de tus amigos?`)) return;
    this.procesandoId = amigo.userId;
    this.limpiarMensajes();
    this.amistadService.eliminarAmigo(amigo.userId).subscribe({
      next: () => {
        this.amigos = this.amigos.filter((item) => item.userId !== amigo.userId);
        this.procesandoId = null;
      },
      error: (err) => this.manejarError(err, 'No se pudo eliminar la amistad.'),
    });
  }

  enviarMensaje(userId: number): void {
    this.procesandoId = userId;
    this.limpiarMensajes();
    this.chatService.iniciarConversacion(userId).subscribe({
      next: (conversacion) => {
        this.procesandoId = null;
        void this.router.navigate(['/chat'], { queryParams: { conversacion: conversacion.id } });
      },
      error: (err) => this.manejarError(err, 'No se pudo abrir la conversación.'),
    });
  }

  iniciales(nombre: string): string {
    return nombre
      .trim()
      .split(/\s+/)
      .slice(0, 2)
      .map((parte) => parte.charAt(0).toUpperCase())
      .join('');
  }

  private manejarError(err: any, fallback: string): void {
    this.procesandoId = null;
    this.error = err?.error?.message || fallback;
  }

  private limpiarMensajes(): void {
    this.error = '';
    this.mensaje = '';
  }
}
