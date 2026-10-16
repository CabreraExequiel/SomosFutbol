import { Component, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink, ActivatedRoute } from '@angular/router';
import { AuthService, CuentaCobro, Usuario } from '../../services/auth.service';
import { CanchaService } from '../../services/cancha.service';
import { Amistad, AmistadService } from '../../services/amistad.service';

import { MercadoPagoCuentaService, EstadoMp } from '../../services/mercadopago-cuenta.service';

interface PerfilForm {
  name: string;
  telefono: string;
  ciudad: string;
}

interface CuentaCobroForm {
  cbuCvu: string;
  aliasMercadoPago: string;
}

@Component({
    selector: 'app-perfil',
    imports: [CommonModule, FormsModule, RouterLink],
    templateUrl: './perfil.component.html',
    changeDetection: ChangeDetectionStrategy.Eager,
    styleUrl: './perfil.component.css'
})
export class PerfilComponent implements OnInit {
  estadoMp:EstadoMp|null=null;
  errorMp='';avisoMp='';vinculando=false;
  cargarMp():void{this.mp.estado().subscribe({next:r=>this.estadoMp=r,error:()=>this.errorMp='No se pudo consultar la vinculación.'});}
  vincularMp():void{if(this.vinculando)return;this.vinculando=true;this.errorMp='';this.mp.vincular().subscribe({next:r=>{const url=new URL(r.url);if(url.origin!=='https://auth.mercadopago.com'){this.errorMp='Enlace de autorización inválido.';this.vinculando=false;return;}window.location.assign(r.url);},error:e=>{this.vinculando=false;this.errorMp=e.error?.message||'No se pudo iniciar la vinculación.';}});}
  usuario: Usuario | null = null;
  ciudades: any[] = [];
  amigos: Amistad[] = [];
  formulario: PerfilForm = this.formularioVacio();
  formularioOriginal: PerfilForm = this.formularioVacio();
  cuentaCobro: CuentaCobroForm = this.cuentaCobroVacia();
  cuentaCobroOriginal: CuentaCobroForm = this.cuentaCobroVacia();

  cargando = true;
  guardando = false;
  cargandoCuentaCobro = false;
  guardandoCuentaCobro = false;
  cuentaCobroSolicitada = false;
  subiendoAvatar = false;
  avatarSeleccionado: File | null = null;
  avatarPreview = '';
  avatarError = false;
  avisoCarga = '';
  errorCarga = '';
  errorGuardado = '';
  mensajeExito = '';
  errorCuentaCobro = '';
  mensajeCuentaCobro = '';

  constructor(
    private authService: AuthService,
    private canchaService: CanchaService,
    private amistadService: AmistadService,
    private mp:MercadoPagoCuentaService,
    private route:ActivatedRoute
  ) {}

  ngOnInit(): void {
    const retorno=this.route.snapshot.queryParamMap.get('mercadopago');
    if(retorno==='vinculado')this.avisoMp='Tu cuenta de Mercado Pago quedó vinculada.';
    if(retorno==='error')this.errorMp='No se completó la vinculación. Volvé a intentarlo desde este navegador.';
    const usuarioGuardado = this.authService.getUsuario();
    if (usuarioGuardado) this.aplicarUsuario(usuarioGuardado);

    this.authService.obtenerPerfil().subscribe({
      next: (usuario) => {
        this.aplicarUsuario(usuario);
        this.cargando = false;
      },
      error: () => {
        this.cargando = false;
        if (usuarioGuardado) {
          this.avisoCarga = 'Mostramos los datos guardados en este dispositivo. No pudimos sincronizarlos con el servidor.';
        } else {
          this.errorCarga = 'No pudimos cargar los datos de tu perfil.';
        }
      },
    });

    this.canchaService.listarCiudades().subscribe({
      next: (res: any) => (this.ciudades = res.data ?? res),
      error: () => (this.ciudades = []),
    });

    this.amistadService.listarAmigos().subscribe({
      next: (amigos) => (this.amigos = amigos),
      error: () => (this.amigos = []),
    });
  }

