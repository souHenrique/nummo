import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';
import { AppDialogService } from '../../../core/feedback/dialog/dialog.service';
import { ToastService } from '../../../core/feedback/toast/toast.service';
import { ApiRequestError } from '../../../core/http/api-request-error';
import { AccountApiService } from '../../accounts/data-access/account-api.service';
import { Account } from '../../accounts/models/account.models';
import { CategoryApiService } from '../../categories/data-access/category-api.service';
import { Category } from '../../categories/models/category.models';
import { BillApiService } from '../data-access/bill-api.service';
import { Bill, CreateBillRequest } from '../models/bill.models';
import { BillListPage } from './bill-list-page';

describe('BillListPage', () => {
  let fixture: ComponentFixture<BillListPage>;
  let component: BillListPage;
  let api: {
    list: ReturnType<typeof vi.fn>;
    create: ReturnType<typeof vi.fn>;
    update: ReturnType<typeof vi.fn>;
    pay: ReturnType<typeof vi.fn>;
    cancel: ReturnType<typeof vi.fn>;
  };
  let dialog: { confirm: ReturnType<typeof vi.fn> };
  let toast: { show: ReturnType<typeof vi.fn> };
  const category: Category = {
    id: 'transport',
    name: 'Transporte',
    type: 'EXPENSE',
    status: 'ACTIVE',
    parentCategoryId: null,
    createdAt: '',
    updatedAt: '',
  };
  const account: Account = {
    id: 'account-jesse',
    name: 'Conta de Jesse',
    status: 'ACTIVE',
    type: 'CHECKING',
    institution: null,
    initialBalance: 1000,
    currentBalance: 1000,
    version: 0,
    createdAt: '',
    updatedAt: '',
  };
  const bill: Bill = {
    id: 'bill-jesse',
    description: 'Moto de Jesse',
    amount: 100.25,
    dueDate: '2026-09-30',
    categoryId: category.id,
    seriesId: 'series-jesse',
    installmentCount: 36,
    installmentNumber: 1,
    status: 'PENDING',
    version: 0,
    paymentTransactionId: null,
  };
  const payload: CreateBillRequest = {
    description: bill.description,
    amount: bill.amount,
    firstDueDate: bill.dueDate,
    categoryId: category.id,
    installmentCount: 36,
  };

  beforeEach(async () => {
    api = {
      list: vi
        .fn()
        .mockReturnValue(
          of({ content: [bill], number: 0, size: 20, totalElements: 21, totalPages: 2 }),
        ),
      create: vi.fn().mockReturnValue(of([bill])),
      update: vi.fn().mockReturnValue(of(bill)),
      pay: vi.fn().mockReturnValue(of({ ...bill, status: 'PAID' })),
      cancel: vi.fn().mockReturnValue(of({ ...bill, status: 'CANCELLED' })),
    };
    dialog = { confirm: vi.fn().mockReturnValue(of(false)) };
    toast = { show: vi.fn() };
    await TestBed.configureTestingModule({
      imports: [BillListPage],
      providers: [
        provideRouter([]),
        { provide: BillApiService, useValue: api },
        {
          provide: AccountApiService,
          useValue: {
            findAll: vi
              .fn()
              .mockReturnValue(
                of([
                  account,
                  { ...account, id: 'inactive', name: 'Conta de Walter', status: 'INACTIVE' },
                ]),
              ),
          },
        },
        {
          provide: CategoryApiService,
          useValue: { findAll: vi.fn().mockReturnValue(of([category])) },
        },
        { provide: AppDialogService, useValue: dialog },
        { provide: ToastService, useValue: toast },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(BillListPage);
    component = fixture.componentInstance;
    component.period.set('2026-09');
    fixture.detectChanges();
  });

  it('renders separate installments with due date, category and status', () => {
    expect(api.list).toHaveBeenCalledWith(2026, 9, '', 0);
    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Moto de Jesse');
    expect(text).toContain('30/09/2026');
    expect(text).toContain('1 de 36');
    expect(text).toContain('Transporte');
  });

  it('preserves month and status during pagination and resets page on filter change', () => {
    component.selectStatus('PENDING');
    component.selectPage(2);
    expect(api.list).toHaveBeenLastCalledWith(2026, 9, 'PENDING', 1);
    component.selectPeriod('2026-10');
    expect(api.list).toHaveBeenLastCalledWith(2026, 10, 'PENDING', 0);
  });

  it('renders loading, empty and retryable error states', () => {
    api.list.mockReturnValueOnce(new Subject());
    component.loadData();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('app-skeleton')).not.toBeNull();
    api.list.mockReturnValueOnce(throwError(() => new Error('offline')));
    component.loadData();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('app-error-state')).not.toBeNull();
    api.list.mockReturnValueOnce(
      of({ content: [], number: 0, size: 20, totalElements: 0, totalPages: 0 }),
    );
    component.loadData();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('app-empty-state')).not.toBeNull();
  });

  it('creates bills, selects their due month and shows success feedback', () => {
    component.openForm();
    component.create({ ...payload, firstDueDate: '2026-10-15' });
    expect(api.create).toHaveBeenCalledWith({ ...payload, firstDueDate: '2026-10-15' });
    expect(component.period()).toBe('2026-10');
    expect(component.formOpen()).toBe(false);
    expect(toast.show).toHaveBeenCalledWith(expect.objectContaining({ tone: 'success' }));
  });

  it('blocks double submission and preserves input after a failed create', () => {
    const request = new Subject<Bill[]>();
    api.create.mockReturnValue(request);
    component.openForm();
    component.create(payload);
    component.create(payload);
    expect(api.create).toHaveBeenCalledOnce();
    expect(component.busy()).toBe(true);
    request.error(new Error('offline'));
    expect(component.formOpen()).toBe(true);
    expect(component.busy()).toBe(false);
    expect(component.mutationError()).toBeTruthy();
  });

  it('shows only active accounts in the payment form', () => {
    component.openPayment(bill);
    fixture.detectChanges();
    const select = fixture.nativeElement.querySelector(
      '#bill-payment-account',
    ) as HTMLSelectElement;
    expect(select.textContent).toContain('Conta de Jesse');
    expect(select.textContent).not.toContain('Conta de Walter');
  });

  it('requires a valid account and date before asking for payment confirmation', () => {
    component.openPayment(bill);
    component.paymentForm.controls.sourceAccountId.setValue('');
    component.pay();
    expect(dialog.confirm).not.toHaveBeenCalled();
    component.paymentForm.patchValue({ sourceAccountId: account.id, paymentDate: '9999-12-31' });
    component.pay();
    expect(dialog.confirm).not.toHaveBeenCalled();
    expect(api.pay).not.toHaveBeenCalled();
  });

  it('shows account, amount and date in confirmation and does not pay when declined', () => {
    component.openPayment(bill);
    component.paymentForm.controls.paymentDate.setValue('2026-09-29');
    component.pay();
    expect(dialog.confirm).toHaveBeenCalledWith(
      expect.objectContaining({ message: expect.stringContaining('Conta de Jesse') }),
    );
    expect(dialog.confirm.mock.calls[0][0].message).toContain('100,25');
    expect(dialog.confirm.mock.calls[0][0].message).toContain('29/09/2026');
    expect(api.pay).not.toHaveBeenCalled();
    expect(component.busy()).toBe(false);
  });

  it('pays once after confirmation and reloads accounts and bills', () => {
    const confirmation = new Subject<boolean>();
    dialog.confirm.mockReturnValue(confirmation);
    component.openPayment(bill);
    component.paymentForm.controls.paymentDate.setValue('2026-09-29');
    component.pay();
    component.pay();
    expect(dialog.confirm).toHaveBeenCalledOnce();
    confirmation.next(true);
    expect(api.pay).toHaveBeenCalledExactlyOnceWith(bill.id, {
      sourceAccountId: account.id,
      paymentDate: '2026-09-29',
      expectedVersion: 0,
    });
    expect(api.list).toHaveBeenCalledTimes(2);
    expect(TestBed.inject(AccountApiService).findAll).toHaveBeenCalledTimes(2);
    expect(toast.show).toHaveBeenCalledWith(expect.objectContaining({ title: 'Boleto pago' }));
    expect(component.paymentBill()).toBeNull();
  });

  it('retains payment data on conflict and requires reload and a new confirmation', () => {
    dialog.confirm.mockReturnValue(of(true));
    api.pay.mockReturnValue(
      throwError(
        () =>
          new ApiRequestError({
            timestamp: '',
            status: 409,
            code: 'BILL_CONFLICT',
            message: 'Boleto alterado',
            path: '',
            fieldErrors: [],
          }),
      ),
    );
    component.openPayment(bill);
    component.pay();
    expect(component.conflict()).toBe(true);
    expect(component.paymentBill()).toEqual(bill);
    component.pay();
    expect(api.pay).toHaveBeenCalledOnce();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Recarregar boletos');
    component.reloadAfterConflict();
    expect(component.conflict()).toBe(false);
    expect(component.paymentBill()).toBeNull();
    expect(api.list).toHaveBeenCalledTimes(2);
  });

  it('edits only the selected bill with expected version', () => {
    component.openForm(bill);
    const request = {
      description: bill.description,
      amount: 120,
      dueDate: bill.dueDate,
      categoryId: category.id,
      expectedVersion: bill.version,
    };
    component.update(request);
    expect(api.update).toHaveBeenCalledWith(bill.id, request);
    expect(component.formOpen()).toBe(false);
  });

  it('requires confirmation before cancelling only the selected installment', () => {
    component.cancelBill(bill);
    expect(api.cancel).not.toHaveBeenCalled();
    dialog.confirm.mockReturnValue(of(true));
    component.cancelBill(bill);
    expect(api.cancel).toHaveBeenCalledWith(bill.id, bill.version);
    expect(toast.show).toHaveBeenCalledWith(expect.objectContaining({ title: 'Boleto cancelado' }));
  });

  it('keeps paid and cancelled bills visible without payment or editing actions', () => {
    component.result.set({
      content: [
        { ...bill, status: 'PAID', paymentTransactionId: 'payment-jesse' },
        { ...bill, id: 'bill-walter', status: 'CANCELLED' },
      ],
      number: 0,
      size: 20,
      totalElements: 2,
      totalPages: 1,
    });
    fixture.detectChanges();
    const grid = fixture.nativeElement.querySelector('.bills__grid') as HTMLElement;
    expect(grid.textContent).toContain('Pago');
    expect(grid.textContent).toContain('Cancelado');
    expect(grid.querySelector('button')).toBeNull();
    expect(grid.querySelector('a')?.getAttribute('href')).toBe('/transactions/payment-jesse');
    component.openPayment({ ...bill, status: 'PAID' });
    expect(component.paymentBill()).toBeNull();
  });

  it('identifies an overdue pending bill in text as well as color', () => {
    expect(component.statusLabel({ ...bill, dueDate: '2000-01-01' })).toBe('Vencido');
    expect(component.statusTone({ ...bill, dueDate: '2000-01-01' })).toBe('danger');
    expect(component.statusLabel({ ...bill, status: 'PAID', dueDate: '2000-01-01' })).toBe('Pago');
  });
});
