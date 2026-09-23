import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { Subject, of } from 'rxjs';

import { ToastService } from '../../../../core/feedback/toast/toast.service';
import { AccountApiService } from '../../../accounts/data-access/account-api.service';
import { Account } from '../../../accounts/models/account.models';
import { CategoryApiService } from '../../../categories/data-access/category-api.service';
import { Category } from '../../../categories/models/category.models';
import { CreditCardApiService } from '../../../credit-cards/data-access/credit-card-api.service';
import { TransactionFormComponent } from '../../components/transaction-form/transaction-form';
import { TransactionApiService } from '../../data-access/transaction-api.service';
import { Transaction, UpdateTransactionRequest } from '../../models/transaction.models';
import { TransactionEditPage } from './transaction-edit-page';

describe('TransactionEditPage', () => {
  let fixture: ComponentFixture<TransactionEditPage>;
  let component: TransactionEditPage;
  let router: Router;
  let toast: { show: ReturnType<typeof vi.fn> };
  let transactionApi: { findById: ReturnType<typeof vi.fn>; update: ReturnType<typeof vi.fn> };

  const account: Account = {
    id: 'bf2e3d4c-5b6a-7980-1234-56789abcdef0',
    name: 'Conta Walter',
    type: 'CHECKING',
    institution: 'Banco Albuquerque',
    initialBalance: 1000,
    currentBalance: 1000,
    status: 'ACTIVE',
    version: 1,
    createdAt: '2026-09-15T10:00:00Z',
    updatedAt: '2026-09-15T10:00:00Z',
  };
  const category: Category = {
    id: 'cf2e3d4c-5b6a-7980-1234-56789abcdef0',
    name: 'Jesse Pinkman',
    type: 'EXPENSE',
    parentCategoryId: null,
    status: 'ACTIVE',
    createdAt: '2026-09-15T10:00:00Z',
    updatedAt: '2026-09-15T10:00:00Z',
  };
  const transaction: Transaction = {
    id: 'df2e3d4c-5b6a-7980-1234-56789abcdef0',
    description: 'Compra no mercado',
    amount: 150,
    competenceDate: '2026-09-15',
    effectiveDate: '2026-09-15',
    dueDate: null,
    type: 'EXPENSE',
    status: 'COMPLETED',
    paymentMethod: 'PIX',
    sourceAccountId: account.id,
    destinationAccountId: null,
    categoryId: category.id,
    creditCardId: null,
    invoiceId: null,
    installmentGroupId: null,
    installmentNumber: null,
    installmentCount: null,
    createdAt: '2026-09-15T10:00:00Z',
    updatedAt: '2026-09-15T10:00:00Z',
  };

  beforeEach(async () => {
    transactionApi = {
      findById: vi.fn().mockReturnValue(of(transaction)),
      update: vi
        .fn()
        .mockReturnValue(of({ ...transaction, description: 'Mercado do Walter White' })),
    };
    toast = { show: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [TransactionEditPage],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: convertToParamMap({ id: transaction.id }) } },
        },
        { provide: TransactionApiService, useValue: transactionApi },
        {
          provide: CreditCardApiService,
          useValue: { updatePurchase: vi.fn().mockReturnValue(of([])) },
        },
        {
          provide: AccountApiService,
          useValue: { findAll: vi.fn().mockReturnValue(of([account])) },
        },
        {
          provide: CategoryApiService,
          useValue: { findAll: vi.fn().mockReturnValue(of([category])) },
        },
        { provide: ToastService, useValue: toast },
      ],
    }).compileComponents();

    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
  });

  function createPage(): void {
    fixture = TestBed.createComponent(TransactionEditPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  it('loads the transaction from the route id and renders edit form', () => {
    createPage();
    const form = fixture.debugElement.query(By.directive(TransactionFormComponent))
      .componentInstance as TransactionFormComponent;

    expect(transactionApi.findById).toHaveBeenCalledWith(transaction.id);
    expect(form.mode()).toBe('edit');
    expect(form.transaction()).toEqual(transaction);
  });

  it('renders loading and then an error state when loading fails', () => {
    const response = new Subject<Transaction>();
    transactionApi.findById.mockReturnValue(response.asObservable());
    createPage();
    expect(fixture.nativeElement.querySelector('app-skeleton')).not.toBeNull();

    response.error(new Error('network'));
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Não foi possível carregar a transação');
  });

  it('sends only changed fields through PATCH', () => {
    createPage();
    const changes: UpdateTransactionRequest = { description: 'Mercado do Walter White' };

    component.update(changes);

    expect(transactionApi.update).toHaveBeenCalledWith(transaction.id, changes);
  });

  it('does not send PATCH when no changes were supplied', () => {
    createPage();

    component.update({});

    expect(transactionApi.update).not.toHaveBeenCalled();
    expect(toast.show).toHaveBeenCalledWith(expect.objectContaining({ tone: 'info' }));
  });

  it('shows success feedback and navigates after updating', () => {
    createPage();

    component.update({ description: 'Mercado do Walter White' });

    expect(toast.show).toHaveBeenCalledWith(expect.objectContaining({ tone: 'success' }));
    expect(router.navigate).toHaveBeenCalledWith(['/transactions', transaction.id]);
  });

  it('does not render an edit form for a cancelled transaction', () => {
    transactionApi.findById.mockReturnValue(of({ ...transaction, status: 'CANCELLED' }));
    createPage();

    expect(fixture.debugElement.query(By.directive(TransactionFormComponent))).toBeNull();
    expect(fixture.nativeElement.textContent).toContain('Edição indisponível');
  });

  it('renders the edit form for a credit card purchase', () => {
    transactionApi.findById.mockReturnValue(
      of({
        ...transaction,
        type: 'CREDIT_CARD_PURCHASE',
        paymentMethod: 'CREDIT_CARD',
        creditCardId: 'ef2e3d4c-5b6a-7980-1234-56789abcdef0',
      }),
    );
    createPage();

    expect(
      fixture.nativeElement.querySelector('.transaction-edit-page__purchase-form'),
    ).not.toBeNull();
  });
});
