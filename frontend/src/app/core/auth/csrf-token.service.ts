import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { finalize, map, Observable, of, shareReplay, tap } from 'rxjs';

import { ApiUrlService } from '../http/api-url.service';

interface CsrfTokenResponse {
  token: string;
  headerName: string;
}

@Injectable({ providedIn: 'root' })
export class CsrfTokenService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = inject(ApiUrlService);

  private token: string | null = null;
  private pendingRequest: Observable<string> | null = null;

  getToken(): Observable<string> {
    if (this.token) {
      return of(this.token);
    }

    if (this.pendingRequest) {
      return this.pendingRequest;
    }

    this.pendingRequest = this.http.get<CsrfTokenResponse>(this.apiUrl.build('auth/csrf')).pipe(
      map((response) => {
        const token = response.token?.trim();

        if (!token || response.headerName !== 'X-XSRF-TOKEN') {
          throw new Error('A API retornou um token CSRF inválido.');
        }

        return token;
      }),
      tap((token) => {
        this.token = token;
      }),
      finalize(() => {
        this.pendingRequest = null;
      }),
      shareReplay({ bufferSize: 1, refCount: false }),
    );

    return this.pendingRequest;
  }

  clear(): void {
    this.token = null;
    this.pendingRequest = null;
  }
}
