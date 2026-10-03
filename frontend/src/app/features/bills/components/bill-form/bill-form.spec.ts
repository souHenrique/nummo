import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Category } from '../../../categories/models/category.models';
import { Bill } from '../../models/bill.models';
import { BillForm } from './bill-form';

describe('BillForm', () => {
  let fixture: ComponentFixture<BillForm>;
  let component: BillForm;
  const category: Category = {
    id: 'transport',
    name: 'Transporte',
    type: 'EXPENSE',
    status: 'ACTIVE',
    parentCategoryId: null,
    createdAt: '',
    updatedAt: '',
  };
  const bill: Bill = {
    id: 'bill-jesse',
    description: 'Moto de Jesse',
    amount: 100.25,
    dueDate: '2026-10-10',
    categoryId: category.id,
    seriesId: 'series-jesse',
    installmentNumber: 2,
    installmentCount: 36,
    status: 'PENDING',
    paymentTransactionId: null,
    version: 3,
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [BillForm] }).compileComponents();
    fixture = TestBed.createComponent(BillForm);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('categories', [category]);
    fixture.componentRef.setInput('defaultDueDate', '2026-10-01');
    fixture.detectChanges();
    component.form.patchValue({
      description: 'Moto de Jesse',
      amount: 100.25,
      categoryId: category.id,
    });
  });

  it('creates one bill in the chosen month', () => {
    const emitted = vi.spyOn(component.created, 'emit');
    component.submit();
    expect(emitted).toHaveBeenCalledWith({
      description: 'Moto de Jesse',
      amount: 100.25,
      categoryId: category.id,
      firstDueDate: '2026-10-01',
      installmentCount: 1,
    });
  });

  it('sends the amount per installment and the chosen monthly count', () => {
    const emitted = vi.spyOn(component.created, 'emit');
    component.form.patchValue({ recurrence: 'monthly', installmentCount: 36 });
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('#bill-count')).not.toBeNull();
    component.submit();
    expect(emitted).toHaveBeenCalledWith(
      expect.objectContaining({ amount: 100.25, installmentCount: 36 }),
    );
  });

  it('resets count when changing back to a single bill', () => {
    const emitted = vi.spyOn(component.created, 'emit');
    component.form.patchValue({ recurrence: 'single', installmentCount: 36 });
    component.submit();
    expect(emitted).toHaveBeenCalledWith(expect.objectContaining({ installmentCount: 1 }));
  });

  it.each([0, -10, 1.234])('rejects invalid amount %s', (amount) => {
    const emitted = vi.spyOn(component.created, 'emit');
    component.form.controls.amount.setValue(amount);
    component.submit();
    expect(emitted).not.toHaveBeenCalled();
  });

  it.each([0, 601, 1.5])('rejects invalid installment count %s', (installmentCount) => {
    const emitted = vi.spyOn(component.created, 'emit');
    component.form.patchValue({ recurrence: 'monthly', installmentCount });
    component.submit();
    expect(emitted).not.toHaveBeenCalled();
  });

  it('rejects blank description and missing due date', () => {
    const emitted = vi.spyOn(component.created, 'emit');
    component.form.patchValue({ description: '   ', dueDate: '' });
    component.submit();
    expect(emitted).not.toHaveBeenCalled();
    expect(component.fieldError('description')).toBeTruthy();
    expect(component.fieldError('dueDate')).toBeTruthy();
  });

  it('offers only active expense categories and rejects a foreign category selection', () => {
    fixture.componentRef.setInput('categories', [
      category,
      { ...category, id: 'inactive', status: 'INACTIVE' },
      { ...category, id: 'income', type: 'INCOME' },
    ]);
    fixture.detectChanges();
    expect(component.activeCategories()).toEqual([category]);
    const emitted = vi.spyOn(component.created, 'emit');
    component.form.controls.categoryId.setValue('inactive');
    component.submit();
    expect(emitted).not.toHaveBeenCalled();
  });

  it('loads the selected installment and emits an edit with its version', () => {
    fixture.componentRef.setInput('bill', bill);
    fixture.detectChanges();
    expect(component.form.controls.amount.value).toBe(100.25);
    expect(fixture.nativeElement.querySelector('#bill-recurrence')).toBeNull();
    const emitted = vi.spyOn(component.updated, 'emit');
    component.form.controls.amount.setValue(150.5);
    component.submit();
    expect(emitted).toHaveBeenCalledWith({
      description: bill.description,
      amount: 150.5,
      dueDate: bill.dueDate,
      categoryId: category.id,
      expectedVersion: 3,
    });
  });

  it('disables controls and blocks programmatic submission while sending', () => {
    fixture.componentRef.setInput('submitting', true);
    fixture.detectChanges();
    const emitted = vi.spyOn(component.created, 'emit');
    component.submit();
    expect(component.form.disabled).toBe(true);
    expect(emitted).not.toHaveBeenCalled();
    expect(
      [...fixture.nativeElement.querySelectorAll('button')].every(
        (button: HTMLButtonElement) => button.disabled,
      ),
    ).toBe(true);
  });

  it('emits cancellation from the cancel button', () => {
    const emitted = vi.spyOn(component.cancelled, 'emit');
    fixture.nativeElement.querySelector('button').click();
    expect(emitted).toHaveBeenCalledOnce();
  });
});
