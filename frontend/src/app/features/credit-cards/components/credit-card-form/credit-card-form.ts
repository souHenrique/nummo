import { Component, computed, effect, inject, input, output } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { Account } from '../../../accounts/models/account.models';
import { Button } from '../../../../shared/ui/button/button';
import { InputDirective } from '../../../../shared/ui/form-control/input';
import { CurrencyInputDirective } from '../../../../shared/ui/form-control/currency-input';
import { SelectDirective } from '../../../../shared/ui/form-control/select';
import { FormField } from '../../../../shared/ui/form-field/form-field';
import {
  CreateCreditCardRequest,
  CreditCard,
  UpdateCreditCardRequest,
} from '../../models/credit-card.models';

export type CreditCardFormMode = 'create' | 'edit';

export interface CreditCardFormSubmit {
  create: CreateCreditCardRequest;
  update: UpdateCreditCardRequest;
}

@Component({
  selector: 'app-credit-card-form',
  imports: [
    ReactiveFormsModule,
    Button,
    CurrencyInputDirective,
    FormField,
    InputDirective,
    SelectDirective,
  ],
  templateUrl: './credit-card-form.html',
  styleUrl: './credit-card-form.scss',
})
export class CreditCardFormComponent {
  private readonly formBuilder = inject(FormBuilder);

  readonly mode = input.required<CreditCardFormMode>();
  readonly creditCard = input<CreditCard | null>(null);
  readonly accounts = input<Account[]>([]);
  readonly submitting = input(false);

  readonly submitted = output<CreditCardFormSubmit>();
  readonly cancelled = output<void>();

  readonly isCreateMode = computed(() => this.mode() === 'create');

  readonly activeAccounts = computed(() =>
    this.accounts()
      .filter((account) => account.status === 'ACTIVE')
      .sort((first, second) => first.name.localeCompare(second.name, 'pt-BR')),
  );

  readonly committedLimit = computed(() => {
    const creditCard = this.creditCard();

    if (!creditCard) {
      return 0;
    }

    return this.getCommittedLimit(creditCard);
  });

  readonly creditLimitHint = computed(() => {
    if (this.isCreateMode()) {
      return 'Informe o limite total concedido pela instituição financeira.';
    }

    return `O limite não pode ser menor que ${this.formatCurrency(this.committedLimit())}, pois esse valor já está comprometido.`;
  });

  readonly form = this.formBuilder.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(120)]],
    creditLimit: [0, [Validators.required, Validators.min(0.01)]],
    closingDay: [1, [Validators.required, Validators.min(1), Validators.max(31)]],
    dueDay: [1, [Validators.required, Validators.min(1), Validators.max(31)]],
    defaultAccountId: ['', Validators.required],
  });

  private readonly creditCardSynchronization = effect(() => {
    const creditCard = this.creditCard();
    const minimumCreditLimit = creditCard ? this.getCommittedLimit(creditCard) : 0.01;

    this.form.controls.creditLimit.setValidators([
      Validators.required,
      Validators.min(minimumCreditLimit),
    ]);

    this.form.controls.creditLimit.updateValueAndValidity({ emitEvent: false });

    if (!creditCard) {
      return;
    }

    this.form.patchValue(
      {
        name: creditCard.name,
        creditLimit: creditCard.creditLimit,
        closingDay: creditCard.closingDay,
        dueDay: creditCard.dueDay,
        defaultAccountId: creditCard.defaultAccountId,
      },
      { emitEvent: false },
    );
  });

  submit(): void {
    if (this.form.invalid || this.submitting() || this.activeAccounts().length === 0) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();

    const create: CreateCreditCardRequest = {
      name: value.name.trim(),
      creditLimit: value.creditLimit,
      closingDay: value.closingDay,
      dueDay: value.dueDay,
      defaultAccountId: value.defaultAccountId,
    };

    const update: UpdateCreditCardRequest = {
      name: create.name,
      creditLimit: create.creditLimit,
      closingDay: create.closingDay,
      dueDay: create.dueDay,
      defaultAccountId: create.defaultAccountId,
    };

    this.submitted.emit({ create, update });
  }

  creditLimitError(): string | undefined {
    const control = this.form.controls.creditLimit;

    if (!control.touched || control.valid) {
      return undefined;
    }

    if (control.hasError('required')) {
      return 'Informe o limite total do cartão.';
    }

    if (control.hasError('min')) {
      return this.isCreateMode()
        ? 'O limite deve ser maior que zero.'
        : `O limite não pode ser menor que ${this.formatCurrency(this.committedLimit())}.`;
    }

    return 'Informe um limite válido.';
  }

  dayError(controlName: 'closingDay' | 'dueDay'): string | undefined {
    const control = this.form.controls[controlName];

    if (!control.touched || control.valid) {
      return undefined;
    }

    return 'Informe um dia inteiro entre 1 e 31.';
  }

  private getCommittedLimit(creditCard: CreditCard): number {
    return Math.max(0, creditCard.creditLimit - creditCard.availableLimit);
  }

  private formatCurrency(value: number): string {
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL',
    }).format(value);
  }
}
