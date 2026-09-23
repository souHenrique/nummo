import { DecimalPipe } from '@angular/common';
import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { EMPTY, forkJoin, map, switchMap, take } from 'rxjs';

import { AccountApiService } from '../../../accounts/data-access/account-api.service';
import { Account } from '../../../accounts/models/account.models';
import { CreditCardApiService } from '../../../credit-cards/data-access/credit-card-api.service';
import { CreditCard } from '../../../credit-cards/models/credit-card.models';
import { Transaction } from '../../../transactions/models/transaction.models';
import { AppDialogService } from '../../../../core/feedback/dialog/dialog.service';
import { ToastService } from '../../../../core/feedback/toast/toast.service';
import { ApiRequestError } from '../../../../core/http/api-request-error';
import { Alert } from '../../../../shared/ui/alert/alert';
import { Badge } from '../../../../shared/ui/badge/badge';
import { Button } from '../../../../shared/ui/button/button';
import { Card } from '../../../../shared/ui/card/card';
import { ErrorState } from '../../../../shared/ui/error-state/error-state';
import { SelectDirective } from '../../../../shared/ui/form-control/select';
import { TextareaDirective } from '../../../../shared/ui/form-control/textarea';
import { FormField } from '../../../../shared/ui/form-field/form-field';
import { Skeleton } from '../../../../shared/ui/skeleton/skeleton';
import type { FeedbackTone } from '../../../../shared/ui/types/feedback-tone';
import { InvoiceApiService } from '../../data-access/invoice-api.service';
import { InvoiceDetail, InvoiceStatus } from '../../models/invoice.models';

type InvoiceDetailState = 'loading' | 'success' | 'error';
type InvoiceOperation =
  | 'confirming-close'
  | 'closing'
  | 'confirming-reopen'
  | 'reopening'
  | 'confirming-payment'
  | 'paying'
  | 'confirming-refund'
  | 'refunding'
  | null;

const nonBlankValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  return typeof control.value === 'string' && control.value.trim().length > 0
    ? null
    : { required: true };
};

