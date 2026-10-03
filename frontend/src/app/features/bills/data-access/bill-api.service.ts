import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiUrlService } from '../../../core/http/api-url.service';
import {
  Bill,
  BillPage,
  BillStatus,
  CreateBillRequest,
  PayBillRequest,
  UpdateBillRequest,
} from '../models/bill.models';

@Injectable({ providedIn: 'root' })
export class BillApiService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = inject(ApiUrlService);

  list(year: number, month: number, status: BillStatus | '' = '', page = 0): Observable<BillPage> {
    let params = new HttpParams()
      .set('year', year)
      .set('month', month)
      .set('page', page)
      .set('size', 20);
    if (status) params = params.set('status', status);
    return this.http.get<BillPage>(this.apiUrl.build('bills'), { params });
  }

  create(request: CreateBillRequest): Observable<Bill[]> {
    return this.http.post<Bill[]>(this.apiUrl.build('bills'), request);
  }

  update(id: string, request: UpdateBillRequest): Observable<Bill> {
    return this.http.patch<Bill>(this.url(id), request);
  }

  pay(id: string, request: PayBillRequest): Observable<Bill> {
    return this.http.post<Bill>(`${this.url(id)}/pay`, request);
  }

  cancel(id: string, expectedVersion: number): Observable<Bill> {
    return this.http.post<Bill>(`${this.url(id)}/cancel`, { expectedVersion });
  }

  private url(id: string): string {
    return this.apiUrl.build(`bills/${encodeURIComponent(id)}`);
  }
}
