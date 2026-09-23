import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiUrlService } from '../../../core/http/api-url.service';
import { PageResponse } from '../../../shared/models/pagination';
import {
  CloseInvoiceRequest,
  InvoiceDetail,
  InvoicePayment,
  InvoiceQuery,
  InvoiceSummary,
  PayInvoiceRequest,
} from '../models/invoice.models';

@Injectable({ providedIn: 'root' })
export class InvoiceApiService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = inject(ApiUrlService);

  findAll(query: InvoiceQuery = {}): Observable<PageResponse<InvoiceSummary>> {
    return this.http.get<PageResponse<InvoiceSummary>>(this.apiUrl.build('invoices'), {
      params: this.buildParams(query),
    });
  }

  findById(id: string): Observable<InvoiceDetail> {
    return this.http.get<InvoiceDetail>(this.apiUrl.build(`invoices/${encodeURIComponent(id)}`));
  }

  findByCreditCard(
    creditCardId: string,
    query: InvoiceQuery = {},
  ): Observable<PageResponse<InvoiceSummary>> {
    return this.http.get<PageResponse<InvoiceSummary>>(
      this.apiUrl.build(`credit-cards/${encodeURIComponent(creditCardId)}/invoices`),
      { params: this.buildParams(query) },
    );
  }

  close(id: string, request: CloseInvoiceRequest): Observable<InvoiceSummary> {
    return this.http.post<InvoiceSummary>(
      this.apiUrl.build(`invoices/${encodeURIComponent(id)}/close`),
      request,
    );
  }

  reopen(id: string, request: CloseInvoiceRequest): Observable<InvoiceSummary> {
    return this.http.post<InvoiceSummary>(
      this.apiUrl.build(`invoices/${encodeURIComponent(id)}/reopen`),
      request,
    );
  }

  pay(id: string, request: PayInvoiceRequest): Observable<InvoicePayment> {
    return this.http.post<InvoicePayment>(
      this.apiUrl.build(`invoices/${encodeURIComponent(id)}/pay`),
      request,
    );
  }

  private buildParams(query: InvoiceQuery): HttpParams {
    let params = new HttpParams();

    const values: [string, string | number | undefined][] = [
      ['creditCardId', query.creditCardId],
      ['referenceMonth', query.referenceMonth],
      ['referenceYear', query.referenceYear],
      ['status', query.status],
      ['page', query.page],
      ['size', query.size],
    ];

    for (const [name, value] of values) {
      if (value !== undefined) {
        params = params.set(name, String(value));
      }
    }

    if (query.sort !== undefined) {
      const values = Array.isArray(query.sort) ? query.sort : [query.sort];

      for (const sort of values) {
        params = params.append('sort', sort);
      }
    }

    return params;
  }
}
