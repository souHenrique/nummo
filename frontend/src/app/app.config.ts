import { DialogModule } from '@angular/cdk/dialog';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import {
  ApplicationConfig,
  importProvidersFrom,
  provideBrowserGlobalErrorListeners,
} from '@angular/core';
import { provideRouter } from '@angular/router';
import { environment } from '../environments/environment';
import { API_BASE_URL } from './core/config/api-base-url';
import { routes } from './app.routes';
import { apiErrorInterceptor } from './core/interceptors/api-error.interceptor';
import { authInterceptor } from './core/interceptors/auth.interceptor';
import { csrfInterceptor } from './core/interceptors/csrf.interceptor';

export function createAppConfig(apiBaseUrl: string = environment.apiBaseUrl): ApplicationConfig {
  return {
    providers: [
      provideBrowserGlobalErrorListeners(),
      provideRouter(routes),
      provideHttpClient(withInterceptors([authInterceptor, csrfInterceptor, apiErrorInterceptor])),
      importProvidersFrom(DialogModule),
      {
        provide: API_BASE_URL,
        useValue: apiBaseUrl,
      },
    ],
  };
}
