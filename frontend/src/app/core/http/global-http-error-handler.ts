import { HttpRequest } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { ApiError } from '../../shared/models/api-error';
import { SessionService } from '../auth/session.service';
import { ToastService } from '../feedback/toast/toast.service';

const GENERIC_SERVER_MESSAGE = 'Não foi possível concluir a operação. Tente novamente mais tarde.';

@Injectable({ providedIn: 'root' })
export class GlobalHttpErrorHandler {
  private readonly router = inject(Router);
  private readonly session = inject(SessionService);
  private readonly toast = inject(ToastService);

  private redirectingToLogin = false;

  handle(error: ApiError, request: HttpRequest<unknown>): void {
    switch (error.status) {
      case 0:
        this.toast.show({
          tone: 'danger',
          title: 'Servidor indisponível',
          message: error.message,
        });
        return;

      case 400:
        this.toast.show({
          tone: 'warning',
          title: 'Verifique os dados',
          message: error.message,
        });
        return;

      case 401:
        this.handleUnauthorized(error, request);
        return;

      case 404:
        this.toast.show({
          tone: 'warning',
          title: 'Recurso não encontrado',
          message: error.message,
        });
        return;

      case 409:
        this.toast.show({
          tone: 'warning',
          title: 'Conflito detectado',
          message: error.message,
        });
        return;

      default:
        this.toast.show({
          tone: 'danger',
          title: 'Erro inesperado',
          message: GENERIC_SERVER_MESSAGE,
        });
    }
  }

  private handleUnauthorized(error: ApiError, request: HttpRequest<unknown>): void {
    if (this.isAuthenticationRequest(request.url)) {
      this.toast.show({
        tone: 'warning',
        title: 'Não foi possível entrar',
        message: error.message,
      });
      return;
    }

    if (this.redirectingToLogin) {
      return;
    }

    this.redirectingToLogin = true;
    this.session.clear();

    this.toast.show({
      tone: 'warning',
      title: 'Sessão encerrada',
      message: 'Entre novamente para continuar.',
    });

    const returnUrl = this.router.url.startsWith('/') ? this.router.url : '/dashboard';

    void this.router
      .navigate(['/login'], {
        queryParams: { returnUrl },
      })
      .finally(() => {
        this.redirectingToLogin = false;
      });
  }

  private isAuthenticationRequest(url: string): boolean {
    return /\/auth\/(login|register)(?:\?|$)/.test(url);
  }
}
