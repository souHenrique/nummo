import { Component, effect, inject, input, output } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';

import { Account } from '../../../accounts/models/account.models';
import { Category } from '../../../categories/models/category.models';
import { CreditCard } from '../../../credit-cards/models/credit-card.models';
import {
  TransactionFilters,
  TransactionStatus,
  TransactionType,
} from '../../models/transaction.models';

import { Button } from '../../../../shared/ui/button/button';
import { FormField } from '../../../../shared/ui/form-field/form-field';
import { InputDirective } from '../../../../shared/ui/form-control/input';
import { CurrencyInputDirective } from '../../../../shared/ui/form-control/currency-input';
import { SelectDirective } from '../../../../shared/ui/form-control/select';

function dateRangeValidator(control: AbstractControl): ValidationErrors | null {
  const { startDate, endDate } = control.value as TransactionFilters;

  if (startDate && endDate && startDate > endDate) {
    return { invalidDateRange: true };
  }

  return null;
}

function amountRangeValidator(control: AbstractControl): ValidationErrors | null {
  const { minAmount, maxAmount } = control.value as TransactionFilters;

  if (
    minAmount !== null &&
    minAmount !== undefined &&
    maxAmount !== null &&
    maxAmount !== undefined &&
    minAmount > maxAmount
  ) {
    return { invalidAmountRange: true };
  }

  return null;
}

@Component({
  selector: 'app-transaction-filter-form',
  imports: [
    ReactiveFormsModule,
    Button,
    CurrencyInputDirective,
    FormField,
    InputDirective,
    SelectDirective,
  ],
  templateUrl: './transaction-filter-form.html',
  styleUrl: './transaction-filter-form.scss',
})
export class TransactionFilterFormComponent {
  private readonly formBuilder = inject(FormBuilder);

  readonly initialFilters = input<TransactionFilters>({});
  readonly accounts = input<Account[]>([]);
  readonly categories = input<Category[]>([]);
  readonly creditCards = input<CreditCard[]>([]);
  readonly submitting = input(false);

  readonly applied = output<TransactionFilters>();
  readonly cleared = output<void>();

  readonly types: { value: TransactionType; label: string }[] = [
    { value: 'INCOME', label: 'Receita' },
    { value: 'EXPENSE', label: 'Despesa' },
    { value: 'TRANSFER', label: 'Transferência' },
    { value: 'CREDIT_CARD_PURCHASE', label: 'Compra no cartão' },
    { value: 'CREDIT_CARD_PAYMENT', label: 'Pagamento de fatura' },
    { value: 'ADJUSTMENT', label: 'Ajuste' },
  ];

  readonly statuses: { value: TransactionStatus; label: string }[] = [
    { value: 'PENDING', label: 'Pendente' },
    { value: 'COMPLETED', label: 'Concluída' },
    { value: 'CANCELLED', label: 'Cancelada' },
  ];

  readonly form = this.formBuilder.group(
    {
      startDate: [''],
      endDate: [''],
      categoryId: [''],
      accountId: [''],
      creditCardId: [''],
      type: [null as TransactionType | null],
      status: [null as TransactionStatus | null],
      minAmount: [null as number | null, [Validators.min(0)]],
      maxAmount: [null as number | null, [Validators.min(0)]],
      description: ['', [Validators.maxLength(255)]],
    },
    {
      validators: [dateRangeValidator, amountRangeValidator],
    },
  );

  constructor() {
    effect(() => {
      const filters = this.initialFilters();

      this.form.reset({
        startDate: filters.startDate ?? '',
        endDate: filters.endDate ?? '',
        categoryId: filters.categoryId ?? '',
        accountId: filters.accountId ?? '',
        creditCardId: filters.creditCardId ?? '',
        type: filters.type ?? null,
        status: filters.status ?? null,
        minAmount: filters.minAmount ?? null,
        maxAmount: filters.maxAmount ?? null,
        description: filters.description ?? '',
      });
    });
  }

  apply(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.applied.emit(this.toFilters());
  }

  clear(): void {
    this.form.reset({
      startDate: '',
      endDate: '',
      categoryId: '',
      accountId: '',
      creditCardId: '',
      type: null,
      status: null,
      minAmount: null,
      maxAmount: null,
      description: '',
    });

    this.cleared.emit();
  }

  private toFilters(): TransactionFilters {
    const value = this.form.getRawValue();
    const filters: TransactionFilters = {};

    if (value.startDate) {
      filters.startDate = value.startDate;
    }

    if (value.endDate) {
      filters.endDate = value.endDate;
    }

    if (value.categoryId) {
      filters.categoryId = value.categoryId;
    }

    if (value.accountId) {
      filters.accountId = value.accountId;
    }

    if (value.creditCardId) {
      filters.creditCardId = value.creditCardId;
    }

    if (value.type) {
      filters.type = value.type;
    }

    if (value.status) {
      filters.status = value.status;
    }

    if (value.minAmount !== null && !Number.isNaN(value.minAmount)) {
      filters.minAmount = value.minAmount;
    }

    if (value.maxAmount !== null && !Number.isNaN(value.maxAmount)) {
      filters.maxAmount = value.maxAmount;
    }

    const description = value.description?.trim();

    if (description) {
      filters.description = description;
    }

    return filters;
  }
}
