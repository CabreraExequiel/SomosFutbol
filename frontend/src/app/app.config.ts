import { ApplicationConfig, ErrorHandler, provideZoneChangeDetection } from '@angular/core';
import { FrontendErrorHandler } from './services/frontend-error-handler';
import { provideRouter, withInMemoryScrolling } from '@angular/router';
import { provideHttpClient, withInterceptors, withXhr } from '@angular/common/http';

import { registerLocaleData } from '@angular/common';
import localeEsAr from '@angular/common/locales/es-AR';
import { LOCALE_ID, isDevMode } from '@angular/core';
registerLocaleData(localeEsAr);
import { routes } from './app.routes';
import { tokenInterceptor } from './interceptors/token.interceptor';
import { provideServiceWorker } from '@angular/service-worker';

export const appConfig: ApplicationConfig = {
  providers: [
    { provide: ErrorHandler, useClass: FrontendErrorHandler },
    {provide:LOCALE_ID,useValue:'es-AR'},
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes,withInMemoryScrolling({scrollPositionRestoration:'enabled',anchorScrolling:'enabled'})),
    provideHttpClient(withXhr(), withInterceptors([tokenInterceptor])), provideServiceWorker('ngsw-worker.js', {
            enabled: !isDevMode(),
            registrationStrategy: 'registerWhenStable:30000'
          }),
  ],
};
