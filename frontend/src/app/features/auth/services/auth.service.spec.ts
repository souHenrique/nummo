import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of } from 'rxjs';

import { SessionService } from '../../../core/auth/session.service';
import { CsrfTokenService } from '../../../core/auth/csrf-token.service';
import { User } from '../../../shared/models/user.models';
import { AuthApiService } from '../data-access/auth-api.service';
import { AuthResponse, LoginRequest, RegisterRequest } from '../models/auth.models';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  let authApi: {
    login: ReturnType<typeof vi.fn>;
    register: ReturnType<typeof vi.fn>;
    logout: ReturnType<typeof vi.fn>;
  };
  let session: {
    start: ReturnType<typeof vi.fn>;
    clear: ReturnType<typeof vi.fn>;
  };
  let router: {
    navigate: ReturnType<typeof vi.fn>;
  };
  let csrfToken: {
    clear: ReturnType<typeof vi.fn>;
  };

  beforeEach(() => {
    authApi = {
      login: vi.fn(),
      register: vi.fn(),
      logout: vi.fn(),
    };
    session = {
      start: vi.fn(),
      clear: vi.fn(),
    };
    router = {
      navigate: vi.fn().mockResolvedValue(true),
    };
    csrfToken = {
      clear: vi.fn(),
    };

    TestBed.configureTestingModule({
      providers: [
        AuthService,
        {
          provide: AuthApiService,
          useValue: authApi,
        },
        {
          provide: SessionService,
          useValue: session,
        },
        {
          provide: Router,
          useValue: router,
        },
        {
          provide: CsrfTokenService,
          useValue: csrfToken,
        },
      ],
    });

    service = TestBed.inject(AuthService);
  });

  it('should record only the session expiration after a successful login', () => {
    const request: LoginRequest = {
      email: 'jesse.pinkman@example.com',
      password: 'SenhaSegura123',
    };
    const response: AuthResponse = {
      expiresIn: 3600,
    };

    authApi.login.mockReturnValue(of(response));

    service.login(request).subscribe((result) => {
      expect(result).toEqual(response);
    });

    expect(authApi.login).toHaveBeenCalledWith(request);
    expect(session.start).toHaveBeenCalledWith(response.expiresIn);
  });

  it('should register without creating a session', () => {
    const request: RegisterRequest = {
      name: 'Jesse Pinkman',
      email: 'jesse.pinkman@example.com',
      password: 'SenhaSegura123',
    };
    const response: User = {
      id: 'f02e76b3-8d53-42dd-b4c5-43fcda2d3d84',
      name: request.name,
      email: request.email,
      createdAt: '2026-09-15T12:00:00Z',
      updatedAt: '2026-09-15T12:00:00Z',
    };

    authApi.register.mockReturnValue(of(response));

    service.register(request).subscribe((result) => {
      expect(result).toEqual(response);
    });

    expect(authApi.register).toHaveBeenCalledWith(request);
    expect(session.start).not.toHaveBeenCalled();
  });

  it('should ask the backend to clear the HttpOnly cookie before navigating to login', () => {
    authApi.logout.mockReturnValue(of(void 0));

    service.logout();

    expect(authApi.logout).toHaveBeenCalledOnce();
    expect(csrfToken.clear).toHaveBeenCalledOnce();
    expect(session.clear).toHaveBeenCalledOnce();
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });
});
