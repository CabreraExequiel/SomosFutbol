import { ChangeDetectionStrategy, Component } from '@angular/core';
import { ContactoLegalComponent } from './contacto-legal.component';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-privacidad',
  imports: [RouterLink,ContactoLegalComponent],
  templateUrl: './privacidad.component.html',
  styleUrl: './legal-pages.css',
  changeDetection: ChangeDetectionStrategy.Eager,
})
export class PrivacidadComponent {}
