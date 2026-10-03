import { Component, computed, effect, inject, input, output } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Category } from '../../../categories/models/category.models';
import { Button } from '../../../../shared/ui/button/button';
import { CurrencyInputDirective } from '../../../../shared/ui/form-control/currency-input';
import { InputDirective } from '../../../../shared/ui/form-control/input';
import { SelectDirective } from '../../../../shared/ui/form-control/select';
import { FormField } from '../../../../shared/ui/form-field/form-field';
import { Bill, CreateBillRequest, UpdateBillRequest } from '../../models/bill.models';

@Component({
  selector: 'app-bill-form',
  imports: [
    ReactiveFormsModule,
    Button,
    CurrencyInputDirective,
    InputDirective,
    SelectDirective,
    FormField,
  ],
  templateUrl: './bill-form.html',
  styleUrl: './bill-form.scss',
})
export class BillForm {
  private readonly fb = inject(FormBuilder);
  readonly bill = input<Bill | null>(null);
  readonly categories = input.required<Category[]>();
  readonly defaultDueDate = input.required<string>();
  readonly submitting = input(false);
  readonly created = output<CreateBillRequest>();
  readonly updated = output<UpdateBillRequest>();
  readonly cancelled = output<void>();
  readonly activeCategories = computed(() =>
    this.categories().filter(
      (category) => category.type === 'EXPENSE' && category.status === 'ACTIVE',
    ),
  );

  readonly form = this.fb.nonNullable.group({
    description: ['', [Validators.required, Validators.pattern(/\S/), Validators.maxLength(255)]],
    amount: [
      0,
      [
        Validators.required,
        Validators.min(0.01),
        Validators.max(Number.MAX_SAFE_INTEGER / 100),
        Validators.pattern(/^\d+(\.\d{1,2})?$/),
      ],
    ],
    dueDate: ['', [Validators.required, Validators.pattern(/^(19|[2-9]\d)\d{2}-\d{2}-\d{2}$/)]],
    categoryId: ['', Validators.required],
    recurrence: ['single', Validators.required],
    installmentCount: [
      1,
      [Validators.required, Validators.min(1), Validators.max(600), Validators.pattern(/^\d+$/)],
    ],
  });

  constructor() {
    effect(() => {
      const bill = this.bill();
      this.form.reset({
        description: bill?.description ?? '',
        amount: bill?.amount ?? 0,
        dueDate: bill?.dueDate ?? this.defaultDueDate(),
        categoryId: bill?.categoryId ?? '',
        recurrence: 'single',
        installmentCount: 1,
      });
    });
    effect(() => {
      if (this.submitting()) this.form.disable({ emitEvent: false });
      else this.form.enable({ emitEvent: false });
    });
  }

  submit(): void {
    if (this.submitting()) return;
    const value = this.form.getRawValue();
    if (value.recurrence === 'single') this.form.controls.installmentCount.setValue(1);
    if (!this.activeCategories().some((category) => category.id === value.categoryId)) {
      this.form.controls.categoryId.setErrors({ unavailable: true });
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const common = {
      description: value.description.trim(),
      amount: value.amount,
      categoryId: value.categoryId,
    };
    const bill = this.bill();
    if (bill) {
      this.updated.emit({ ...common, dueDate: value.dueDate, expectedVersion: bill.version });
    } else {
      this.created.emit({
        ...common,
        firstDueDate: value.dueDate,
        installmentCount: value.recurrence === 'single' ? 1 : value.installmentCount,
      });
    }
  }

  fieldError(field: keyof typeof this.form.controls): string | undefined {
    const control = this.form.controls[field];
    if (!control.touched || control.valid) return undefined;
    const messages: Record<keyof typeof this.form.controls, string> = {
      description: 'Informe uma descrição de até 255 caracteres.',
      amount: 'Informe um valor maior que zero.',
      dueDate: 'Informe uma data de vencimento válida.',
      categoryId: 'Selecione uma categoria de despesa ativa.',
      recurrence: 'Selecione o tipo de boleto.',
      installmentCount: 'Informe uma quantidade inteira entre 1 e 600.',
    };
    return messages[field];
  }
}
