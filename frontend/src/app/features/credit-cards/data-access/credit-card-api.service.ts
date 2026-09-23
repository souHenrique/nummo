import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiUrlService } from '../../../core/http/api-url.service';
import { Transaction } from '../../transactions/models/transaction.models';
import {
  CreateCreditCardPurchaseRequest,
  UpdateCreditCardPurchaseRequest,
  CreateCreditCardRequest,
  CreditCard,
  CreditCardRefund,
  CreditCardRefundRequest,
  UpdateCreditCardRequest,
} from '../models/credit-card.models';

@Injectable({ providedIn: 'root' })
export class CreditCardApiService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = inject(ApiUrlService);

  create(request: CreateCreditCardRequest): Observable<CreditCard> {
    return this.http.post<CreditCard>(this.apiUrl.build('credit-cards'), request);
  }

  findAll(): Observable<CreditCard[]> {
    return this.http.get<CreditCard[]>(this.apiUrl.build('credit-cards'));
  }

  findById(id: string): Observable<CreditCard> {
    return this.http.get<CreditCard>(this.apiUrl.build(`credit-cards/${encodeURIComponent(id)}`));
  }

  update(id: string, request: UpdateCreditCardRequest): Observable<CreditCard> {
    return this.http.patch<CreditCard>(
      this.apiUrl.build(`credit-cards/${encodeURIComponent(id)}`),
      request,
    );
  }

  createPurchase(id: string, request: CreateCreditCardPurchaseRequest): Observable<Transaction[]> {
    return this.http.post<Transaction[]>(
      this.apiUrl.build(`credit-cards/${encodeURIComponent(id)}/purchases`),
      request,
    );
  }

  updatePurchase(
    creditCardId: string,
    transactionId: string,
    request: UpdateCreditCardPurchaseRequest,
  ): Observable<Transaction[]> {
    return this.http.patch<Transaction[]>(
      this.apiUrl.build(
        `credit-cards/${encodeURIComponent(creditCardId)}/purchases/${encodeURIComponent(transactionId)}`,
      ),
      request,
    );
  }

  refundPurchase(
    creditCardId: string,
    transactionId: string,
    request: CreditCardRefundRequest,
  ): Observable<CreditCardRefund> {
    return this.http.post<CreditCardRefund>(
      this.apiUrl.build(
        `credit-cards/${encodeURIComponent(creditCardId)}/purchase/${encodeURIComponent(
          transactionId,
        )}/refund`,
      ),
      request,
    );
  }
}
