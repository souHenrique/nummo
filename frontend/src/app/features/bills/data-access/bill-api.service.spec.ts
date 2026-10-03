import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { API_BASE_URL } from '../../../core/config/api-base-url';
import { BillApiService } from './bill-api.service';

describe('BillApiService', () => {
  let service: BillApiService;
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: API_BASE_URL, useValue: '/api/v1' },
      ],
    });
    service = TestBed.inject(BillApiService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());

  it('preserves due month and status when requesting another page', () => {
    service.list(2026, 10, 'PENDING', 2).subscribe();
    const request = http.expectOne((req) => req.url === '/api/v1/bills');
    expect(request.request.params.get('year')).toBe('2026');
    expect(request.request.params.get('month')).toBe('10');
    expect(request.request.params.get('status')).toBe('PENDING');
    expect(request.request.params.get('page')).toBe('2');
    request.flush({ content: [], number: 2, size: 20, totalElements: 0, totalPages: 0 });
  });

  it('omits an empty status filter', () => {
    service.list(2026, 10).subscribe();
    const request = http.expectOne((req) => req.url === '/api/v1/bills');
    expect(request.request.params.has('status')).toBe(false);
    request.flush({ content: [] });
  });

  it('posts the per-installment amount and recurrence', () => {
    const payload = {
      description: 'Moto de Jesse',
      amount: 100.25,
      firstDueDate: '2026-10-10',
      installmentCount: 36,
      categoryId: 'transport',
    };
    service.create(payload).subscribe();
    const request = http.expectOne('/api/v1/bills');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(payload);
    request.flush([]);
  });

  it('patches only the selected bill with concurrency version', () => {
    const payload = {
      description: 'Moto de Walter',
      amount: 120,
      dueDate: '2026-10-15',
      categoryId: 'transport',
      expectedVersion: 2,
    };
    service.update('bill-walter', payload).subscribe();
    const request = http.expectOne('/api/v1/bills/bill-walter');
    expect(request.request.method).toBe('PATCH');
    expect(request.request.body).toEqual(payload);
    request.flush({});
  });

  it('posts payment account, date and version without retrying a conflict', () => {
    const error = vi.fn();
    const payload = {
      sourceAccountId: 'account-jesse',
      paymentDate: '2026-09-29',
      expectedVersion: 0,
    };
    service.pay('bill-jesse', payload).subscribe({ error });
    const request = http.expectOne('/api/v1/bills/bill-jesse/pay');
    expect(request.request.body).toEqual(payload);
    request.flush({}, { status: 409, statusText: 'Conflict' });
    expect(error).toHaveBeenCalledOnce();
    http.expectNone('/api/v1/bills/bill-jesse/pay');
  });

  it('cancels with the expected version instead of deleting the bill', () => {
    service.cancel('bill-jesse', 2).subscribe();
    const request = http.expectOne('/api/v1/bills/bill-jesse/cancel');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ expectedVersion: 2 });
    request.flush({});
  });
});
