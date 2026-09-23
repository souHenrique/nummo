import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { API_BASE_URL } from '../config/api-base-url';
import { CsrfTokenService } from './csrf-token.service';

describe('CsrfTokenService', () => {
  let service: CsrfTokenService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        CsrfTokenService,
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: API_BASE_URL, useValue: '/api/v1' },
      ],
    });

    service = TestBed.inject(CsrfTokenService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('shares one token request while it is in progress', () => {
    const received: string[] = [];

    service.getToken().subscribe((token) => received.push(token));
    service.getToken().subscribe((token) => received.push(token));

    const request = httpMock.expectOne('/api/v1/auth/csrf');
    request.flush({ token: 'csrf-token', headerName: 'X-XSRF-TOKEN' });

    expect(received).toEqual(['csrf-token', 'csrf-token']);
  });

  it('loads a fresh token for a later mutation', () => {
    const received: string[] = [];

    service.getToken().subscribe((token) => received.push(token));
    httpMock
      .expectOne('/api/v1/auth/csrf')
      .flush({ token: 'first-token', headerName: 'X-XSRF-TOKEN' });

    service.getToken().subscribe((token) => received.push(token));
    httpMock
      .expectOne('/api/v1/auth/csrf')
      .flush({ token: 'second-token', headerName: 'X-XSRF-TOKEN' });

    expect(received).toEqual(['first-token', 'second-token']);
  });

  it('allows a new token request after clearing a pending request', () => {
    service.getToken().subscribe();
    httpMock
      .expectOne('/api/v1/auth/csrf')
      .flush({ token: 'first-token', headerName: 'X-XSRF-TOKEN' });

    service.clear();
    service.getToken().subscribe();

    httpMock
      .expectOne('/api/v1/auth/csrf')
      .flush({ token: 'second-token', headerName: 'X-XSRF-TOKEN' });
  });

  it('rejects an invalid token response', () => {
    let receivedError: unknown;

    service.getToken().subscribe({ error: (error) => (receivedError = error) });
    httpMock.expectOne('/api/v1/auth/csrf').flush({ token: '', headerName: 'invalid' });

    expect(receivedError).toBeInstanceOf(Error);
  });
});
