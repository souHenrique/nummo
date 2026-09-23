import { Component, computed, inject, input, output } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';

import { Alert } from '../../../../shared/ui/alert/alert';
import { Button } from '../../../../shared/ui/button/button';
import { InputDirective } from '../../../../shared/ui/form-control/input';
import { CurrencyInputDirective } from '../../../../shared/ui/form-control/currency-input';
import { SelectDirective } from '../../../../shared/ui/form-control/select';
import { FormField } from '../../../../shared/ui/form-field/form-field';
import { Category } from '../../../categories/models/category.models';
import { CreateCreditCardPurchaseRequest } from '../../models/credit-card.models';

function integerValidator(control: AbstractControl): ValidationErrors | null {
  const value = control.value;

  if (value === null || value === '') {
    return null;
  }

  return Number.isInteger(Number(value)) ? null : { integer: true };
}

@Component({
  selector: 'app-credit-card-purchase-form',
  imports: [
    ReactiveFormsModule,
    Alert,
    Button,
    CurrencyInputDirective,
    FormField,
    InputDirective,
    SelectDirective,
  ],
  templateUrl: './credit-card-purchase-form.html',
  styleUrl: './credit-card-purchase-form.scss',
})
export class CreditCardPurchaseFormComponent {
  private readonly formBuilder = inject(FormBuilder);

  readonly categories = input<Category[]>([]);
  readonly submitting = input(false);

  readonly submitted = output<CreateCreditCardPurchaseRequest>();
  readonly cancelled = output<void>();

  readonly activeExpenseCategories = computed(() =>
    this.categories()
      .filter((category) => category.type === 'EXPENSE' && category.status === 'ACTIVE')
      .sort((first, second) => first.name.localeCompare(second.name, 'pt-BR')),
  );

  readonly form = this.formBuilder.group({
    description: ['', [Validators.required, Validators.maxLength(255)]],
    amount: [null as number | null, [Validators.required, Validators.min(0.01)]],
    purchaseDate: ['', Validators.required],
    categoryId: ['', Validators.required],
    installmentCount: [
      1 as number | null,
      [Validators.required, Validators.min(1), integerValidator],
    ],
  });

  submit(): void {
    if (this.form.invalid || this.submitting() || this.activeExpenseCategories().length === 0) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();

    this.submitted.emit({
      description: value.description!.trim(),
      amount: value.amount!,
      purchaseDate: value.purchaseDate!,
      categoryId: value.categoryId!,
      installmentCount: value.installmentCount!,
    });
  }

  descriptionError(): string | undefined {
    const control = this.form.controls.description;

    if (!control.touched || control.valid) {
      return undefined;
    }

    return 'Informe uma descrição de até 255 caracteres.';
  }

  amountError(): string | undefined {
    const control = this.form.controls.amount;

    if (!control.touched || control.valid) {
      return undefined;
    }

    return 'Informe um valor maior que zero.';
  }

  purchaseDateError(): string | undefined {
    const control = this.form.controls.purchaseDate;

    if (!control.touched || control.valid) {
      return undefined;
    }

    return 'Informe a data da compra.';
  }

  categoryError(): string | undefined {
    const control = this.form.controls.categoryId;

    if (!control.touched || control.valid) {
      return undefined;
    }

    return 'Selecione uma categoria de despesa.';
  }

  installmentCountError(): string | undefined {
    const control = this.form.controls.installmentCount;

    if (!control.touched || control.valid) {
      return undefined;
    }

    if (control.hasError('integer')) {
      return 'Informe uma quantidade inteira de parcelas.';
    }

    return 'Informe pelo menos uma parcela.';
  }

  formatAmount(amount: number | null): string {
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL',
    }).format(amount ?? 0);
  }
}
