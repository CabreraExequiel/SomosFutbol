import { AfterViewInit, Directive, ElementRef, Input, OnDestroy } from '@angular/core';
import { PublicidadesMetricasService } from '../services/publicidades-metricas.service';
import { UbicacionPublicidadPublica } from '../services/publicidades-publicas.service';

/** 10D: solo contar cuando al menos la mitad de la tarjeta es visible durante 1 segundo. */
@Directive({ selector: '[sfImpresionPublicidad]', standalone: true })
export class PublicidadImpresionDirective implements AfterViewInit, OnDestroy {
  @Input({ alias: 'sfImpresionPublicidad', required: true }) publicidadId!: number;
  @Input({ required: true }) ubicacionImpresion!: UbicacionPublicidadPublica;
  private observador?: IntersectionObserver;
  private espera?: ReturnType<typeof setTimeout>;
  private enviada = false;

  constructor(private readonly elemento: ElementRef<HTMLElement>,
              private readonly metricas: PublicidadesMetricasService) {}

  ngAfterViewInit(): void {
    if (typeof IntersectionObserver === 'undefined') return;
    this.observador = new IntersectionObserver(entradas => {
      const entrada = entradas[0];
      if (this.enviada) return;
      if (entrada?.isIntersecting && entrada.intersectionRatio >= 0.5) {
        if (!this.espera) this.espera = setTimeout(() => {
          this.espera = undefined;
          this.enviada = true;
          this.observador?.disconnect();
          // Medición auxiliar: un error de red nunca debe bloquear la publicidad.
          this.metricas.impresion(this.publicidadId, this.ubicacionImpresion).subscribe({ error: () => {} });
        }, 1000);
      } else {
        if (this.espera) clearTimeout(this.espera);
        this.espera = undefined;
      }
    }, { threshold: [0, 0.5, 1] });
    this.observador.observe(this.elemento.nativeElement);
  }

  ngOnDestroy(): void {
    if (this.espera) clearTimeout(this.espera);
    this.observador?.disconnect();
  }
}
