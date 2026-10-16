import { Component, ChangeDetectionStrategy } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
    selector: 'app-landing',
    imports: [RouterLink],
    templateUrl: './landing.component.html',
    changeDetection: ChangeDetectionStrategy.Eager,
    styleUrl: './landing.component.css'
})
export class LandingComponent {}
