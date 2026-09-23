import { Component, computed, effect, inject, input, output, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { Account } from '../../../accounts/models/account.models';
import { Category } from '../../../categories/models/category.models';
import {
  CreateTransactionRequest,
  PaymentMethod,
  Transaction,
  TransactionStatus,
  TransactionType,
  UpdateTransactionRequest,
} from '../../models/transaction.models';

import { Button } from '../../../../shared/ui/button/button';
import { FormField } from '../../../../shared/ui/form-field/form-field';
import { InputDirective } from '../../../../shared/ui/form-control/input';
import { CurrencyInputDirective } from '../../../../shared/ui/form-control/currency-input';
import { SelectDirective } from '../../../../shared/ui/form-control/select';

export type TransactionFormMode = 'create' | 'edit';

type TransactionFormResult = CreateTransactionRequest | UpdateTransactionRequest;

@Component({
  selector: 'app-transaction-form',
  imports: [
    ReactiveFormsModule,
    Button,
    CurrencyInputDirective,
    FormField,
    InputDirective,
    SelectDirective,
  ],
  templateUrl: './transaction-form.html',
  styleUrl: './transaction-form.scss',
})
export class TransactionFormComponent {
  private readonly formBuilder = inject(FormBuilder);

  readonly mode = input.required<TransactionFormMode>();
  readonly transaction = input<Transaction | null>(null);
  readonly accounts = input<Account[]>([]);
  readonly categories = input<Category[]>([]);
  readonly submitting = input(false);

  readonly submitted = output<TransactionFormResult>();
  readonly cancelled = output<void>();

  readonly transactionTypes = [
    { value: 'INCOME', label: 'Receita' },
    { value: 'EXPENSE', label: 'Despesa' },
  ] as const;

  readonly statuses = [
    { value: 'PENDING', label: 'Pendente' },
    { value: 'COMPLETED', label: 'Concluída' },
  ] as const;

  readonly paymentMethods = [
    { value: 'DEBIT', label: 'Débito' },
    { value: 'PIX', label: 'PIX' },
    { value: 'CASH', label: 'Dinheiro' },
    { value: 'TRANSFER', label: 'Transferência' },
    { value: 'OTHER', label: 'Outro' },
  ] as const;

  readonly form = this.formBuilder.group({
    type: [null as TransactionType | null, Validators.required],
    description: ['', [Validators.required, Validators.maxLength(255)]],
    amount: [null as number | null, [Validators.required, Validators.min(0.01)]],
    competenceDate: ['', Validators.required],
    status: ['COMPLETED' as TransactionStatus, Validators.required],
    effectiveDate: ['', Validators.required],
    paymentMethod: ['PIX' as PaymentMethod, Validators.required],
    sourceAccountId: [''],
    destinationAccountId: [''],
    categoryId: ['', Validators.required],
  });

  readonly selectedType = signal<TransactionType | null>(null);

  readonly isCardManagedEdit = computed(
    () =>
      this.mode() === 'edit' &&
      (this.selectedType() === 'CREDIT_CARD_PURCHASE' ||
        this.selectedType() === 'CREDIT_CARD_PAYMENT'),
  );

  readonly canEditCategory = computed(() => this.selectedType() !== 'CREDIT_CARD_PAYMENT');

  readonly activeAccounts = computed(() =>
    this.accounts().filter((account) => account.status === 'ACTIVE'),
  );

  readonly visibleCategories = computed(() =>
    this.categories().filter(
      (category) =>
        category.status === 'ACTIVE' &&
        category.type === (this.selectedType() === 'INCOME' ? 'INCOME' : 'EXPENSE'),
    ),
  );

  constructor() {
    effect(() => {
      const transaction = this.transaction();

      if (this.mode() !== 'edit' || !transaction) {
        return;
      }

      this.form.patchValue({
        type: transaction.type,
        description: transaction.description,
        amount: transaction.amount,
        competenceDate: transaction.competenceDate,
        status: transaction.status,
        effectiveDate: transaction.effectiveDate ?? '',
        paymentMethod: transaction.paymentMethod,
        sourceAccountId: transaction.sourceAccountId ?? '',
        destinationAccountId: transaction.destinationAccountId ?? '',
        categoryId: transaction.categoryId ?? '',
      });

      this.form.controls.type.disable();
      this.selectedType.set(transaction.type);
      this.syncConditionalControls();
    });
  }

  onTypeChange(): void {
    this.selectedType.set(this.form.controls.type.value);
    this.form.controls.categoryId.setValue('');
    this.form.controls.sourceAccountId.setValue('');
    this.form.controls.destinationAccountId.setValue('');
    this.syncConditionalControls();
  }

  onStatusChange(): void {
    this.syncConditionalControls();
  }

  private syncConditionalControls(): void {
    const type = this.selectedType();
    const status = this.form.controls.status.value;

    if (this.isCardManagedEdit()) {
      this.form.controls.categoryId.setValidators(
        type === 'CREDIT_CARD_PURCHASE' ? Validators.required : [],
      );
      this.form.controls.categoryId.updateValueAndValidity();
      return;
    }

    if (type === 'INCOME') {
      this.form.controls.destinationAccountId.setValidators(Validators.required);
      this.form.controls.sourceAccountId.clearValidators();
      this.form.controls.sourceAccountId.setValue('');
    }

    if (type === 'EXPENSE') {
      this.form.controls.sourceAccountId.setValidators(Validators.required);
      this.form.controls.destinationAccountId.clearValidators();
      this.form.controls.destinationAccountId.setValue('');
    }

    if (status === 'PENDING') {
      this.form.controls.effectiveDate.clearValidators();
      this.form.controls.effectiveDate.setValue('');
    } else {
      this.form.controls.effectiveDate.setValidators(Validators.required);
    }

    this.form.controls.sourceAccountId.updateValueAndValidity();
    this.form.controls.destinationAccountId.updateValueAndValidity();
    this.form.controls.effectiveDate.updateValueAndValidity();
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    if (this.mode() === 'create') {
      this.submitted.emit(this.toCreateRequest());
      return;
    }

    const request = this.toUpdateRequest();

    if (Object.keys(request).length > 0) {
      this.submitted.emit(request);
    }
  }

  private toCreateRequest(): CreateTransactionRequest {
    const value = this.form.getRawValue();
    const type = value.type!;

    return {
      description: value.description!.trim(),
      amount: value.amount!,
      competenceDate: value.competenceDate!,
      effectiveDate: value.status === 'COMPLETED' ? value.effectiveDate : null,
      type,
      status: value.status!,
      paymentMethod: value.paymentMethod!,
      categoryId: value.categoryId!,
      sourceAccountId: type === 'EXPENSE' ? value.sourceAccountId! : null,
      destinationAccountId: type === 'INCOME' ? value.destinationAccountId! : null,
    };
  }

  private changed<T>(current: T, original: T): T | undefined {
    return current === original ? undefined : current;
  }

  private toUpdateRequest(): UpdateTransactionRequest {
    const original = this.transaction();

    if (!original) {
      return {};
    }

    const value = this.form.getRawValue();
    const request: UpdateTransactionRequest = {};

    const description = value.description!.trim();

    if (description !== original.description) {
      request.description = description;
    }

    if (this.isCardManagedEdit()) {
      if (original.type === 'CREDIT_CARD_PURCHASE' && value.categoryId !== original.categoryId) {
        request.categoryId = value.categoryId!;
      }

      return request;
    }

    if (value.amount !== original.amount) {
      request.amount = value.amount!;
    }

    if (value.competenceDate !== original.competenceDate) {
      request.competenceDate = value.competenceDate!;
    }

    if (value.status !== original.status) {
      request.status = value.status!;
    }

    if (value.paymentMethod !== original.paymentMethod) {
      request.paymentMethod = value.paymentMethod!;
    }

    const effectiveDate = value.status === 'COMPLETED' ? value.effectiveDate : null;

    if (effectiveDate !== original.effectiveDate) {
      request.effectiveDate = effectiveDate;
    }

    if (value.categoryId !== original.categoryId) {
      request.categoryId = value.categoryId!;
    }

    if (original.type === 'EXPENSE' && value.sourceAccountId !== original.sourceAccountId) {
      request.sourceAccountId = value.sourceAccountId!;
    }

    if (
      original.type === 'INCOME' &&
      value.destinationAccountId !== original.destinationAccountId
    ) {
      request.destinationAccountId = value.destinationAccountId!;
    }

    return request;
  }

  typeLabel(type: TransactionType | null): string {
    const labels: Record<TransactionType, string> = {
      INCOME: 'Receita',
      EXPENSE: 'Despesa',
      TRANSFER: 'Transferência',
      CREDIT_CARD_PURCHASE: 'Compra no cartão',
      CREDIT_CARD_PAYMENT: 'Pagamento de fatura',
      ADJUSTMENT: 'Ajuste',
    };

    return type ? labels[type] : '';
  }
}