@Component({
  selector: 'app-invoice-detail-page',
  imports: [
    DecimalPipe,
    ReactiveFormsModule,
    Alert,
    Badge,
    Button,
    Card,
    ErrorState,
    FormField,
    SelectDirective,
    Skeleton,
    TextareaDirective,
  ],
  templateUrl: './invoice-detail-page.html',
  styleUrl: './invoice-detail-page.scss',
})
export class InvoiceDetailPage implements OnInit {
  private readonly accountApi = inject(AccountApiService);
  private readonly creditCardApi = inject(CreditCardApiService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly dialog = inject(AppDialogService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly invoiceApi = inject(InvoiceApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);

  private readonly invoiceId = this.route.snapshot.paramMap.get('id');

  readonly invoice = signal<InvoiceDetail | null>(null);
  readonly creditCard = signal<CreditCard | null>(null);
  readonly accounts = signal<Account[]>([]);
  readonly state = signal<InvoiceDetailState>('loading');
  readonly operation = signal<InvoiceOperation>(null);
  readonly isPaymentFormOpen = signal(false);
  readonly conflictMessage = signal('');
  readonly refundTransaction = signal<Transaction | null>(null);
  readonly isRefundFormSubmitted = signal(false);

  readonly activeAccounts = computed(() =>
    this.accounts()
      .filter((account) => account.status === 'ACTIVE')
      .sort((first, second) => first.name.localeCompare(second.name, 'pt-BR')),
  );

  readonly isProcessing = computed(() => this.operation() !== null);

  readonly paymentForm = this.formBuilder.nonNullable.group({
    sourceAccountId: [''],
    paymentDate: ['', Validators.required],
  });

  readonly refundForm = this.formBuilder.nonNullable.group({
    reason: ['', [nonBlankValidator, Validators.maxLength(500)]],
  });

  ngOnInit(): void {
    this.loadInvoice();
  }

  loadInvoice(preserveConflictMessage = false): void {
    if (!this.invoiceId) {
      void this.router.navigate(['/invoices']);
      return;
    }

    this.state.set('loading');

    if (!preserveConflictMessage) {
      this.conflictMessage.set('');
    }

    this.isPaymentFormOpen.set(false);

    this.invoiceApi
      .findById(this.invoiceId)
      .pipe(
        switchMap((invoice) =>
          forkJoin({
            creditCard: this.creditCardApi.findById(invoice.creditCardId),
            accounts: this.accountApi.findAll(),
          }).pipe(
            map(({ creditCard, accounts }) => ({
              invoice,
              creditCard,
              accounts,
            })),
          ),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: ({ invoice, creditCard, accounts }) => {
          this.invoice.set(invoice);
          this.creditCard.set(creditCard);
          this.accounts.set(accounts);
          this.selectDefaultPaymentAccount(creditCard, accounts);
          this.state.set('success');
        },
        error: () => {
          this.state.set('error');
        },
      });
  }

  openPaymentForm(): void {
    const invoice = this.invoice();

    if (invoice?.status !== 'CLOSED' || this.isProcessing()) {
      return;
    }

    this.isPaymentFormOpen.set(true);
  }

  closeInvoice(): void {
    const invoice = this.invoice();

    if (!invoice || invoice.status !== 'OPEN' || this.isProcessing()) {
      return;
    }

    this.operation.set('confirming-close');

    this.dialog
      .confirm({
        title: 'Fechar fatura?',
        message: 'Depois de fechada, a fatura não aceitará novas compras.',
        confirmLabel: 'Fechar fatura',
        cancelLabel: 'Cancelar',
      })
      .pipe(
        take(1),
        switchMap((confirmed) => {
          if (!confirmed) {
            this.operation.set(null);
            return EMPTY;
          }

          this.operation.set('closing');

          return this.invoiceApi.close(invoice.id, {
            expectedVersion: invoice.version,
          });
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => {
          this.operation.set(null);

          this.toast.show({
            tone: 'success',
            title: 'Fatura fechada',
            message: 'A fatura foi fechada e está pronta para pagamento.',
          });

          this.loadInvoice();
        },
        error: (error: unknown) => {
          this.operation.set(null);
          this.handleMutationError(error);
        },
      });
  }

  reopenInvoice(): void {
    const invoice = this.invoice();
    if (
      !invoice ||
      (invoice.status !== 'CLOSED' && invoice.status !== 'PAID') ||
      this.isProcessing()
    )
      return;
    this.operation.set('confirming-reopen');
    this.dialog
      .confirm({
        title: 'Reabrir fatura?',
        message:
          invoice.status === 'PAID'
            ? 'O pagamento será desfeito e a fatura voltará a aceitar correções nas compras e parcelas.'
            : 'A fatura voltará a aceitar correções nas compras e parcelas.',
        confirmLabel: 'Reabrir fatura',
        cancelLabel: 'Cancelar',
      })
      .pipe(
        take(1),
        switchMap((confirmed) => {
          if (!confirmed) {
            this.operation.set(null);
            return EMPTY;
          }
          this.operation.set('reopening');
          return this.invoiceApi.reopen(invoice.id, { expectedVersion: invoice.version });
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => {
          this.operation.set(null);
          this.toast.show({
            tone: 'success',
            title: 'Fatura reaberta',
            message: 'Agora você pode corrigir as compras desta fatura.',
          });
          this.loadInvoice();
        },
        error: (error: unknown) => {
          this.operation.set(null);
          this.handleMutationError(error);
        },
      });
  }

  payInvoice(): void {
    const invoice = this.invoice();

    if (!invoice || invoice.status !== 'CLOSED' || this.isProcessing()) {
      return;
    }

    if (this.paymentForm.invalid) {
      this.paymentForm.markAllAsTouched();
      return;
    }

    const sourceAccountId = this.paymentForm.controls.sourceAccountId.value || null;
    const paymentDate = this.paymentForm.controls.paymentDate.value;
    const sourceAccount = this.activeAccounts().find((account) => account.id === sourceAccountId);
    const paymentSource = sourceAccount
      ? `usando a conta ${sourceAccount.name}`
      : 'com os créditos disponíveis';

    this.operation.set('confirming-payment');

    this.dialog
      .confirm({
        title: 'Pagar fatura?',
        message: `A fatura de ${this.formatAmount(invoice.totalAmount)} será quitada ${paymentSource} em ${this.formatDate(paymentDate)}.`,
        confirmLabel: 'Pagar fatura',
        cancelLabel: 'Revisar dados',
        danger: true,
      })
      .pipe(
        take(1),
        switchMap((confirmed) => {
          if (!confirmed) {
            this.operation.set(null);
            return EMPTY;
          }

          this.operation.set('paying');

          return this.invoiceApi.pay(invoice.id, {
            sourceAccountId,
            paymentDate,
            expectedVersion: invoice.version,
          });
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => {
          this.operation.set(null);

          this.toast.show({
            tone: 'success',
            title: 'Fatura paga',
            message: 'O pagamento foi registrado e o limite do cartão foi atualizado.',
          });

          this.loadInvoice();
        },
        error: (error: unknown) => {
          this.operation.set(null);
          this.handleMutationError(error);
        },
      });
  }

  openRefundForm(transaction: Transaction): void {
    if (!this.isRefundEligible(transaction) || this.isProcessing()) {
      return;
    }

    this.refundTransaction.set(transaction);
    this.isRefundFormSubmitted.set(false);
    this.refundForm.reset({ reason: '' });
    this.refundForm.markAsPristine();
    this.refundForm.markAsUntouched();
  }

  cancelRefund(): void {
    if (this.isProcessing()) {
      return;
    }

    this.refundTransaction.set(null);
    this.isRefundFormSubmitted.set(false);
    this.refundForm.reset({ reason: '' });
  }

  submitRefund(): void {
    const creditCard = this.creditCard();
    const transaction = this.refundTransaction();

    if (!creditCard || !transaction || !this.isRefundEligible(transaction) || this.isProcessing()) {
      return;
    }

    this.isRefundFormSubmitted.set(true);

    if (this.refundForm.invalid) {
      this.refundForm.markAllAsTouched();
      return;
    }

    const reason = this.refundForm.controls.reason.value.trim();

    this.operation.set('confirming-refund');

    this.dialog
      .confirm({
        title: 'Estornar compra?',
        message: `A compra “${transaction.description}” de ${this.formatAmount(transaction.amount)} será estornada integralmente.`,
        confirmLabel: 'Confirmar estorno',
        cancelLabel: 'Voltar',
        danger: true,
      })
      .pipe(
        take(1),
        switchMap((confirmed) => {
          if (!confirmed) {
            this.operation.set(null);
            return EMPTY;
          }

          this.operation.set('refunding');

          return this.creditCardApi.refundPurchase(creditCard.id, transaction.id, { reason });
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => {
          this.operation.set(null);
          this.refundTransaction.set(null);
          this.isRefundFormSubmitted.set(false);
          this.refundForm.reset({ reason: '' });

          this.toast.show({
            tone: 'success',
            title: 'Estorno registrado',
            message: 'O cartão, as faturas e as transações foram atualizados.',
          });

          this.loadInvoice();
        },
        error: () => {
          this.operation.set(null);
        },
      });
  }

  goBack(): void {
    void this.router.navigate(['/invoices']);
  }

  goToCreditCard(): void {
    const creditCard = this.creditCard();

    if (creditCard) {
      void this.router.navigate(['/credit-cards', creditCard.id]);
    }
  }

  referenceLabel(invoice: InvoiceDetail): string {
    return `${String(invoice.referenceMonth).padStart(2, '0')}/${invoice.referenceYear}`;
  }

  formatDate(value: string): string {
    const [year, month, day] = value.split('-');

    return `${day}/${month}/${year}`;
  }

  statusLabel(status: InvoiceStatus): string {
    const labels: Record<InvoiceStatus, string> = {
      OPEN: 'Aberta',
      CLOSED: 'Fechada',
      PAID: 'Paga',
      CANCELLED: 'Cancelada',
    };

    return labels[status];
  }

  statusTone(status: InvoiceStatus): FeedbackTone {
    const tones: Record<InvoiceStatus, FeedbackTone> = {
      OPEN: 'info',
      CLOSED: 'warning',
      PAID: 'success',
      CANCELLED: 'neutral',
    };

    return tones[status];
  }

  transactionTypeLabel(type: Transaction['type']): string {
    const labels: Record<Transaction['type'], string> = {
      INCOME: 'Receita',
      EXPENSE: 'Despesa',
      TRANSFER: 'Transferência',
      CREDIT_CARD_PURCHASE: 'Compra no cartão',
      CREDIT_CARD_PAYMENT: 'Pagamento de fatura',
      ADJUSTMENT: 'Ajuste',
    };

    return labels[type];
  }

  transactionStatusLabel(status: Transaction['status']): string {
    const labels: Record<Transaction['status'], string> = {
      PENDING: 'Pendente',
      COMPLETED: 'Concluída',
      CANCELLED: 'Cancelada',
    };

    return labels[status];
  }

  transactionStatusTone(status: Transaction['status']): FeedbackTone {
    const tones: Record<Transaction['status'], FeedbackTone> = {
      PENDING: 'warning',
      COMPLETED: 'success',
      CANCELLED: 'neutral',
    };

    return tones[status];
  }

  installmentLabel(transaction: Transaction): string {
    if (
      transaction.type !== 'CREDIT_CARD_PURCHASE' ||
      !transaction.installmentCount ||
      transaction.installmentCount === 1
    ) {
      return 'À vista';
    }

    return `Parcela ${transaction.installmentNumber} de ${transaction.installmentCount}`;
  }

  isRefundEligible(transaction: Transaction): boolean {
    const creditCard = this.creditCard();

    return (
      transaction.type === 'CREDIT_CARD_PURCHASE' &&
      transaction.paymentMethod === 'CREDIT_CARD' &&
      transaction.status === 'COMPLETED' &&
      transaction.creditCardId === creditCard?.id &&
      transaction.invoiceId !== null
    );
  }

  refundReasonError(): string | undefined {
    const control = this.refundForm.controls.reason;

    if (!this.isRefundFormSubmitted() && !control.touched) {
      return undefined;
    }

    if (control.hasError('required')) {
      return 'Informe o motivo do estorno.';
    }

    if (control.hasError('maxlength')) {
      return 'O motivo deve ter no máximo 500 caracteres.';
    }

    return undefined;
  }

  private selectDefaultPaymentAccount(creditCard: CreditCard, accounts: Account[]): void {
    const activeAccounts = accounts.filter((account) => account.status === 'ACTIVE');

    const selectedAccount =
      activeAccounts.find((account) => account.id === creditCard.defaultAccountId) ??
      activeAccounts[0];

    this.paymentForm.reset({
      sourceAccountId: selectedAccount?.id ?? '',
      paymentDate: new Date().toISOString().slice(0, 10),
    });
  }

  private handleMutationError(error: unknown): void {
    if (error instanceof ApiRequestError && error.status === 409) {
      this.conflictMessage.set(
        `${error.message} Os dados atualizados foram carregados. Revise-os e confirme a operação novamente.`,
      );
      this.loadInvoice(true);
    }
  }

  private formatAmount(amount: number): string {
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL',
    }).format(amount);
  }
}
