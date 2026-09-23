import { DialogModule } from '@angular/cdk/dialog';
import { registerLocaleData } from '@angular/common';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import {
  ApplicationConfig,
  importProvidersFrom,
  LOCALE_ID,
  provideBrowserGlobalErrorListeners,
} from '@angular/core';
import localePt from '@angular/common/locales/pt';
import { provideRouter } from '@angular/router';
import { environment } from '../environments/environment';
import { API_BASE_URL } from './core/config/api-base-url';
import { routes } from './app.routes';
import { apiErrorInterceptor } from './core/interceptors/api-error.interceptor';
import { authInterceptor } from './core/interceptors/auth.interceptor';
import { csrfInterceptor } from './core/interceptors/csrf.interceptor';

registerLocaleData(localePt);

export function createAppConfig(apiBaseUrl: string = environment.apiBaseUrl): ApplicationConfig {
  return {
    providers: [
      provideBrowserGlobalErrorListeners(),
      provideRouter(routes),
      provideHttpClient(withInterceptors([authInterceptor, csrfInterceptor, apiErrorInterceptor])),
      importProvidersFrom(DialogModule),
      {
        provide: LOCALE_ID,
        useValue: 'pt-BR',
      },
      {
        provide: API_BASE_URL,
        useValue: apiBaseUrl,
      },
    ],
  };
}
