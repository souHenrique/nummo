import { Component, effect, inject, input, output } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators,
} from '@angular/forms';

import { Button } from '../../../../shared/ui/button/button';
import { InputDirective } from '../../../../shared/ui/form-control/input';
import { CurrencyInputDirective } from '../../../../shared/ui/form-control/currency-input';
import { SelectDirective } from '../../../../shared/ui/form-control/select';
import { FormField } from '../../../../shared/ui/form-field/form-field';
import { Category } from '../../../categories/models/category.models';
import { Budget, CreateBudgetRequest, UpdateBudgetRequest } from '../../models/budget.models';

export type BudgetFormMode = 'create' | 'edit';

const MONTHS = [
  'Janeiro',
  'Fevereiro',
  'Março',
  'Abril',
  'Maio',
  'Junho',
  'Julho',
  'Agosto',
  'Setembro',
  'Outubro',
  'Novembro',
  'Dezembro',
] as const;

const MAX_AMOUNT_LIMIT = Number.MAX_SAFE_INTEGER;

const maxTwoDecimalPlaces: ValidatorFn = (
  control: AbstractControl<number>,
): ValidationErrors | null => {
  const value = control.value;

  if (!Number.isFinite(value) || Math.round(value * 100) === value * 100) {
    return null;
  }

  return { decimalPlaces: true };
};

@Component({
  selector: 'app-budget-form',
  imports: [
    ReactiveFormsModule,
    Button,
    CurrencyInputDirective,
    FormField,
    InputDirective,
    SelectDirective,
  ],
  templateUrl: './budget-form.html',
  styleUrl: './budget-form.scss',
})
export class BudgetFormComponent {
  private readonly formBuilder = inject(FormBuilder);

  readonly mode = input.required<BudgetFormMode>();
  readonly budget = input<Budget | null>(null);
  readonly categories = input.required<Category[]>();
  readonly defaultMonth = input.required<number>();
  readonly defaultYear = input.required<number>();
  readonly submitting = input(false);

  readonly created = output<CreateBudgetRequest>();
  readonly updated = output<UpdateBudgetRequest>();
  readonly cancelled = output<void>();

  readonly months = MONTHS;

  readonly form = this.formBuilder.nonNullable.group({
    categoryId: ['', Validators.required],
    month: [1, [Validators.required, Validators.min(1), Validators.max(12)]],
    year: [
      new Date().getFullYear(),
      [Validators.required, Validators.min(1), Validators.max(9999)],
    ],
    amountLimit: [
      0,
      [
        Validators.required,
        Validators.min(0.01),
        Validators.max(MAX_AMOUNT_LIMIT),
        maxTwoDecimalPlaces,
      ],
    ],
  });

  constructor() {
    effect(() => {
      const budget = this.budget();

      if (this.mode() === 'edit' && budget) {
        this.form.reset({
          categoryId: budget.categoryId,
          month: budget.month,
          year: budget.year,
          amountLimit: budget.amountLimit,
        });
        return;
      }

      this.form.reset({
        categoryId: '',
        month: this.defaultMonth(),
        year: this.defaultYear(),
        amountLimit: 0,
      });
    });
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();

    if (this.mode() === 'create') {
      this.created.emit(value);
      return;
    }

    const budget = this.budget();

    if (!budget) {
      return;
    }

    const request: UpdateBudgetRequest = {};

    if (value.categoryId !== budget.categoryId) {
      request.categoryId = value.categoryId;
    }

    if (value.month !== budget.month) {
      request.month = value.month;
    }

    if (value.year !== budget.year) {
      request.year = value.year;
    }

    if (value.amountLimit !== budget.amountLimit) {
      request.amountLimit = value.amountLimit;
    }

    if (Object.keys(request).length === 0) {
      return;
    }

    this.updated.emit(request);
  }

  categoryLabel(category: Category): string {
    return category.name;
  }

  fieldError(field: 'categoryId' | 'month' | 'year' | 'amountLimit'): string | undefined {
    const control = this.form.controls[field];

    if (!control.touched || control.valid) {
      return undefined;
    }

    if (field === 'categoryId') {
      return 'Selecione uma categoria de despesa.';
    }

    if (field === 'amountLimit') {
      if (control.hasError('decimalPlaces')) {
        return 'Informe um limite com até duas casas decimais.';
      }

      return 'Informe um limite maior que zero.';
    }

    return 'Informe um período válido.';
  }
}
