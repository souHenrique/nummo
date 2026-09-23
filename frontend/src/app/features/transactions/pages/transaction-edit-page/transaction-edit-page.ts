import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { finalize, forkJoin } from 'rxjs';

import { ToastService } from '../../../../core/feedback/toast/toast.service';
import { ApiRequestError } from '../../../../core/http/api-request-error';
import { Alert } from '../../../../shared/ui/alert/alert';
import { Button } from '../../../../shared/ui/button/button';
import { ErrorState } from '../../../../shared/ui/error-state/error-state';
import { Skeleton } from '../../../../shared/ui/skeleton/skeleton';
import { AccountApiService } from '../../../accounts/data-access/account-api.service';
import { Account } from '../../../accounts/models/account.models';
import { CategoryApiService } from '../../../categories/data-access/category-api.service';
import { Category } from '../../../categories/models/category.models';
import { CreditCardApiService } from '../../../credit-cards/data-access/credit-card-api.service';
import { UpdateCreditCardPurchaseRequest } from '../../../credit-cards/models/credit-card.models';
import { TransactionFormComponent } from '../../components/transaction-form/transaction-form';
import { TransactionApiService } from '../../data-access/transaction-api.service';
import {
  CreateTransactionRequest,
  Transaction,
  UpdateTransactionRequest,
} from '../../models/transaction.models';

@Component({
  selector: 'app-transaction-edit-page',
  imports: [Alert, Button, ErrorState, ReactiveFormsModule, Skeleton, TransactionFormComponent],
  templateUrl: './transaction-edit-page.html',
  styleUrl: './transaction-edit-page.scss',
})
export class TransactionEditPage implements OnInit {
  private readonly accountApi = inject(AccountApiService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly creditCardApi = inject(CreditCardApiService);
  private readonly categoryApi = inject(CategoryApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);
  private readonly transactionApi = inject(TransactionApiService);

  private readonly transactionId = this.route.snapshot.paramMap.get('id');

  readonly accounts = signal<Account[]>([]);
  readonly categories = signal<Category[]>([]);
  readonly hasConflict = signal(false);
  readonly hasLoadError = signal(false);
  readonly isLoading = signal(true);
  readonly isSubmitting = signal(false);
  readonly transaction = signal<Transaction | null>(null);
  readonly purchaseForm = this.formBuilder.group({
    description: ['', [Validators.required, Validators.maxLength(255)]],
    amount: [null as number | null, [Validators.required, Validators.min(0.01)]],
    purchaseDate: ['', Validators.required],
    categoryId: ['', Validators.required],
    installmentCount: [1, [Validators.required, Validators.min(1)]],
  });

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    if (!this.transactionId) {
      void this.router.navigate(['/transactions']);
      return;
    }

    this.isLoading.set(true);
    this.hasLoadError.set(false);

    forkJoin({
      transaction: this.transactionApi.findById(this.transactionId),
      accounts: this.accountApi.findAll(),
      categories: this.categoryApi.findAll(),
    })
      .pipe(finalize(() => this.isLoading.set(false)))
      .subscribe({
        next: ({ transaction, accounts, categories }) => {
          this.transaction.set(transaction);
          if (this.isCreditCardPurchase(transaction)) {
            this.purchaseForm.patchValue({
              description: transaction.description,
              amount: transaction.amount * (transaction.installmentCount ?? 1),
              purchaseDate: transaction.competenceDate,
              categoryId: transaction.categoryId ?? '',
              installmentCount: transaction.installmentCount ?? 1,
            });
          }

          this.accounts.set(
            [...accounts].sort((first, second) => first.name.localeCompare(second.name, 'pt-BR')),
          );

          this.categories.set(
            [...categories].sort((first, second) => first.name.localeCompare(second.name, 'pt-BR')),
          );
        },
        error: () => {
          this.hasLoadError.set(true);
        },
      });
  }

  update(payload: CreateTransactionRequest | UpdateTransactionRequest): void {
    const currentTransaction = this.transaction();

    if (
      !currentTransaction ||
      !this.transactionId ||
      this.isSubmitting() ||
      !this.isEditable(currentTransaction) ||
      this.isCreateTransactionRequest(payload)
    ) {
      return;
    }

    if (Object.keys(payload).length === 0) {
      this.toast.show({
        tone: 'info',
        title: 'Nenhuma alteração',
        message: 'Altere pelo menos um campo antes de salvar.',
      });
      return;
    }

    this.hasConflict.set(false);
    this.isSubmitting.set(true);

    this.transactionApi
      .update(this.transactionId, payload)
      .pipe(finalize(() => this.isSubmitting.set(false)))
      .subscribe({
        next: (transaction) => {
          this.transaction.set(transaction);

          this.toast.show({
            tone: 'success',
            title: 'Transação atualizada',
            message: 'As alterações foram salvas com sucesso.',
          });

          void this.router.navigate(['/transactions', transaction.id]);
        },
        error: (error: unknown) => {
          if (error instanceof ApiRequestError && error.status === 409) {
            this.hasConflict.set(true);
          }
        },
      });
  }

  updatePurchase(): void {
    const transaction = this.transaction();
    if (!transaction || !this.transactionId || !transaction.creditCardId || this.isSubmitting())
      return;
    if (this.purchaseForm.invalid) {
      this.purchaseForm.markAllAsTouched();
      return;
    }
    this.isSubmitting.set(true);
    this.creditCardApi
      .updatePurchase(
        transaction.creditCardId,
        this.transactionId,
        this.purchaseForm.getRawValue() as UpdateCreditCardPurchaseRequest,
      )
      .pipe(finalize(() => this.isSubmitting.set(false)))
      .subscribe({
        next: (transactions) => {
          const updated = transactions[0];
          this.toast.show({
            tone: 'success',
            title: 'Compra atualizada',
            message: 'As alterações foram salvas.',
          });
          void this.router.navigate(['/transactions', updated.id]);
        },
      });
  }

  reloadAfterConflict(): void {
    this.hasConflict.set(false);
    this.loadData();
  }

  goBack(): void {
    const transaction = this.transaction();

    void this.router.navigate(transaction ? ['/transactions', transaction.id] : ['/transactions']);
  }

  isEditable(transaction: Transaction): boolean {
    return transaction.status !== 'CANCELLED';
  }

  isCreditCardPurchase(transaction: Transaction): boolean {
    return transaction.type === 'CREDIT_CARD_PURCHASE';
  }

  private isCreateTransactionRequest(
    payload: CreateTransactionRequest | UpdateTransactionRequest,
  ): payload is CreateTransactionRequest {
    return 'type' in payload && 'status' in payload && 'paymentMethod' in payload;
  }
}
