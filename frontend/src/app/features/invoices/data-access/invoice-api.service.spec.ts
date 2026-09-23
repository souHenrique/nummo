import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { API_BASE_URL } from '../../../core/config/api-base-url';
import { PageResponse } from '../../../shared/models/pagination';
import {
  CloseInvoiceRequest,
  InvoiceDetail,
  InvoicePayment,
  InvoiceSummary,
  PayInvoiceRequest,
} from '../models/invoice.models';
import { InvoiceApiService } from './invoice-api.service';

describe('InvoiceApiService', () => {
  let service: InvoiceApiService;
  let httpMock: HttpTestingController;

  const invoice: InvoiceSummary = {
    id: '72486234-ef50-4c7e-99a7-9193a28533a8',
    creditCardId: 'd89835ee-3463-4a35-a2e9-38d96ab17418',
    referenceMonth: 9,
    referenceYear: 2026,
    closingDate: '2026-09-20',
    dueDate: '2026-09-28',
    totalAmount: 850.75,
    status: 'OPEN',
    paidAt: null,
    version: 0,
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        InvoiceApiService,
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: API_BASE_URL,
          useValue: '/api/v1',
        },
      ],
    });

    service = TestBed.inject(InvoiceApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('deve listar faturas com filtros e paginação', () => {
    const page: PageResponse<InvoiceSummary> = {
      content: [invoice],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
      first: true,
      last: true,
    };

    service
      .findAll({
        creditCardId: invoice.creditCardId,
        referenceMonth: 9,
        referenceYear: 2026,
        status: 'OPEN',
        page: 0,
        size: 20,
        sort: ['referenceYear,desc', 'referenceMonth,desc'],
      })
      .subscribe((response) => {
        expect(response).toEqual(page);
      });

    const request = httpMock.expectOne((candidate) => candidate.url === '/api/v1/invoices');

    expect(request.request.method).toBe('GET');
    expect(request.request.params.get('creditCardId')).toBe(invoice.creditCardId);
    expect(request.request.params.get('referenceMonth')).toBe('9');
    expect(request.request.params.get('referenceYear')).toBe('2026');
    expect(request.request.params.get('status')).toBe('OPEN');
    expect(request.request.params.get('page')).toBe('0');
    expect(request.request.params.get('size')).toBe('20');
    expect(request.request.params.getAll('sort')).toEqual([
      'referenceYear,desc',
      'referenceMonth,desc',
    ]);

    request.flush(page);
  });

  it('deve buscar os detalhes de uma fatura', () => {
    const detail: InvoiceDetail = {
      ...invoice,
      transactions: [],
      creditAppliedAmount: 0,
    };

    service.findById(invoice.id).subscribe((response) => {
      expect(response).toEqual(detail);
    });

    const request = httpMock.expectOne(`/api/v1/invoices/${invoice.id}`);

    expect(request.request.method).toBe('GET');

    request.flush(detail);
  });

  it('deve listar as faturas de um cartão', () => {
    const page: PageResponse<InvoiceSummary> = {
      content: [invoice],
      page: 1,
      size: 10,
      totalElements: 11,
      totalPages: 2,
      first: false,
      last: true,
    };

    service
      .findByCreditCard(invoice.creditCardId, {
        referenceYear: 2026,
        referenceMonth: 9,
        status: 'OPEN',
        page: 1,
        size: 10,
      })
      .subscribe((response) => {
        expect(response).toEqual(page);
      });

    const request = httpMock.expectOne(
      (candidate) => candidate.url === `/api/v1/credit-cards/${invoice.creditCardId}/invoices`,
    );

    expect(request.request.method).toBe('GET');
    expect(request.request.params.get('referenceYear')).toBe('2026');
    expect(request.request.params.get('referenceMonth')).toBe('9');
    expect(request.request.params.get('status')).toBe('OPEN');
    expect(request.request.params.get('page')).toBe('1');
    expect(request.request.params.get('size')).toBe('10');

    request.flush(page);
  });

  it('deve fechar uma fatura com a versão esperada', () => {
    const payload: CloseInvoiceRequest = {
      expectedVersion: 0,
    };
    const response: InvoiceSummary = {
      ...invoice,
      status: 'CLOSED',
      version: 1,
    };

    service.close(invoice.id, payload).subscribe((result) => {
      expect(result).toEqual(response);
    });

    const request = httpMock.expectOne(`/api/v1/invoices/${invoice.id}/close`);

    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(payload);

    request.flush(response);
  });

  it('deve reabrir uma fatura com a versão esperada', () => {
    const payload: CloseInvoiceRequest = { expectedVersion: 1 };
    const response: InvoiceSummary = { ...invoice, status: 'OPEN', version: 2 };

    service.reopen(invoice.id, payload).subscribe((result) => {
      expect(result).toEqual(response);
    });

    const request = httpMock.expectOne(`/api/v1/invoices/${invoice.id}/reopen`);

    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(payload);

    request.flush(response);
  });

  it('deve pagar uma fatura com a versão esperada', () => {
    const payload: PayInvoiceRequest = {
      sourceAccountId: '0f6d7313-77f8-4b48-a63d-5338dd95461e',
      paymentDate: '2026-09-28',
      expectedVersion: 1,
    };
    const response: InvoicePayment = {
      invoiceId: invoice.id,
      totalAmount: invoice.totalAmount,
      creditAppliedAmount: 50,
      cashPaidAmount: 800.75,
      paymentTransactionId: '8ed48f2a-6ad0-4851-b069-0bd64d211b8d',
      paidAt: '2026-09-28T14:30:00Z',
    };

    service.pay(invoice.id, payload).subscribe((result) => {
      expect(result).toEqual(response);
    });

    const request = httpMock.expectOne(`/api/v1/invoices/${invoice.id}/pay`);

    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(payload);

    request.flush(response);
  });
});
