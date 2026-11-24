import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes, withComponentInputBinding()),
    // La protection CSRF (cookie XSRF-TOKEN / en-tête X-XSRF-TOKEN) est gérée par défaut par HttpClient.
    provideHttpClient(),
  ],
};
