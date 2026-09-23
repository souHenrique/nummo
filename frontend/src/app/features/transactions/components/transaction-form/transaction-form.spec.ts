import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Account } from '../../../accounts/models/account.models';
import { Category } from '../../../categories/models/category.models';
import { Transaction } from '../../models/transaction.models';
import { TransactionFormComponent } from './transaction-form';

describe('TransactionFormComponent', () => {
  let fixture: ComponentFixture<TransactionFormComponent>;
  let component: TransactionFormComponent;

  const activeAccount: Account = {
    id: '1f2e3d4c-5b6a-7980-1234-56789abcdef0',
    name: 'Conta Walter',
    type: 'CHECKING',
    institution: 'Banco Albuquerque',
    initialBalance: 1000,
    currentBalance: 1200,
    status: 'ACTIVE',
    version: 1,
    createdAt: '2026-09-15T10:00:00Z',
    updatedAt: '2026-09-15T10:00:00Z',
  };
  const inactiveAccount: Account = {
    ...activeAccount,
    id: '2f2e3d4c-5b6a-7980-1234-56789abcdef0',
    name: 'Conta Gus',
    status: 'INACTIVE',
  };
  const incomeCategory: Category = {
    id: '3f2e3d4c-5b6a-7980-1234-56789abcdef0',
    name: 'Saul Goodman',
    type: 'INCOME',
    parentCategoryId: null,
    status: 'ACTIVE',
    createdAt: '2026-09-15T10:00:00Z',
    updatedAt: '2026-09-15T10:00:00Z',
  };
  const expenseCategory: Category = {
    id: '4f2e3d4c-5b6a-7980-1234-56789abcdef0',
    name: 'Jesse Pinkman',
    type: 'EXPENSE',
    parentCategoryId: null,
    status: 'ACTIVE',
    createdAt: '2026-09-15T10:00:00Z',
    updatedAt: '2026-09-15T10:00:00Z',
  };
  const expenseTransaction: Transaction = {
    id: '5f2e3d4c-5b6a-7980-1234-56789abcdef0',
    description: 'Mercado do Jesse Pinkman',
    amount: 150,
    competenceDate: '2026-09-10',
    effectiveDate: '2026-09-10',
    dueDate: null,
    type: 'EXPENSE',
    status: 'COMPLETED',
    paymentMethod: 'PIX',
    sourceAccountId: activeAccount.id,
    destinationAccountId: null,
    categoryId: expenseCategory.id,
    creditCardId: null,
    invoiceId: null,
    installmentGroupId: null,
    installmentNumber: null,
    installmentCount: null,
    createdAt: '2026-09-10T10:00:00Z',
    updatedAt: '2026-09-10T10:00:00Z',
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TransactionFormComponent],
    }).compileComponents();
  });

  function createComponent(mode: 'create' | 'edit', transaction: Transaction | null = null): void {
    fixture = TestBed.createComponent(TransactionFormComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('mode', mode);
    fixture.componentRef.setInput('transaction', transaction);
    fixture.componentRef.setInput('accounts', [activeAccount, inactiveAccount]);
    fixture.componentRef.setInput('categories', [incomeCategory, expenseCategory]);
    fixture.detectChanges();
  }

  function fillValidCreateForm(type: 'INCOME' | 'EXPENSE'): void {
    component.form.controls.type.setValue(type);
    component.onTypeChange();
    component.form.patchValue({
      description: 'Lançamento de Walter White',
      amount: 250,
      competenceDate: '2026-09-15',
      status: 'COMPLETED',
      effectiveDate: '2026-09-15',
      paymentMethod: 'PIX',
      categoryId: type === 'INCOME' ? incomeCategory.id : expenseCategory.id,
    });
    fixture.detectChanges();

    if (type === 'INCOME') component.form.controls.destinationAccountId.setValue(activeAccount.id);
    else component.form.controls.sourceAccountId.setValue(activeAccount.id);
  }

  it('emits destinationAccountId for an income', () => {
    createComponent('create');
    const submitted = vi.fn();
    component.submitted.subscribe(submitted);
    fillValidCreateForm('INCOME');
    component.submit();

    expect(fixture.nativeElement.querySelector('#destination-account')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('#source-account')).toBeNull();
    expect(submitted).toHaveBeenCalledWith(
      expect.objectContaining({
        type: 'INCOME',
        destinationAccountId: activeAccount.id,
        sourceAccountId: null,
      }),
    );
  });

  it('emits sourceAccountId for an expense', () => {
    createComponent('create');
    const submitted = vi.fn();
    component.submitted.subscribe(submitted);
    fillValidCreateForm('EXPENSE');
    component.submit();

    expect(fixture.nativeElement.querySelector('#source-account')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('#destination-account')).toBeNull();
    expect(submitted).toHaveBeenCalledWith(
      expect.objectContaining({
        type: 'EXPENSE',
        sourceAccountId: activeAccount.id,
        destinationAccountId: null,
      }),
    );
  });

  it('changes visible categories according to selected type', () => {
    createComponent('create');
    component.form.controls.type.setValue('INCOME');
    component.onTypeChange();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('#transaction-category').textContent).toContain(
      incomeCategory.name,
    );
    expect(fixture.nativeElement.querySelector('#transaction-category').textContent).not.toContain(
      expenseCategory.name,
    );

    component.form.controls.type.setValue('EXPENSE');
    component.onTypeChange();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('#transaction-category').textContent).toContain(
      expenseCategory.name,
    );
    expect(fixture.nativeElement.querySelector('#transaction-category').textContent).not.toContain(
      incomeCategory.name,
    );
  });

  it('does not render inactive accounts', () => {
    createComponent('create');
    component.form.controls.type.setValue('EXPENSE');
    component.onTypeChange();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain(activeAccount.name);
    expect(fixture.nativeElement.textContent).not.toContain(inactiveAccount.name);
  });

  it('requires effectiveDate for a completed transaction', () => {
    createComponent('create');
    const submitted = vi.fn();
    component.submitted.subscribe(submitted);
    fillValidCreateForm('EXPENSE');
    component.form.controls.effectiveDate.setValue('');
    component.onStatusChange();
    component.submit();

    expect(component.form.controls.effectiveDate.hasError('required')).toBe(true);
    expect(submitted).not.toHaveBeenCalled();
  });

  it('clears and omits effectiveDate for a pending transaction', () => {
    createComponent('create');
    const submitted = vi.fn();
    component.submitted.subscribe(submitted);
    fillValidCreateForm('EXPENSE');
    component.form.controls.status.setValue('PENDING');
    component.onStatusChange();
    component.submit();

    expect(component.form.controls.effectiveDate.value).toBe('');
    expect(submitted).toHaveBeenCalledWith(
      expect.objectContaining({ status: 'PENDING', effectiveDate: null }),
    );
  });

  it.each([0, -10])('does not submit invalid amount %s', (amount) => {
    createComponent('create');
    const submitted = vi.fn();
    component.submitted.subscribe(submitted);
    fillValidCreateForm('EXPENSE');
    component.form.controls.amount.setValue(amount);
    component.submit();

    expect(submitted).not.toHaveBeenCalled();
  });

  it('hides type selection and loads fields in edit mode', () => {
    createComponent('edit', expenseTransaction);

    expect(fixture.nativeElement.querySelector('#transaction-type')).toBeNull();
    expect(fixture.nativeElement.querySelector('#transaction-type-readonly')).not.toBeNull();
    expect(component.form.controls.description.value).toBe(expenseTransaction.description);
    expect(component.form.controls.sourceAccountId.value).toBe(activeAccount.id);
  });

  it('emits only changed fields in edit mode', () => {
    createComponent('edit', expenseTransaction);
    const submitted = vi.fn();
    component.submitted.subscribe(submitted);
    component.form.controls.description.setValue('Mercado do Walter White');
    component.submit();

    expect(submitted).toHaveBeenCalledWith({ description: 'Mercado do Walter White' });
  });

  it('allows a card purchase to update only its description and category', () => {
    const purchase: Transaction = {
      ...expenseTransaction,
      type: 'CREDIT_CARD_PURCHASE',
      paymentMethod: 'CREDIT_CARD',
      creditCardId: '6f2e3d4c-5b6a-7980-1234-56789abcdef0',
      installmentGroupId: '7f2e3d4c-5b6a-7980-1234-56789abcdef0',
      installmentNumber: 1,
      installmentCount: 3,
    };
    createComponent('edit', purchase);
    const submitted = vi.fn();
    component.submitted.subscribe(submitted);
    component.form.controls.description.setValue('Compra do Walter White');
    component.submit();

    expect(fixture.nativeElement.querySelector('#transaction-amount')).toBeNull();
    expect(fixture.nativeElement.querySelector('#competence-date')).toBeNull();
    expect(fixture.nativeElement.querySelector('#transaction-category')).not.toBeNull();
    expect(submitted).toHaveBeenCalledWith({ description: 'Compra do Walter White' });
  });

  it('disables actions while submitting', () => {
    createComponent('create');
    fixture.componentRef.setInput('submitting', true);
    fixture.detectChanges();
    const buttons = fixture.nativeElement.querySelectorAll('app-button button');

    expect(buttons).toHaveLength(2);
    expect(buttons[0].disabled).toBe(true);
    expect(buttons[1].disabled).toBe(true);
  });

  it('emits cancelled when the user presses cancel', () => {
    createComponent('create');
    const cancelled = vi.fn();
    component.cancelled.subscribe(cancelled);
    (fixture.nativeElement.querySelector('app-button button') as HTMLButtonElement).click();

    expect(cancelled).toHaveBeenCalledOnce();
  });
});
