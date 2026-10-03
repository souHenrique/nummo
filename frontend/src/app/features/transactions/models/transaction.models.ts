import { PageQuery } from '../../../shared/models/pagination';

export type TransactionType =
  'INCOME' | 'EXPENSE' | 'TRANSFER' | 'CREDIT_CARD_PURCHASE' | 'CREDIT_CARD_PAYMENT' | 'ADJUSTMENT';

export type TransactionStatus = 'PENDING' | 'COMPLETED' | 'CANCELLED';

export type PaymentMethod =
  'DEBIT' | 'PIX' | 'CASH' | 'TRANSFER' | 'CREDIT_CARD' | 'OTHER' | 'BOLETO';

export interface Transaction {
  id: string;
  description: string;
  amount: number;
  competenceDate: string;
  effectiveDate: string | null;
  dueDate: string | null;
  type: TransactionType;
  status: TransactionStatus;
  paymentMethod: PaymentMethod;
  sourceAccountId: string | null;
  destinationAccountId: string | null;
  categoryId: string | null;
  creditCardId: string | null;
  invoiceId: string | null;
  installmentGroupId: string | null;
  installmentNumber: number | null;
  installmentCount: number | null;
  createdAt: string;
  updatedAt: string;
}

export interface TransactionListItem {
  transaction: Transaction;
  displayAmount: number;
  installmentPurchase: boolean;
}

export interface TransactionInstallmentDetails {
  totalAmount: number;
  installments: Transaction[];
}

export interface CreateTransactionRequest {
  description: string;
  amount: number;
  competenceDate: string;
  effectiveDate?: string | null;
  dueDate?: string | null;
  type: TransactionType;
  status: TransactionStatus;
  paymentMethod: PaymentMethod;
  sourceAccountId?: string | null;
  destinationAccountId?: string | null;
  categoryId: string;
  creditCardId?: string | null;
  invoiceId?: string | null;
  installmentGroupId?: string | null;
  installmentNumber?: number | null;
  installmentCount?: number | null;
}

export interface UpdateTransactionRequest {
  description?: string;
  amount?: number;
  competenceDate?: string;
  effectiveDate?: string | null;
  status?: TransactionStatus;
  paymentMethod?: PaymentMethod;
  sourceAccountId?: string;
  destinationAccountId?: string;
  categoryId?: string;
}

export interface TransactionFilters {
  startDate?: string;
  endDate?: string;
  categoryId?: string;
  accountId?: string;
  creditCardId?: string;
  type?: TransactionType;
  status?: TransactionStatus;
  minAmount?: number;
  maxAmount?: number;
  description?: string;
}

export type TransactionQuery = TransactionFilters & PageQuery;
