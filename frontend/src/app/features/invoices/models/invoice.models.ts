import { PageQuery } from '../../../shared/models/pagination';
import { Transaction } from '../../transactions/models/transaction.models';

export type InvoiceStatus = 'OPEN' | 'CLOSED' | 'PAID' | 'CANCELLED';

export interface InvoiceSummary {
  id: string;
  creditCardId: string;
  referenceMonth: number;
  referenceYear: number;
  closingDate: string;
  dueDate: string;
  totalAmount: number;
  status: InvoiceStatus;
  paidAt: string | null;
  version: number;
}

export interface InvoiceDetail extends InvoiceSummary {
  transactions: Transaction[];
  creditAppliedAmount: number;
}

export interface InvoiceFilters {
  creditCardId?: string;
  referenceMonth?: number;
  referenceYear?: number;
  status?: InvoiceStatus;
}

export type InvoiceQuery = InvoiceFilters & PageQuery;

export interface CloseInvoiceRequest {
  expectedVersion: number;
}

export interface PayInvoiceRequest {
  sourceAccountId?: string | null;
  paymentDate: string;
  expectedVersion: number;
}

export interface InvoicePayment {
  invoiceId: string;
  totalAmount: number;
  creditAppliedAmount: number;
  cashPaidAmount: number;
  paymentTransactionId: string | null;
  paidAt: string;
}
