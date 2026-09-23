import { Component, OnInit, computed, inject, input, output } from '@angular/core';
import { ReactiveFormsModule, Validators, FormBuilder } from '@angular/forms';
import {
  Account,
  AccountType,
  CreateAccountRequest,
  UpdateAccountRequest,
} from '../../models/account.models';

import { Button } from '../../../../shared/ui/button/button';
import { FormField } from '../../../../shared/ui/form-field/form-field';
import { InputDirective } from '../../../../shared/ui/form-control/input';
import { CurrencyInputDirective } from '../../../../shared/ui/form-control/currency-input';
import { SelectDirective } from '../../../../shared/ui/form-control/select';
import { ACCOUNT_TYPE_OPTIONS } from '../../models/account-type.options';

export type AccountFormMode = 'create' | 'edit';

export interface AccountFormSubmit {
  create: CreateAccountRequest;
  update: UpdateAccountRequest;
}

@Component({
  selector: 'app-account-form',
  imports: [
    ReactiveFormsModule,
    Button,
    CurrencyInputDirective,
    FormField,
    InputDirective,
    SelectDirective,
  ],
  templateUrl: './account-form.html',
  styleUrl: './account-form.scss',
})
export class AccountFormComponent implements OnInit {
  private readonly formBuilder = inject(FormBuilder);

  readonly mode = input.required<AccountFormMode>();
  readonly account = input<Account | null>(null);
  readonly submitting = input(false);

  readonly submitted = output<AccountFormSubmit>();
  readonly cancelled = output<void>();

  readonly isCreateMode = computed(() => this.mode() === 'create');

  readonly accountTypes = ACCOUNT_TYPE_OPTIONS;

  readonly form = this.formBuilder.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(120)]],
    type: ['CHECKING' as AccountType, Validators.required],
    institution: [''],
    initialBalance: [0, [Validators.required]],
  });

  ngOnInit(): void {
    const account = this.account();

    if (account) {
      this.form.patchValue({
        name: account.name,
        type: account.type,
        institution: account.institution ?? '',
      });
    }

    if (!this.isCreateMode()) {
      this.form.controls.initialBalance.disable();
    }
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const rawValue = this.form.getRawValue();

    const create: CreateAccountRequest = {
      name: rawValue.name.trim(),
      type: rawValue.type,
      institution: rawValue.institution.trim() || null,
      initialBalance: rawValue.initialBalance,
    };

    const update: UpdateAccountRequest = {
      name: create.name,
      type: create.type,
      institution: rawValue.institution.trim(),
    };

    this.submitted.emit({ create, update });
  }
}