  seleccionarAvatar(event: Event): void {
    this.errorGuardado = '';
    this.mensajeExito = '';
    const input = event.target as HTMLInputElement;
    const archivo = input.files?.[0];
    if (!archivo) return;

    if (!['image/jpeg', 'image/png'].includes(archivo.type)) {
      this.errorGuardado = 'Elegí una imagen JPG o PNG.';
      input.value = '';
      return;
    }
    if (archivo.size > 5 * 1024 * 1024) {
      this.errorGuardado = 'La imagen no puede superar los 5 MB.';
      input.value = '';
      return;
    }

    this.avatarSeleccionado = archivo;
    const reader = new FileReader();
    reader.onload = () => (this.avatarPreview = String(reader.result || ''));
    reader.readAsDataURL(archivo);
  }

  subirAvatar(): void {
    if (!this.avatarSeleccionado) return;
    this.subiendoAvatar = true;
    this.errorGuardado = '';
    this.mensajeExito = '';

    this.authService.subirAvatar(this.avatarSeleccionado).subscribe({
      next: (usuario) => {
        this.aplicarUsuario(usuario);
        this.avatarSeleccionado = null;
        this.avatarPreview = '';
        this.subiendoAvatar = false;
        this.mensajeExito = 'Tu foto de perfil se actualizó correctamente.';
      },
      error: (err) => {
        this.subiendoAvatar = false;
        this.errorGuardado = err?.error?.message || 'No se pudo subir la foto de perfil.';
      },
    });
  }

  guardar(): void {
    if(this.guardando)return;
    this.errorGuardado = '';
    this.mensajeExito = '';

    const datos: PerfilForm = {
      name: this.formulario.name.trim(),
      telefono: this.formulario.telefono.trim(),
      ciudad: this.formulario.ciudad.trim(),
    };

    if (datos.name.length < 2) {
      this.errorGuardado = 'Ingresá un nombre de al menos 2 caracteres.';
      return;
    }
    if (!datos.telefono) {
      this.errorGuardado = 'Ingresá un número de teléfono.';
      return;
    }
    if (!/^[0-9+()\-\s]{7,30}$/.test(datos.telefono)) {
      this.errorGuardado = 'Ingresá un número de teléfono válido.';
      return;
    }

    this.guardando = true;
    this.authService.actualizarPerfil(datos).subscribe({
      next: (usuario) => {
        this.aplicarUsuario(usuario);
        this.guardando = false;
        this.avisoCarga = '';
        this.mensajeExito = 'Tus datos se actualizaron correctamente.';
      },
      error: (err) => {
        this.guardando = false;
        const respuesta = err?.error;
        this.errorGuardado = respuesta?.message
          || (respuesta && typeof respuesta === 'object' ? Object.values(respuesta).join(' ') : '')
          || 'No se pudieron guardar los cambios.';
      },
    });
  }

  cancelarCambios(): void {
    this.formulario = { ...this.formularioOriginal };
    this.errorGuardado = '';
    this.mensajeExito = '';
  }

