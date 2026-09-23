import { HttpRequest } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';

import { SessionService } from '../auth/session.service';
import { ToastService } from '../feedback/toast/toast.service';
import { ApiError } from '../../shared/models/api-error';
import { GlobalHttpErrorHandler } from './global-http-error-handler';

const unauthorizedError: ApiError = {
  timestamp: '2026-09-15T12:00:00Z',
  status: 401,
  code: 'UNAUTHORIZED',
  message: 'Token inválido ou expirado.',
  path: '/api/v1/transactions',
  fieldErrors: [],
};

describe('GlobalHttpErrorHandler', () => {
  let handler: GlobalHttpErrorHandler;
  let session: { clear: ReturnType<typeof vi.fn> };
  let router: {
    url: string;
    navigate: ReturnType<typeof vi.fn>;
  };
  let toast: { show: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    session = { clear: vi.fn() };
    router = {
      url: '/transactions?month=9',
      navigate: vi.fn().mockResolvedValue(true),
    };
    toast = { show: vi.fn() };

    TestBed.configureTestingModule({
      providers: [
        GlobalHttpErrorHandler,
        { provide: SessionService, useValue: session },
        { provide: Router, useValue: router },
        { provide: ToastService, useValue: toast },
      ],
    });

    handler = TestBed.inject(GlobalHttpErrorHandler);
  });

  it('should end the session and redirect after a 401 from a private endpoint', () => {
    handler.handle(unauthorizedError, new HttpRequest('GET', '/api/v1/transactions'));

    expect(session.clear).toHaveBeenCalledOnce();
    expect(router.navigate).toHaveBeenCalledWith(['/login'], {
      queryParams: {
        returnUrl: '/transactions?month=9',
      },
    });
    expect(toast.show).toHaveBeenCalledWith({
      tone: 'warning',
      title: 'Sessão encerrada',
      message: 'Entre novamente para continuar.',
    });
  });

  it('should not clear the session or redirect after an invalid login', () => {
    handler.handle(unauthorizedError, new HttpRequest('POST', '/api/v1/auth/login', null));

    expect(session.clear).not.toHaveBeenCalled();
    expect(router.navigate).not.toHaveBeenCalled();
    expect(toast.show).toHaveBeenCalledWith({
      tone: 'warning',
      title: 'Não foi possível entrar',
      message: unauthorizedError.message,
    });
  });
});
