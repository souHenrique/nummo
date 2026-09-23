import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { Router } from '@angular/router';
import { EMPTY, catchError, finalize, of, switchMap, take } from 'rxjs';

import { AppDialogService } from '../../../../core/feedback/dialog/dialog.service';
import { ToastService } from '../../../../core/feedback/toast/toast.service';
import { Button } from '../../../../shared/ui/button/button';
import { ErrorState } from '../../../../shared/ui/error-state/error-state';
import { FormField } from '../../../../shared/ui/form-field/form-field';
import { InputDirective } from '../../../../shared/ui/form-control/input';
import { CurrencyInputDirective } from '../../../../shared/ui/form-control/currency-input';
import { SelectDirective } from '../../../../shared/ui/form-control/select';
import { Skeleton } from '../../../../shared/ui/skeleton/skeleton';
import { AccountApiService } from '../../../accounts/data-access/account-api.service';
import { Account } from '../../../accounts/models/account.models';
import { TransferApiService } from '../../data-access/transfer-api.service';
import { CreateTransferRequest } from '../../models/transfer.models';

type AccountsState = 'loading' | 'success' | 'error';

function differentAccountsValidator(control: AbstractControl): ValidationErrors | null {
  const { sourceAccountId, destinationAccountId } = control.value as CreateTransferRequest;

  if (sourceAccountId && destinationAccountId && sourceAccountId === destinationAccountId) {
    return { sameAccount: true };
  }

  return null;
}

@Component({
  selector: 'app-transfer-create-page',
  imports: [
    ReactiveFormsModule,
    Button,
    ErrorState,
    FormField,
    CurrencyInputDirective,
    InputDirective,
    SelectDirective,
    Skeleton,
  ],
  templateUrl: './transfer-create-page.html',
  styleUrl: './transfer-create-page.scss',
})
export class TransferCreatePage implements OnInit {
  private readonly accountApi = inject(AccountApiService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly dialog = inject(AppDialogService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);
  private readonly transferApi = inject(TransferApiService);

  readonly accounts = signal<Account[]>([]);
  readonly accountsState = signal<AccountsState>('loading');
  readonly isProcessing = signal(false);
  readonly sourceAccountId = signal('');
  readonly destinationAccountId = signal('');

  readonly sourceAccounts = computed(() =>
    this.accounts().filter((account) => account.id !== this.destinationAccountId()),
  );

  readonly destinationAccounts = computed(() =>
    this.accounts().filter((account) => account.id !== this.sourceAccountId()),
  );

  readonly form = this.formBuilder.group(
    {
      sourceAccountId: ['', Validators.required],
      destinationAccountId: ['', Validators.required],
      amount: [null as number | null, [Validators.required, Validators.min(0.01)]],
      date: ['', Validators.required],
      description: ['', [Validators.required, Validators.maxLength(255)]],
    },
    {
      validators: differentAccountsValidator,
    },
  );

  ngOnInit(): void {
    this.loadAccounts();
  }

  loadAccounts(): void {
    this.accountsState.set('loading');

    this.accountApi
      .findAll()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (accounts) => {
          this.accounts.set(this.toActiveSortedAccounts(accounts));
          this.accountsState.set('success');
        },
        error: () => {
          this.accountsState.set('error');
        },
      });
  }

  onSourceAccountChange(): void {
    const sourceAccountId = this.form.controls.sourceAccountId.value;

    this.sourceAccountId.set(sourceAccountId ?? '');

    if (sourceAccountId === this.form.controls.destinationAccountId.value) {
      this.form.controls.destinationAccountId.setValue('');
      this.destinationAccountId.set('');
    }
  }

  onDestinationAccountChange(): void {
    const destinationAccountId = this.form.controls.destinationAccountId.value;

    this.destinationAccountId.set(destinationAccountId ?? '');

    if (destinationAccountId === this.form.controls.sourceAccountId.value) {
      this.form.controls.sourceAccountId.setValue('');
      this.sourceAccountId.set('');
    }
  }

  submit(): void {
    if (this.form.invalid || this.isProcessing()) {
      this.form.markAllAsTouched();
      return;
    }

    const request = this.toRequest();
    const sourceAccount = this.accountName(request.sourceAccountId);
    const destinationAccount = this.accountName(request.destinationAccountId);

    this.isProcessing.set(true);

    this.dialog
      .confirm({
        title: 'Confirmar transferência?',
        message: `Transferir ${this.formatAmount(request.amount)} de ${sourceAccount} para ${destinationAccount}.`,
        confirmLabel: 'Confirmar transferência',
        cancelLabel: 'Revisar dados',
      })
      .pipe(
        take(1),
        switchMap((confirmed) => (confirmed ? this.transferApi.create(request) : EMPTY)),
        finalize(() => this.isProcessing.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (transaction) => {
          this.refreshAccounts();

          this.toast.show({
            tone: 'success',
            title: 'Transferência concluída',
            message: 'Os saldos das contas foram atualizados.',
          });

          void this.router.navigate(['/transactions', transaction.id]);
        },
      });
  }

  goBack(): void {
    void this.router.navigate(['/transactions']);
  }

  private refreshAccounts(): void {
    this.accountApi
      .findAll()
      .pipe(
        catchError(() => of([])),
        take(1),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((accounts) => {
        this.accounts.set(this.toActiveSortedAccounts(accounts));
      });
  }

  private toRequest(): CreateTransferRequest {
    const value = this.form.getRawValue();

    return {
      sourceAccountId: value.sourceAccountId!,
      destinationAccountId: value.destinationAccountId!,
      amount: value.amount!,
      date: value.date!,
      description: value.description!.trim(),
    };
  }

  private toActiveSortedAccounts(accounts: Account[]): Account[] {
    return accounts
      .filter((account) => account.status === 'ACTIVE')
      .sort((first, second) => first.name.localeCompare(second.name, 'pt-BR'));
  }

  private accountName(accountId: string): string {
    return this.accounts().find((account) => account.id === accountId)?.name ?? 'Conta selecionada';
  }

  private formatAmount(amount: number): string {
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL',
    }).format(amount);
  }
}
