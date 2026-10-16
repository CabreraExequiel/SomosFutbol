import { Component, OnDestroy, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { Subscription, timer, exhaustMap, filter, catchError, EMPTY }  from 'rxjs';
import { ChatService } from '../../services/chat.service';
import { AuthService } from '../../services/auth.service';
import { Amistad, AmistadService } from '../../services/amistad.service';
import { NotificacionService } from '../../services/notificacion.service';

@Component({
    selector: 'app-chat',
    imports: [CommonModule, FormsModule],
    templateUrl: './chat.component.html',
    changeDetection: ChangeDetectionStrategy.Eager,
    styleUrl: './chat.component.css'
})
export class ChatComponent implements OnInit, OnDestroy {
  conversaciones: any[] = [];
  amigos: Amistad[] = [];
  conversacionActiva: any = null;
  mensajes: any[] = [];
  nuevoMensaje = '';
  filtroAmigos = '';
  usuarioActual: any;
  mostrarAmigos = false;
  cargandoConversaciones = true;
  cargandoMensajes = false;
  iniciandoConId: number | null = null;
  errorConversaciones = '';
  errorMensajes = '';
  errorEnvio = '';

  enviando=false;
  private cargaMensajes?:Subscription;
  private pollingSubscription?: Subscription;

  constructor(
    private chatService: ChatService,
    private authService: AuthService,
    private amistadService: AmistadService,
    private notificacionService: NotificacionService,
    private route: ActivatedRoute
  ) {
    this.usuarioActual = this.authService.getUsuario();
  }

  ngOnInit(): void {
    this.cargarAmigos();
    this.cargarConversaciones();
  }

  ngOnDestroy(): void {
    this.detenerPolling();
    this.cargaMensajes?.unsubscribe();
  }

  cargarConversaciones(): void {
    this.cargandoConversaciones = true;
    this.errorConversaciones = '';
    this.chatService.listarConversaciones().subscribe({
      next: (res: any) => {
        this.conversaciones = res.data ?? res;
        this.cargandoConversaciones = false;
        const conversacionId = Number(this.route.snapshot.queryParamMap.get('conversacion'));
        if (conversacionId) {
          const conversacion = this.conversaciones.find((item) => item.id === conversacionId);
          if (conversacion) this.abrirConversacion(conversacion);
        }
      },
      error: () => {
        this.errorConversaciones = 'Revisá tu conexión e intentá nuevamente.';
        this.cargandoConversaciones = false;
      },
    });
  }

  cargarAmigos(): void {
    this.amistadService.listarAmigos().subscribe({
      next: (amigos) => (this.amigos = amigos),
      error: () => (this.amigos = []),
    });
  }

  iniciarConversacion(amigo: Amistad): void {
    this.iniciandoConId = amigo.userId;
    this.errorConversaciones = '';
    this.chatService.iniciarConversacion(amigo.userId).subscribe({
      next: (conversacion: any) => {
        const indice = this.conversaciones.findIndex((item) => item.id === conversacion.id);
        if (indice >= 0) this.conversaciones[indice] = conversacion;
        else this.conversaciones.unshift(conversacion);
        this.iniciandoConId = null;
        this.mostrarAmigos = false;
        this.abrirConversacion(conversacion);
      },
      error: (err) => {
        this.iniciandoConId = null;
        this.errorConversaciones = err?.error?.message || 'No pudimos iniciar la conversación.';
      },
    });
  }

  abrirConversacion(conversacion: any): void {
    this.cargaMensajes?.unsubscribe();
    this.mensajes=[];
    this.nuevoMensaje='';
    this.conversacionActiva = conversacion;
    this.conversacionActiva.noLeidos = 0;
    this.cargandoMensajes = true;
    this.errorMensajes = '';
    this.detenerPolling();
    this.cargaMensajes=this.chatService.obtenerMensajes(conversacion.id).subscribe({
      next: (res: any) => {
        this.mensajes = res.data ?? res;
        this.cargandoMensajes = false;
        this.notificacionService.actualizarConteo().subscribe({ error: () => undefined });
        this.iniciarPolling();
      },
      error: () => {
        this.errorMensajes = 'No pudimos cargar esta conversación.';
        this.cargandoMensajes = false;
      },
    });
  }

  enviar(): void {
    const contenido = this.nuevoMensaje.trim();
    if (this.enviando || !contenido || !this.conversacionActiva) return;
    const id=this.conversacionActiva.id;
    this.enviando=true;

    this.errorEnvio = '';
    this.chatService.enviarMensaje(this.conversacionActiva.id, contenido).subscribe({
      next: (mensaje: any) => {
        this.enviando=false;
        if(this.conversacionActiva?.id!==id)return;
        this.mensajes=[...this.mensajes.filter(m=>m.id!==mensaje.id),mensaje];
        this.nuevoMensaje = '';
        this.conversacionActiva.ultimoMensaje = mensaje.contenido;
        this.conversacionActiva.ultimoMensajeAt = mensaje.createdAt;
        this.conversacionActiva.updatedAt = mensaje.createdAt;
      },
      error: (err) => {
        this.enviando=false;
        this.errorEnvio = err?.error?.message || 'No se pudo enviar el mensaje.';
      },
    });
  }

  cerrarConversacion(): void {
    this.cargaMensajes?.unsubscribe();
    this.detenerPolling();
    this.conversacionActiva = null;
    this.mensajes = [];
    this.errorMensajes = '';
    this.errorEnvio = '';
  }

  otroUsuario(conversacion: any): any {
    return {
      id: conversacion.otroUserId,
      name: conversacion.otroUserNombre,
      avatar: conversacion.otroUserAvatar,
    };
  }

  iniciales(nombre?: string): string {
    if (!nombre) return 'J';
    return nombre.trim().split(/\s+/).slice(0, 2).map((parte) => parte.charAt(0).toUpperCase()).join('');
  }

  esMio(mensaje: any): boolean {
    return mensaje.userId === this.usuarioActual?.id;
  }

  get amigosFiltrados(): Amistad[] {
    const filtro = this.filtroAmigos.trim().toLocaleLowerCase();
    if (!filtro) return this.amigos;
    return this.amigos.filter((amigo) => amigo.nombre.toLocaleLowerCase().includes(filtro));
  }

  private iniciarPolling(): void {
    const id=this.conversacionActiva.id;
    this.pollingSubscription=timer(5000,5000).pipe(
      filter(()=>!document.hidden&&!this.enviando),
      exhaustMap(()=>this.chatService.obtenerMensajes(id).pipe(catchError(()=>EMPTY)))
    ).subscribe((res:any)=>{if(this.conversacionActiva?.id===id&&!this.enviando)this.mensajes=res.data??res;});
  }

  private detenerPolling(): void {
    this.pollingSubscription?.unsubscribe();
    this.pollingSubscription = undefined;
  }
}
