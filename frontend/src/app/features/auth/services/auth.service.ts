import { inject, Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { finalize, Observable, tap } from 'rxjs';

import { SessionService } from '../../../core/auth/session.service';
import { CsrfTokenService } from '../../../core/auth/csrf-token.service';
import { User } from '../../../shared/models/user.models';
import { AuthApiService } from '../data-access/auth-api.service';
import { AuthResponse, LoginRequest, RegisterRequest } from '../models/auth.models';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly authApi = inject(AuthApiService);
  private readonly session = inject(SessionService);
  private readonly csrfToken = inject(CsrfTokenService);
  private readonly router = inject(Router);

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.authApi.login(request).pipe(
      tap((response) => {
        this.session.start(response.expiresIn);
      }),
    );
  }

  register(request: RegisterRequest): Observable<User> {
    return this.authApi.register(request);
  }

  logout(): void {
    this.authApi
      .logout()
      .pipe(
        finalize(() => {
          this.csrfToken.clear();
          this.session.clear();
          void this.router.navigate(['/login']);
        }),
      )
      .subscribe({ error: () => undefined });
  }
}
