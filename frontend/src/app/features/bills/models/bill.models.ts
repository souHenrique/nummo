export type BillStatus = 'PENDING' | 'PAID' | 'CANCELLED';

export interface Bill {
  id: string;
  description: string;
  amount: number;
  dueDate: string;
  categoryId: string;
  seriesId: string;
  installmentNumber: number;
  installmentCount: number;
  status: BillStatus;
  paymentTransactionId: string | null;
  version: number;
}

export interface BillPage {
  content: Bill[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface CreateBillRequest {
  description: string;
  amount: number;
  firstDueDate: string;
  installmentCount: number;
  categoryId: string;
}

export interface UpdateBillRequest {
  description: string;
  amount: number;
  dueDate: string;
  categoryId: string;
  expectedVersion: number;
}

export interface PayBillRequest {
  sourceAccountId: string;
  paymentDate: string;
  expectedVersion: number;
}
