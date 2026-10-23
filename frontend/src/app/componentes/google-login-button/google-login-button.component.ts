import { AfterViewInit, Component, ElementRef, EventEmitter, NgZone, OnDestroy, Output, ViewChild } from '@angular/core';
import { GOOGLE_WEB_CLIENT_ID } from '../../config/google-auth.config';

type GoogleCredential = { credential: string };
type GoogleLibrary = {
  accounts: { id: {
    initialize(options: { client_id: string; callback: (response: GoogleCredential) => void; auto_select?: boolean }): void;
    renderButton(element: HTMLElement, options: Record<string, string | number>): void;
  } };
};

declare global { interface Window { google?: GoogleLibrary } }

let scriptPromise: Promise<void> | undefined;
function cargarGoogle(): Promise<void> {
  if (window.google?.accounts?.id) return Promise.resolve();
  if (!scriptPromise) {
    scriptPromise = new Promise<void>((resolve, reject) => {
      const script = document.createElement('script');
      script.src = 'https://accounts.google.com/gsi/client';
      script.async = true;
      script.defer = true;
      script.onload = () => window.google?.accounts?.id ? resolve() : reject(new Error('SDK no disponible'));
      script.onerror = () => reject(new Error('No se pudo cargar Google Identity Services'));
      document.head.appendChild(script);
    }).catch(err => { scriptPromise = undefined; throw err; });
  }
  return scriptPromise;
}

@Component({
  selector: 'app-google-login-button',
  standalone: true,
  template: `<div #botonGoogle class="google-button" aria-label="Continuar con Google"></div>`,
  styles: [`.google-button { display:flex; justify-content:center; margin:16px auto; min-height:42px; }`]
})
export class GoogleLoginButtonComponent implements AfterViewInit, OnDestroy {
  @Output() credentialReceived = new EventEmitter<string>();
  @Output() loadError = new EventEmitter<string>();
  @ViewChild('botonGoogle', { static: true }) boton!: ElementRef<HTMLDivElement>;
  private activo = true;

  constructor(private zona: NgZone) {}

  ngAfterViewInit(): void {
    void cargarGoogle().then(() => {
      if (!this.activo || !window.google) return;
      window.google.accounts.id.initialize({
        client_id: GOOGLE_WEB_CLIENT_ID,
        auto_select: false,
        callback: (respuesta) => {
          if (this.activo && respuesta?.credential) this.zona.run(() => this.credentialReceived.emit(respuesta.credential));
        }
      });
      window.google.accounts.id.renderButton(this.boton.nativeElement, {
        type: 'standard', theme: 'outline', size: 'large', text: 'continue_with',
        shape: 'rectangular', width: 320, locale: 'es'
      });
    }).catch(() => {
      if (this.activo) this.zona.run(() => this.loadError.emit('No se pudo cargar el botón de Google. Revisá la conexión.'));
    });
  }

  ngOnDestroy(): void { this.activo = false; }
}
