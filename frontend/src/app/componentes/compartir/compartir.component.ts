import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

@Component({
  selector: 'app-compartir',
  standalone: true,
  imports: [RouterLink],
  template: `
    <main style="padding: 32px; text-align: center;">
      <h1>Compartir en Somos Fútbol</h1>

      @if (error) {
        <p role="alert">{{ error }}</p>
        <a routerLink="/inicio">Ir al feed</a>
      } @else {
        <p>Preparando tu publicación...</p>
      }
    </main>
  `
})
export class CompartirComponent implements OnInit {
  error = '';

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    const params = this.route.snapshot.queryParamMap;
    const texto = [params.get('url'), params.get('text')]
      .filter((valor): valor is string => !!valor)
      .join(' ');

    const videoUrl = this.buscarVideoYoutube(texto);

    if (!videoUrl) {
      this.error = 'No se recibió un enlace válido de YouTube.';
      return;
    }

    // El feed lo leerá al abrirse; si hay que iniciar sesión,
    // el dato permanece en esta pestaña mientras dure la sesión.
    sessionStorage.setItem('somosFutbolYoutubeCompartido', videoUrl);

    void this.router.navigate(['/inicio'], { replaceUrl: true });
  }

  private buscarVideoYoutube(texto: string): string | null {
    const enlaces = texto.match(/https:\/\/[^\s<>"']+/gi) ?? [];

    for (const enlace of enlaces) {
      try {
        const url = new URL(enlace.replace(/[),.!?]+$/, ''));

        if (
          url.protocol !== 'https:' ||
          url.port ||
          url.username ||
          url.password
        ) {
          continue;
        }

        const host = url.hostname.toLowerCase();
        let id: string | null = null;

        if (host === 'youtu.be') {
          id = /^\/([A-Za-z0-9_-]{11})\/?$/.exec(url.pathname)?.[1] ?? null;
        } else if (
          ['youtube.com', 'www.youtube.com', 'm.youtube.com', 'music.youtube.com']
            .includes(host)
        ) {
          if (url.pathname === '/watch') {
            id = url.searchParams.get('v');
          } else {
            id = /^\/(?:shorts|live)\/([A-Za-z0-9_-]{11})\/?$/
              .exec(url.pathname)?.[1] ?? null;
          }
        }

        if (id && /^[A-Za-z0-9_-]{11}$/.test(id)) {
          return `https://www.youtube.com/watch?v=${id}`;
        }
      } catch {
        // Omitir enlaces inválidos sin interrumpir la recepción.
      }
    }

    return null;
  }
}
