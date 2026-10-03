import { CurrencyPipe, DOCUMENT } from '@angular/common';
import {
  Component,
  DestroyRef,
  Injector,
  OnInit,
  afterNextRender,
  computed,
  inject,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { EMPTY, Observable, Subscription, finalize, forkJoin, switchMap, take } from 'rxjs';
import { AppDialogService } from '../../../core/feedback/dialog/dialog.service';
import { ToastService } from '../../../core/feedback/toast/toast.service';
import { ApiRequestError } from '../../../core/http/api-request-error';
import { Alert } from '../../../shared/ui/alert/alert';
import { Badge } from '../../../shared/ui/badge/badge';
import { Button } from '../../../shared/ui/button/button';
import { Card } from '../../../shared/ui/card/card';
import { EmptyState } from '../../../shared/ui/empty-state/empty-state';
import { ErrorState } from '../../../shared/ui/error-state/error-state';
import { InputDirective } from '../../../shared/ui/form-control/input';
import { SelectDirective } from '../../../shared/ui/form-control/select';
import { FormField } from '../../../shared/ui/form-field/form-field';
import { Pagination } from '../../../shared/ui/pagination/pagination';
import { Skeleton } from '../../../shared/ui/skeleton/skeleton';
import { FeedbackTone } from '../../../shared/ui/types/feedback-tone';
import { AccountApiService } from '../../accounts/data-access/account-api.service';
import { Account } from '../../accounts/models/account.models';
import { CategoryApiService } from '../../categories/data-access/category-api.service';
import { Category } from '../../categories/models/category.models';
import { BillForm } from '../components/bill-form/bill-form';
import { BillApiService } from '../data-access/bill-api.service';
import {
  Bill,
  BillPage,
  BillStatus,
  CreateBillRequest,
  UpdateBillRequest,
} from '../models/bill.models';

@Component({
  selector: 'app-bill-list-page',
  imports: [
    CurrencyPipe,
    ReactiveFormsModule,
    RouterLink,
    Alert,
    Badge,
    Button,
    Card,
    EmptyState,
    ErrorState,
    InputDirective,
    SelectDirective,
    FormField,
    Pagination,
    Skeleton,
    BillForm,
  ],
  templateUrl: './bill-list-page.html',
  styleUrl: './bill-list-page.scss',
})
export class BillListPage implements OnInit {
  private readonly api = inject(BillApiService);
  private readonly accountApi = inject(AccountApiService);
  private readonly categoryApi = inject(CategoryApiService);
  private readonly dialog = inject(AppDialogService);
  private readonly toast = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly fb = inject(FormBuilder);
  private readonly document = inject(DOCUMENT);
  private readonly injector = inject(Injector);
  private loadSubscription?: Subscription;

  readonly today = this.localDate(new Date());
  readonly period = signal(this.today.slice(0, 7));
  readonly statusFilter = signal<BillStatus | ''>('');
  readonly page = signal(0);
  readonly result = signal<BillPage | null>(null);
  readonly categories = signal<Category[]>([]);
  readonly accounts = signal<Account[]>([]);
  readonly state = signal<'loading' | 'success' | 'error'>('loading');
  readonly formOpen = signal(false);
  readonly editingBill = signal<Bill | null>(null);
  readonly paymentBill = signal<Bill | null>(null);
  readonly busy = signal(false);
  readonly mutationError = signal('');
  readonly conflict = signal(false);
  readonly activeAccounts = computed(() =>
    this.accounts().filter((account) => account.status === 'ACTIVE'),
  );
  readonly defaultDueDate = computed(() => `${this.period()}-01`);
  readonly paymentForm = this.fb.nonNullable.group({
    sourceAccountId: ['', Validators.required],
    paymentDate: [this.today, Validators.required],
  });

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.loadSubscription?.unsubscribe();
    this.state.set('loading');
    const [year, month] = this.period().split('-').map(Number);
    this.loadSubscription = forkJoin({
      bills: this.api.list(year, month, this.statusFilter(), this.page()),
      accounts: this.accountApi.findAll(),
      categories: this.categoryApi.findAll(),
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ({ bills, accounts, categories }) => {
          this.result.set(bills);
          this.accounts.set(accounts);
          this.categories.set(categories);
          this.state.set('success');
        },
        error: () => this.state.set('error'),
      });
  }

  selectPeriod(value: string): void {
    if (this.busy() || !/^(19|[2-9]\d)\d{2}-(0[1-9]|1[0-2])$/.test(value)) return;
    this.period.set(value);
    this.page.set(0);
    this.closeForms();
    this.loadData();
  }

  selectStatus(value: string): void {
    if (this.busy() || !['', 'PENDING', 'PAID', 'CANCELLED'].includes(value)) return;
    this.statusFilter.set(value as BillStatus | '');
    this.page.set(0);
    this.closeForms();
    this.loadData();
  }

  selectPage(value: number): void {
    if (this.busy()) return;
    this.page.set(value - 1);
    this.loadData();
  }

  openForm(bill: Bill | null = null): void {
    if (this.busy() || (bill && bill.status !== 'PENDING')) return;
    this.closeForms();
    this.editingBill.set(bill);
    this.formOpen.set(true);
    this.focusControl('bill-description');
  }

  create(request: CreateBillRequest): void {
    if (this.busy() || this.conflict()) return;
    this.busy.set(true);
    this.mutationError.set('');
    this.api
      .create(request)
      .pipe(
        finalize(() => this.busy.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => {
          this.period.set(request.firstDueDate.slice(0, 7));
          this.statusFilter.set('');
          this.page.set(0);
          this.closeForms();
          this.toast.show({
            tone: 'success',
            title: request.installmentCount === 1 ? 'Boleto cadastrado' : 'Boletos cadastrados',
            message: `${request.installmentCount} boleto(s) salvo(s) com seus vencimentos.`,
          });
          this.loadData();
        },
        error: (error: unknown) => this.handleError(error),
      });
  }

  update(request: UpdateBillRequest): void {
    const bill = this.editingBill();
    if (!bill || this.busy() || this.conflict()) return;
    this.busy.set(true);
    this.mutationError.set('');
    this.api
      .update(bill.id, request)
      .pipe(
        finalize(() => this.busy.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => {
          this.period.set(request.dueDate.slice(0, 7));
          this.page.set(0);
          this.closeForms();
          this.toast.show({
            tone: 'success',
            title: 'Boleto atualizado',
            message: 'As alterações desta parcela foram salvas.',
          });
          this.loadData();
        },
        error: (error: unknown) => this.handleError(error),
      });
  }

  openPayment(bill: Bill): void {
    if (this.busy() || bill.status !== 'PENDING') return;
    this.closeForms();
    this.paymentBill.set(bill);
    this.paymentForm.reset({
      sourceAccountId: this.activeAccounts()[0]?.id ?? '',
      paymentDate: this.today,
    });
    this.focusControl('bill-payment-account');
  }

  pay(): void {
    const bill = this.paymentBill();
    const request = this.paymentForm.getRawValue();
    if (!bill || this.busy() || this.conflict()) return;
    if (
      this.paymentForm.invalid ||
      request.paymentDate > this.today ||
      !this.activeAccounts().some((account) => account.id === request.sourceAccountId)
    ) {
      this.paymentForm.markAllAsTouched();
      this.mutationError.set('Selecione uma conta ativa e uma data de pagamento até hoje.');
      return;
    }
    const account = this.activeAccounts().find((item) => item.id === request.sourceAccountId)!;
    this.confirmOperation({
      title: 'Registrar pagamento do boleto?',
      message: `${bill.description}: ${this.money(bill.amount)} será debitado de ${account.name}, com pagamento em ${this.formatDate(request.paymentDate)}.`,
      confirmLabel: 'Confirmar pagamento',
      operation: () => this.api.pay(bill.id, { ...request, expectedVersion: bill.version }),
      successTitle: 'Boleto pago',
      successMessage: 'O pagamento foi registrado e o saldo da conta foi atualizado.',
    });
  }

  cancelBill(bill: Bill): void {
    if (this.busy() || this.conflict() || bill.status !== 'PENDING') return;
    this.confirmOperation({
      title: 'Cancelar este boleto?',
      message: `${bill.description} — parcela ${bill.installmentNumber} de ${bill.installmentCount}, no valor de ${this.money(bill.amount)}. Os outros boletos permanecerão como estão.`,
      confirmLabel: 'Cancelar boleto',
      operation: () => this.api.cancel(bill.id, bill.version),
      successTitle: 'Boleto cancelado',
      successMessage: 'O boleto foi mantido no histórico como cancelado.',
    });
  }

  reloadAfterConflict(): void {
    if (this.busy()) return;
    this.closeForms();
    this.loadData();
  }

  closeForms(): void {
    this.formOpen.set(false);
    this.editingBill.set(null);
    this.paymentBill.set(null);
    this.mutationError.set('');
    this.conflict.set(false);
  }

  categoryName(id: string): string {
    return this.categories().find((category) => category.id === id)?.name ?? 'Categoria';
  }

  formatDate(date: string): string {
    return date.split('-').reverse().join('/');
  }
  money(amount: number): string {
    return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(amount);
  }
  isOverdue(bill: Bill): boolean {
    return bill.status === 'PENDING' && bill.dueDate < this.today;
  }
  statusLabel(bill: Bill): string {
    if (this.isOverdue(bill)) return 'Vencido';
    return { PENDING: 'A pagar', PAID: 'Pago', CANCELLED: 'Cancelado' }[bill.status];
  }
  statusTone(bill: Bill): FeedbackTone {
    if (this.isOverdue(bill)) return 'danger';
    return ({ PENDING: 'warning', PAID: 'success', CANCELLED: 'neutral' } as const)[bill.status];
  }

  private confirmOperation(config: {
    title: string;
    message: string;
    confirmLabel: string;
    operation: () => Observable<Bill>;
    successTitle: string;
    successMessage: string;
  }): void {
    this.busy.set(true);
    this.mutationError.set('');
    this.dialog
      .confirm({
        title: config.title,
        message: config.message,
        confirmLabel: config.confirmLabel,
        cancelLabel: 'Voltar',
      })
      .pipe(
        take(1),
        switchMap((confirmed) => (confirmed ? config.operation() : EMPTY)),
        finalize(() => this.busy.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => {
          this.closeForms();
          this.toast.show({
            tone: 'success',
            title: config.successTitle,
            message: config.successMessage,
          });
          this.loadData();
        },
        error: (error: unknown) => this.handleError(error),
      });
  }

  private handleError(error: unknown): void {
    this.conflict.set(error instanceof ApiRequestError && error.status === 409);
    this.mutationError.set(
      error instanceof ApiRequestError
        ? error.message
        : 'Não foi possível salvar. Tente novamente.',
    );
  }

  private localDate(date: Date): string {
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
  }

  private focusControl(id: string): void {
    afterNextRender(() => this.document.getElementById(id)?.focus(), { injector: this.injector });
  }
}