  guardarCuentaCobro(): void {
    if(this.guardandoCuentaCobro)return;
    this.errorCuentaCobro = '';
    this.mensajeCuentaCobro = '';

    const cbuCvu = this.cuentaCobro.cbuCvu.replace(/[\s-]/g, '');
    const aliasMercadoPago = this.cuentaCobro.aliasMercadoPago.trim();

    if (!cbuCvu && !aliasMercadoPago) {
      this.errorCuentaCobro = 'Informá un CBU/CVU o alias de Mercado Pago.';
      return;
    }
    if (cbuCvu && !/^\d{22}$/.test(cbuCvu)) {
      this.errorCuentaCobro = 'El CBU/CVU debe contener exactamente 22 dígitos.';
      return;
    }
    if (aliasMercadoPago && !/^[A-Za-z0-9.-]{6,20}$/.test(aliasMercadoPago)) {
      this.errorCuentaCobro = 'El alias debe tener entre 6 y 20 caracteres y usar solo letras, números, puntos o guiones.';
      return;
    }

    this.guardandoCuentaCobro = true;
    this.authService.actualizarCuentaCobro({ cbuCvu: cbuCvu || null, aliasMercadoPago: aliasMercadoPago || null }).subscribe({
      next: (cuenta) => {
        this.aplicarCuentaCobro(cuenta);
        this.guardandoCuentaCobro = false;
        this.mensajeCuentaCobro = 'La cuenta para transferencias se actualizó correctamente.';
      },
      error: (err) => {
        this.guardandoCuentaCobro = false;
        const respuesta = err?.error;
        this.errorCuentaCobro = respuesta?.message
          || (respuesta && typeof respuesta === 'object' ? Object.values(respuesta).join(' ') : '')
          || 'No se pudo actualizar la cuenta para transferencias.';
      },
    });
  }

  cancelarCuentaCobro(): void {
    this.cuentaCobro = { ...this.cuentaCobroOriginal };
    this.errorCuentaCobro = '';
    this.mensajeCuentaCobro = '';
  }

  reintentar(): void {
    this.errorCarga = '';
    this.cargando = true;
    this.ngOnInit();
  }

  ocultarAvatar(): void {
    this.avatarError = true;
  }

  get iniciales(): string {
    const partes = this.usuario?.name?.trim().split(/\s+/).filter(Boolean) || [];
    return partes.slice(0, 2).map((parte) => parte.charAt(0).toUpperCase()).join('') || 'J';
  }

  get etiquetaRol(): string {
    if (this.usuario?.role === 'dueno') return 'Dueño de cancha';
    if (this.usuario?.role === 'administrador') return 'Administrador';
    return 'Jugador';
  }

  get perfilCompleto(): number {
    if (!this.usuario) return 0;
    const valores = [this.usuario.name, this.usuario.email, this.usuario.telefono, this.usuario.ciudad, this.usuario.avatar];
    return valores.filter((valor) => !!valor?.trim()).length * 20;
  }

  get hayCambios(): boolean {
    return JSON.stringify(this.formulario) !== JSON.stringify(this.formularioOriginal);
  }

  get hayCambiosCuentaCobro(): boolean {
    return JSON.stringify(this.cuentaCobro) !== JSON.stringify(this.cuentaCobroOriginal);
  }

  private aplicarUsuario(usuario: Usuario): void {
    this.usuario = usuario;
    this.avatarError = false;
    this.formulario = {
      name: usuario.name || '',
      telefono: usuario.telefono || '',
      ciudad: usuario.ciudad || '',
    };
    this.formularioOriginal = { ...this.formulario };
    if (usuario.role === 'dueno' && !this.cuentaCobroSolicitada) {
      this.cargarCuentaCobro();
      this.cargarMp();
    }
  }

  private cargarCuentaCobro(): void {
    this.cuentaCobroSolicitada = true;
    this.cargandoCuentaCobro = true;
    this.authService.obtenerCuentaCobro().subscribe({
      next: (cuenta) => {
        this.aplicarCuentaCobro(cuenta);
        this.cargandoCuentaCobro = false;
      },
      error: () => {
        this.cargandoCuentaCobro = false;
        this.errorCuentaCobro = 'No pudimos cargar tu cuenta para transferencias.';
      },
    });
  }

  private aplicarCuentaCobro(cuenta: CuentaCobro): void {
    this.cuentaCobro = {
      cbuCvu: cuenta.cbuCvu || '',
      aliasMercadoPago: cuenta.aliasMercadoPago || '',
    };
    this.cuentaCobroOriginal = { ...this.cuentaCobro };
  }

  private formularioVacio(): PerfilForm {
    return { name: '', telefono: '', ciudad: '' };
  }

  private cuentaCobroVacia(): CuentaCobroForm {
    return { cbuCvu: '', aliasMercadoPago: '' };
  }
}
