import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { switchMap } from 'rxjs';

import { CsrfTokenService } from '../auth/csrf-token.service';
import { API_BASE_URL } from '../config/api-base-url';

const UNSAFE_METHODS = new Set(['POST', 'PUT', 'PATCH', 'DELETE']);

export const csrfInterceptor: HttpInterceptorFn = (request, next) => {
  const apiBaseUrl = inject(API_BASE_URL);
  const isApiRequest = request.url === apiBaseUrl || request.url.startsWith(`${apiBaseUrl}/`);

  if (!isApiRequest || !UNSAFE_METHODS.has(request.method.toUpperCase())) {
    return next(request);
  }

  return inject(CsrfTokenService)
    .getToken()
    .pipe(
      switchMap((token) =>
        next(
          request.clone({
            setHeaders: { 'X-XSRF-TOKEN': token },
          }),
        ),
      ),
    );
};
