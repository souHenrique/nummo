import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { API_BASE_URL } from '../config/api-base-url';
import { authInterceptor } from './auth.interceptor';
import { csrfInterceptor } from './csrf.interceptor';

describe('csrfInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor, csrfInterceptor])),
        provideHttpClientTesting(),
        { provide: API_BASE_URL, useValue: '/api/v1' },
      ],
    });

    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('adds the CSRF token to an API mutation', () => {
    http.post('/api/v1/accounts', { name: 'Los Pollos Hermanos' }).subscribe();

    const tokenRequest = httpMock.expectOne('/api/v1/auth/csrf');
    expect(tokenRequest.request.withCredentials).toBe(true);
    tokenRequest.flush({ token: 'csrf-token', headerName: 'X-XSRF-TOKEN' });

    const mutation = httpMock.expectOne('/api/v1/accounts');
    expect(mutation.request.withCredentials).toBe(true);
    expect(mutation.request.headers.get('X-XSRF-TOKEN')).toBe('csrf-token');
    mutation.flush({});
  });

  it('does not request or attach a token to safe API requests', () => {
    http.get('/api/v1/accounts').subscribe();

    const request = httpMock.expectOne('/api/v1/accounts');
    expect(request.request.headers.has('X-XSRF-TOKEN')).toBe(false);
    request.flush([]);
    httpMock.expectNone('/api/v1/auth/csrf');
  });

  it('does not attach a token to an external mutation', () => {
    http.post('https://example.com/accounts', {}).subscribe();

    const request = httpMock.expectOne('https://example.com/accounts');
    expect(request.request.withCredentials).toBe(false);
    expect(request.request.headers.has('X-XSRF-TOKEN')).toBe(false);
    request.flush({});
    httpMock.expectNone('/api/v1/auth/csrf');
  });
});
