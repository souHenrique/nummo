import type { InvoiceStatus } from '../../invoices/models/invoice.models';
import type { Transaction } from '../../transactions/models/transaction.models';

export type CreditCardStatus = 'ACTIVE' | 'INACTIVE' | 'BLOCKED';

export type CreditCardRefundTreatment = 'UNPAID_CANCELLATION' | 'FUTURE_INVOICE_CREDIT';

export interface CreditCard {
  id: string;
  name: string;
  creditLimit: number;
  availableLimit: number;
  closingDay: number;
  dueDay: number;
  defaultAccountId: string;
  status: CreditCardStatus;
  version: number;
}

export interface CreateCreditCardRequest {
  name: string;
  creditLimit: number;
  closingDay: number;
  dueDay: number;
  defaultAccountId: string;
}

export interface UpdateCreditCardRequest {
  name?: string;
  creditLimit?: number;
  closingDay?: number;
  dueDay?: number;
  defaultAccountId?: string;
  status?: CreditCardStatus;
}

export interface CreateCreditCardPurchaseRequest {
  description: string;
  amount: number;
  purchaseDate: string;
  categoryId: string;
  installmentCount: number;
}

export type UpdateCreditCardPurchaseRequest = CreateCreditCardPurchaseRequest;

export interface CreditCardRefundRequest {
  reason: string;
}

export interface CreditCardRefundItem {
  id: string;
  originalTransactionId: string;
  originalInvoiceId: string;
  originalInvoiceStatus: InvoiceStatus;
  amount: number;
  treatment: CreditCardRefundTreatment;
  creditId: string | null;
}

export interface CreditCardRefund {
  id: string;
  creditCardId: string;
  selectedTransactionId: string;
  installmentGroupId: string | null;
  reason: string;
  totalAmount: number;
  limitRestoredAmount: number;
  paidCompensationAmount: number;
  createdAt: string;
  items: CreditCardRefundItem[];
}

export type CreditCardPurchaseResponse = Transaction[];
