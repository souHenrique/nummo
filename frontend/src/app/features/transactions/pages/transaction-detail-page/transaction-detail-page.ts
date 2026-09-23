import { CurrencyPipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { EMPTY, catchError, finalize, forkJoin, map, of, switchMap, take } from 'rxjs';

import { AppDialogService } from '../../../../core/feedback/dialog/dialog.service';
import { ToastService } from '../../../../core/feedback/toast/toast.service';
import { Alert } from '../../../../shared/ui/alert/alert';
import { Badge } from '../../../../shared/ui/badge/badge';
import { Button } from '../../../../shared/ui/button/button';
import { Card } from '../../../../shared/ui/card/card';
import { ErrorState } from '../../../../shared/ui/error-state/error-state';
import { Skeleton } from '../../../../shared/ui/skeleton/skeleton';
import { IsoDatePipe } from '../../../../shared/pipes/iso-date.pipe';
import { AccountApiService } from '../../../accounts/data-access/account-api.service';
import { Account } from '../../../accounts/models/account.models';
import { CategoryApiService } from '../../../categories/data-access/category-api.service';
import { Category } from '../../../categories/models/category.models';
import { CreditCardApiService } from '../../../credit-cards/data-access/credit-card-api.service';
import { CreditCard } from '../../../credit-cards/models/credit-card.models';
import { TransactionApiService } from '../../data-access/transaction-api.service';
import {
  PaymentMethod,
  Transaction,
  TransactionInstallmentDetails,
  TransactionStatus,
  TransactionType,
} from '../../models/transaction.models';

@Component({
  selector: 'app-transaction-detail-page',
  imports: [Alert, Badge, Button, Card, CurrencyPipe, ErrorState, IsoDatePipe, Skeleton],
  templateUrl: './transaction-detail-page.html',
  styleUrl: './transaction-detail-page.scss',
})
export class TransactionDetailPage implements OnInit {
  private readonly accountApi = inject(AccountApiService);
  private readonly categoryApi = inject(CategoryApiService);
  private readonly creditCardApi = inject(CreditCardApiService);
  private readonly dialog = inject(AppDialogService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);
  private readonly transactionApi = inject(TransactionApiService);

  private readonly transactionId = this.route.snapshot.paramMap.get('id');

  readonly accounts = signal<Account[]>([]);
  readonly categories = signal<Category[]>([]);
  readonly creditCards = signal<CreditCard[]>([]);
  readonly hasLoadError = signal(false);
  readonly isCancelling = signal(false);
  readonly isLoading = signal(true);
  readonly installmentDetails = signal<TransactionInstallmentDetails | null>(null);
  readonly transaction = signal<Transaction | null>(null);

  ngOnInit(): void {
    this.loadTransaction();
  }

  loadTransaction(): void {
    if (!this.transactionId) {
      void this.router.navigate(['/transactions']);
      return;
    }

    this.isLoading.set(true);
    this.hasLoadError.set(false);
    this.installmentDetails.set(null);

    forkJoin({
      transaction: this.transactionApi.findById(this.transactionId),
      accounts: this.accountApi.findAll().pipe(catchError(() => of([]))),
      categories: this.categoryApi.findAll().pipe(catchError(() => of([]))),
      creditCards: this.creditCardApi.findAll().pipe(catchError(() => of([]))),
    })
      .pipe(
        switchMap((data) => {
          if (!this.isInstallmentPurchase(data.transaction)) {
            return of({ ...data, installmentDetails: null });
          }

          return this.transactionApi
            .findInstallmentDetails(data.transaction.id)
            .pipe(map((installmentDetails) => ({ ...data, installmentDetails })));
        }),
        finalize(() => this.isLoading.set(false)),
      )
      .subscribe({
        next: ({ transaction, accounts, categories, creditCards, installmentDetails }) => {
          this.transaction.set(transaction);
          this.accounts.set(accounts);
          this.categories.set(categories);
          this.creditCards.set(creditCards);
          this.installmentDetails.set(installmentDetails);
        },
        error: () => {
          this.hasLoadError.set(true);
        },
      });
  }

  confirmCancellation(): void {
    const currentTransaction = this.transaction();

    if (
      !currentTransaction ||
      !this.isCancellationAllowed(currentTransaction) ||
      this.isCancelling()
    ) {
      return;
    }

    this.isCancelling.set(true);

    this.dialog
      .confirm({
        title: 'Cancelar transação?',
        message:
          'A transação continuará no histórico como cancelada e seu impacto no saldo será revertido.',
        confirmLabel: 'Cancelar transação',
        cancelLabel: 'Voltar',
        danger: true,
      })
      .pipe(
        take(1),
        switchMap((confirmed) =>
          confirmed ? this.transactionApi.cancel(currentTransaction.id) : EMPTY,
        ),
        finalize(() => this.isCancelling.set(false)),
      )
      .subscribe({
        next: (transaction) => {
          this.transaction.set(transaction);

          this.toast.show({
            tone: 'success',
            title: 'Transação cancelada',
            message: 'A transação foi cancelada e permanece no histórico.',
          });
        },
      });
  }

  goToEdit(): void {
    const currentTransaction = this.transaction();

    if (!currentTransaction || !this.isEditable(currentTransaction)) {
      return;
    }

    void this.router.navigate(['/transactions', currentTransaction.id, 'edit']);
  }

  goBack(): void {
    void this.router.navigate(['/transactions']);
  }

  categoryName(categoryId: string | null): string {
    if (!categoryId) {
      return '—';
    }

    return (
      this.categories().find((category) => category.id === categoryId)?.name ?? 'Categoria removida'
    );
  }

  accountName(accountId: string | null): string {
    if (!accountId) {
      return '—';
    }

    return this.accounts().find((account) => account.id === accountId)?.name ?? 'Conta removida';
  }

  creditCardName(creditCardId: string | null): string {
    if (!creditCardId) {
      return '—';
    }

    return this.creditCards().find((card) => card.id === creditCardId)?.name ?? 'Cartão removido';
  }

  accountOrCardLabel(transaction: Transaction): string {
    if (transaction.creditCardId) {
      return this.creditCardName(transaction.creditCardId);
    }

    if (
      transaction.type === 'TRANSFER' &&
      transaction.sourceAccountId &&
      transaction.destinationAccountId
    ) {
      return `${this.accountName(transaction.sourceAccountId)} → ${this.accountName(
        transaction.destinationAccountId,
      )}`;
    }

    return this.accountName(transaction.sourceAccountId ?? transaction.destinationAccountId);
  }

  typeLabel(type: TransactionType): string {
    const labels: Record<TransactionType, string> = {
      INCOME: 'Receita',
      EXPENSE: 'Despesa',
      TRANSFER: 'Transferência',
      CREDIT_CARD_PURCHASE: 'Compra no cartão',
      CREDIT_CARD_PAYMENT: 'Pagamento de fatura',
      ADJUSTMENT: 'Ajuste',
    };

    return labels[type];
  }

  statusLabel(status: TransactionStatus): string {
    const labels: Record<TransactionStatus, string> = {
      PENDING: 'Pendente',
      COMPLETED: 'Concluída',
      CANCELLED: 'Cancelada',
    };

    return labels[status];
  }

  statusTone(status: TransactionStatus): 'neutral' | 'success' | 'warning' {
    if (status === 'COMPLETED') {
      return 'success';
    }

    if (status === 'PENDING') {
      return 'warning';
    }

    return 'neutral';
  }

  paymentMethodLabel(paymentMethod: PaymentMethod): string {
    const labels: Record<PaymentMethod, string> = {
      DEBIT: 'Débito',
      PIX: 'PIX',
      CASH: 'Dinheiro',
      TRANSFER: 'Transferência',
      CREDIT_CARD: 'Cartão de crédito',
      OTHER: 'Outro',
    };

    return labels[paymentMethod];
  }

  isEditable(transaction: Transaction): boolean {
    return transaction.status !== 'CANCELLED';
  }

  isCancellationAllowed(transaction: Transaction): boolean {
    return (
      transaction.status !== 'CANCELLED' &&
      transaction.type !== 'CREDIT_CARD_PURCHASE' &&
      transaction.type !== 'CREDIT_CARD_PAYMENT'
    );
  }

  isInstallmentPurchase(transaction: Transaction): boolean {
    return (
      transaction.type === 'CREDIT_CARD_PURCHASE' &&
      transaction.installmentGroupId !== null &&
      (transaction.installmentCount ?? 0) > 1
    );
  }
}
