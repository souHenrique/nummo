import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap } from '@angular/router';
import { Subject, of, throwError } from 'rxjs';

import { AccountApiService } from '../../../accounts/data-access/account-api.service';
import { Account } from '../../../accounts/models/account.models';
import { CreditCardApiService } from '../../../credit-cards/data-access/credit-card-api.service';
import { CreditCard, CreditCardRefund } from '../../../credit-cards/models/credit-card.models';
import { Transaction } from '../../../transactions/models/transaction.models';
import { AppDialogService } from '../../../../core/feedback/dialog/dialog.service';
import { ToastService } from '../../../../core/feedback/toast/toast.service';
import { ApiRequestError } from '../../../../core/http/api-request-error';
import { InvoiceApiService } from '../../data-access/invoice-api.service';
import { InvoiceDetail } from '../../models/invoice.models';
import { InvoiceDetailPage } from './invoice-detail-page';

describe('InvoiceDetailPage', () => {
  let fixture: ComponentFixture<InvoiceDetailPage>;
  let component: InvoiceDetailPage;
  let accountApi: { findAll: ReturnType<typeof vi.fn> };
  let creditCardApi: {
    findById: ReturnType<typeof vi.fn>;
    refundPurchase: ReturnType<typeof vi.fn>;
  };
  let dialog: { confirm: ReturnType<typeof vi.fn> };
  let invoiceApi: {
    findById: ReturnType<typeof vi.fn>;
    close: ReturnType<typeof vi.fn>;
    reopen: ReturnType<typeof vi.fn>;
    pay: ReturnType<typeof vi.fn>;
  };
  let router: { navigate: ReturnType<typeof vi.fn> };
  let toast: { show: ReturnType<typeof vi.fn> };

  const account: Account = {
    id: '0f6d7313-77f8-4b48-a63d-5338dd95461e',
    name: 'Conta Walter',
    type: 'CHECKING',
    institution: 'Banco Albuquerque',
    initialBalance: 1500,
    currentBalance: 1800,
    status: 'ACTIVE',
    version: 1,
    createdAt: '2026-09-16T10:00:00Z',
    updatedAt: '2026-09-16T10:00:00Z',
  };

  const inactiveAccount: Account = {
    ...account,
    id: 'a63330b4-5742-4e7e-9e4f-547b4df7246d',
    name: 'Conta Gus',
    status: 'INACTIVE',
  };

  const creditCard: CreditCard = {
    id: 'd89835ee-3463-4a35-a2e9-38d96ab17418',
    name: 'Cartão Heisenberg',
    creditLimit: 5000,
    availableLimit: 3200,
    closingDay: 10,
    dueDay: 17,
    defaultAccountId: account.id,
    status: 'ACTIVE',
    version: 2,
  };

  const purchase: Transaction = {
    id: '3e207b3a-769a-42c6-bffc-2989bb091212',
    description: 'Mercado do Jesse',
    amount: 116.96,
    competenceDate: '2026-09-16',
    effectiveDate: null,
    dueDate: '2026-10-17',
    type: 'CREDIT_CARD_PURCHASE',
    status: 'COMPLETED',
    paymentMethod: 'CREDIT_CARD',
    sourceAccountId: null,
    destinationAccountId: null,
    categoryId: '4e207b3a-769a-42c6-bffc-2989bb091212',
    creditCardId: creditCard.id,
    invoiceId: '72486234-ef50-4c7e-99a7-9193a28533a8',
    installmentGroupId: '5e207b3a-769a-42c6-bffc-2989bb091212',
    installmentNumber: 2,
    installmentCount: 3,
    createdAt: '2026-09-16T10:00:00Z',
    updatedAt: '2026-09-16T10:00:00Z',
  };

  const invoice: InvoiceDetail = {
    id: '72486234-ef50-4c7e-99a7-9193a28533a8',
    creditCardId: creditCard.id,
    referenceMonth: 9,
    referenceYear: 2026,
    closingDate: '2026-09-20',
    dueDate: '2026-09-28',
    totalAmount: 850.75,
    status: 'OPEN',
    paidAt: null,
    version: 4,
    transactions: [purchase],
    creditAppliedAmount: 0,
  };

  const refund: CreditCardRefund = {
    id: 'f2031850-66b4-4cd9-924d-340765bb0e76',
    creditCardId: creditCard.id,
    selectedTransactionId: purchase.id,
    installmentGroupId: purchase.installmentGroupId,
    reason: 'Produto devolvido ao estabelecimento',
    totalAmount: purchase.amount,
    limitRestoredAmount: purchase.amount,
    paidCompensationAmount: 0,
    createdAt: '2026-09-17T11:00:00Z',
    items: [],
  };

  beforeEach(async () => {
    accountApi = {
      findAll: vi.fn().mockReturnValue(of([account, inactiveAccount])),
    };
    creditCardApi = {
      findById: vi.fn().mockReturnValue(of(creditCard)),
      refundPurchase: vi.fn().mockReturnValue(of(refund)),
    };
    dialog = { confirm: vi.fn().mockReturnValue(of(true)) };
    invoiceApi = {
      findById: vi.fn().mockReturnValue(of(invoice)),
      close: vi.fn().mockReturnValue(of({ ...invoice, status: 'CLOSED', version: 5 })),
      reopen: vi.fn().mockReturnValue(of({ ...invoice, status: 'OPEN', version: 6 })),
      pay: vi.fn().mockReturnValue(
        of({
          invoiceId: invoice.id,
          totalAmount: invoice.totalAmount,
          creditAppliedAmount: 0,
          cashPaidAmount: invoice.totalAmount,
          paymentTransactionId: '6e207b3a-769a-42c6-bffc-2989bb091212',
          paidAt: '2026-09-28T14:30:00Z',
        }),
      ),
    };
    router = { navigate: vi.fn().mockResolvedValue(true) };
    toast = { show: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [InvoiceDetailPage],
      providers: [
        { provide: AccountApiService, useValue: accountApi },
        { provide: CreditCardApiService, useValue: creditCardApi },
        { provide: AppDialogService, useValue: dialog },
        { provide: InvoiceApiService, useValue: invoiceApi },
        { provide: Router, useValue: router },
        { provide: ToastService, useValue: toast },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: convertToParamMap({ id: invoice.id }),
            },
          },
        },
      ],
    }).compileComponents();
  });

  function createPage(): void {
    fixture = TestBed.createComponent(InvoiceDetailPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  it('should load invoice, card, purchases, installments and version', () => {
    createPage();

    const content = fixture.nativeElement.textContent as string;

    expect(invoiceApi.findById).toHaveBeenCalledWith(invoice.id);
    expect(creditCardApi.findById).toHaveBeenCalledWith(creditCard.id);
    expect(accountApi.findAll).toHaveBeenCalledOnce();
    expect(content).toContain('Cartão Heisenberg');
    expect(content).toContain('09/2026');
    expect(content).not.toContain('Versão');
    expect(content).toContain('Mercado do Jesse');
    expect(content).toContain('Parcela 2 de 3');
    expect(content).toContain('Fechar fatura');
    expect(content).toContain('Estornar compra');
  });

  it('should only offer a refund for an eligible credit card purchase', () => {
    createPage();

    expect(component.isRefundEligible(purchase)).toBe(true);

    component.invoice.set({
      ...invoice,
      transactions: [{ ...purchase, status: 'CANCELLED' }],
    });
    fixture.detectChanges();

    expect(component.isRefundEligible({ ...purchase, status: 'CANCELLED' })).toBe(false);
    expect(fixture.nativeElement.textContent).not.toContain('Estornar compra');
  });

  it('should require a non-blank reason with at most 500 characters before refund confirmation', () => {
    createPage();
    component.openRefundForm(purchase);
    fixture.detectChanges();

    const reasonField = fixture.nativeElement.querySelector(
      '#refund-reason',
    ) as HTMLTextAreaElement;

    expect(reasonField.maxLength).toBe(500);

    component.refundForm.controls.reason.setValue('   ');
    component.submitRefund();
    fixture.detectChanges();

    expect(creditCardApi.refundPurchase).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('Informe o motivo do estorno.');

    reasonField.value = 'a'.repeat(501);
    reasonField.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    expect(component.refundForm.controls.reason.value).toHaveLength(501);
    expect(component.refundForm.controls.reason.hasError('required')).toBe(false);
    expect(component.refundForm.controls.reason.hasError('maxlength')).toBe(true);

    component.submitRefund();
    fixture.detectChanges();

    expect(creditCardApi.refundPurchase).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain(
      'O motivo deve ter no máximo 500 caracteres.',
    );
  });

  it('should not refund a purchase when confirmation is cancelled', () => {
    dialog.confirm.mockReturnValue(of(false));
    createPage();
    component.openRefundForm(purchase);
    component.refundForm.controls.reason.setValue('Produto devolvido ao estabelecimento');

    component.submitRefund();

    expect(dialog.confirm).toHaveBeenCalledOnce();
    expect(creditCardApi.refundPurchase).not.toHaveBeenCalled();
  });

  it('should refund a purchase and reload the card, invoice and transactions', () => {
    createPage();
    component.openRefundForm(purchase);
    component.refundForm.controls.reason.setValue('  Produto devolvido ao estabelecimento  ');

    component.submitRefund();

    expect(creditCardApi.refundPurchase).toHaveBeenCalledWith(creditCard.id, purchase.id, {
      reason: refund.reason,
    });
    expect(toast.show).toHaveBeenCalledWith({
      tone: 'success',
      title: 'Estorno registrado',
      message: 'O cartão, as faturas e as transações foram atualizados.',
    });
    expect(invoiceApi.findById).toHaveBeenCalledTimes(2);
    expect(creditCardApi.findById).toHaveBeenCalledTimes(2);
    expect(accountApi.findAll).toHaveBeenCalledTimes(2);
  });

  it('should prevent a second refund while the confirmation or request is pending', () => {
    const confirmation = new Subject<boolean>();
    const refundResponse = new Subject<CreditCardRefund>();
    dialog.confirm.mockReturnValue(confirmation.asObservable());
    creditCardApi.refundPurchase.mockReturnValue(refundResponse.asObservable());
    createPage();
    component.openRefundForm(purchase);
    component.refundForm.controls.reason.setValue(refund.reason);

    component.submitRefund();
    component.submitRefund();

    expect(dialog.confirm).toHaveBeenCalledOnce();
    expect(component.isProcessing()).toBe(true);

    confirmation.next(true);
    component.submitRefund();

    expect(creditCardApi.refundPurchase).toHaveBeenCalledOnce();
  });

  it('should render loading while the invoice is pending', () => {
    const invoices = new Subject<InvoiceDetail>();
    invoiceApi.findById.mockReturnValue(invoices.asObservable());

    createPage();

    expect(fixture.nativeElement.querySelector('app-skeleton')).not.toBeNull();

    invoices.next(invoice);
    invoices.complete();
  });

  it('should show an error and retry loading the invoice', () => {
    invoiceApi.findById
      .mockReturnValueOnce(throwError(() => new Error('network')))
      .mockReturnValueOnce(of(invoice));

    createPage();

    expect(fixture.nativeElement.textContent).toContain('Não foi possível carregar a fatura');

    (fixture.nativeElement.querySelector('.error-state button') as HTMLButtonElement).click();
    fixture.detectChanges();

    expect(invoiceApi.findById).toHaveBeenCalledTimes(2);
    expect(component.state()).toBe('success');
  });

  it('should not close an open invoice when confirmation is cancelled', () => {
    dialog.confirm.mockReturnValue(of(false));
    createPage();

    component.closeInvoice();

    expect(invoiceApi.close).not.toHaveBeenCalled();
  });

  it('should close an open invoice with its current version and reload it', () => {
    createPage();

    component.closeInvoice();

    expect(invoiceApi.close).toHaveBeenCalledWith(invoice.id, {
      expectedVersion: invoice.version,
    });
    expect(toast.show).toHaveBeenCalledWith({
      tone: 'success',
      title: 'Fatura fechada',
      message: 'A fatura foi fechada e está pronta para pagamento.',
    });
    expect(invoiceApi.findById).toHaveBeenCalledTimes(2);
  });

  it('should prevent a second close while confirmation or request is pending', () => {
    const confirmation = new Subject<boolean>();
    const closing = new Subject<InvoiceDetail>();
    dialog.confirm.mockReturnValue(confirmation.asObservable());
    invoiceApi.close.mockReturnValue(closing.asObservable());
    createPage();

    component.closeInvoice();
    component.closeInvoice();

    expect(dialog.confirm).toHaveBeenCalledOnce();
    expect(component.isProcessing()).toBe(true);

    confirmation.next(true);
    component.closeInvoice();

    expect(invoiceApi.close).toHaveBeenCalledOnce();
  });

  it('should expose payment only for a closed invoice', () => {
    invoiceApi.findById.mockReturnValue(of({ ...invoice, status: 'CLOSED' }));

    createPage();

    const content = fixture.nativeElement.textContent as string;

    expect(content).toContain('Pagar fatura');
    expect(content).not.toContain('Fechar fatura');

    component.openPaymentForm();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Conta de pagamento');
    expect(component.paymentForm.controls.sourceAccountId.value).toBe(account.id);
  });

  it('should not pay a closed invoice when confirmation is cancelled', () => {
    invoiceApi.findById.mockReturnValue(of({ ...invoice, status: 'CLOSED' }));
    dialog.confirm.mockReturnValue(of(false));
    createPage();
    component.openPaymentForm();

    component.payInvoice();

    expect(invoiceApi.pay).not.toHaveBeenCalled();
  });

  it('should pay a closed invoice with the selected active account and reload it', () => {
    invoiceApi.findById.mockReturnValue(of({ ...invoice, status: 'CLOSED' }));
    createPage();
    component.openPaymentForm();

    component.payInvoice();

    expect(invoiceApi.pay).toHaveBeenCalledWith(invoice.id, {
      sourceAccountId: account.id,
      paymentDate: expect.any(String),
      expectedVersion: invoice.version,
    });
    expect(dialog.confirm).toHaveBeenCalledWith(
      expect.objectContaining({
        message: expect.stringContaining('Conta Walter'),
      }),
    );
    expect(dialog.confirm.mock.calls[0][0].message).toContain('850,75');
    expect(toast.show).toHaveBeenCalledWith({
      tone: 'success',
      title: 'Fatura paga',
      message: 'O pagamento foi registrado e o limite do cartão foi atualizado.',
    });
    expect(invoiceApi.findById).toHaveBeenCalledTimes(2);
  });

  it('should prevent a second payment while confirmation or request is pending', () => {
    const confirmation = new Subject<boolean>();
    const payment = new Subject<unknown>();
    invoiceApi.findById.mockReturnValue(of({ ...invoice, status: 'CLOSED' }));
    dialog.confirm.mockReturnValue(confirmation.asObservable());
    invoiceApi.pay.mockReturnValue(payment.asObservable());
    createPage();
    component.openPaymentForm();

    component.payInvoice();
    component.payInvoice();

    expect(dialog.confirm).toHaveBeenCalledOnce();
    expect(component.isProcessing()).toBe(true);

    confirmation.next(true);
    component.payInvoice();

    expect(invoiceApi.pay).toHaveBeenCalledOnce();
  });

  it('should allow a credit-only payment without requiring an account', () => {
    invoiceApi.findById.mockReturnValue(of({ ...invoice, status: 'CLOSED' }));
    accountApi.findAll.mockReturnValue(of([inactiveAccount]));
    createPage();
    component.openPaymentForm();

    component.payInvoice();

    expect(invoiceApi.pay).toHaveBeenCalledWith(invoice.id, {
      sourceAccountId: null,
      paymentDate: expect.any(String),
      expectedVersion: invoice.version,
    });
  });

  it('should allow reopening a paid invoice while keeping cancelled invoices read-only', () => {
    invoiceApi.findById.mockReturnValue(
      of({
        ...invoice,
        status: 'PAID',
        paidAt: '2026-09-28T14:30:00Z',
        creditAppliedAmount: 50,
      }),
    );

    createPage();

    const paidContent = fixture.nativeElement.textContent as string;

    expect(paidContent).toContain('Créditos aplicados');
    expect(paidContent).toContain('Fatura quitada');
    expect(paidContent).toContain('Reabrir fatura');
    expect(paidContent).not.toContain('Fechar fatura');
    expect(paidContent).not.toContain('Pagar fatura');

    component.invoice.set({ ...invoice, status: 'CANCELLED' });
    fixture.detectChanges();

    const cancelledContent = fixture.nativeElement.textContent as string;

    expect(cancelledContent).toContain('Fatura cancelada');
    expect(cancelledContent).not.toContain('Reabrir fatura');
    expect(cancelledContent).not.toContain('Fechar fatura');
    expect(cancelledContent).not.toContain('Pagar fatura');
  });

  it('should confirm reopening a paid invoice and reload its data', () => {
    const paidInvoice = {
      ...invoice,
      status: 'PAID' as const,
      paidAt: '2026-09-28T14:30:00Z',
      version: 7,
    };
    invoiceApi.findById.mockReturnValue(of(paidInvoice));
    createPage();

    component.reopenInvoice();

    expect(dialog.confirm).toHaveBeenCalledWith(
      expect.objectContaining({ message: expect.stringContaining('pagamento será desfeito') }),
    );
    expect(invoiceApi.reopen).toHaveBeenCalledWith(invoice.id, { expectedVersion: 7 });
    expect(toast.show).toHaveBeenCalledWith({
      tone: 'success',
      title: 'Fatura reaberta',
      message: 'Agora você pode corrigir as compras desta fatura.',
    });
    expect(invoiceApi.findById).toHaveBeenCalledTimes(2);
  });

  it('should reload a conflicting invoice without automatically repeating the operation', () => {
    const conflict = new ApiRequestError({
      timestamp: '2026-09-16T10:00:00Z',
      status: 409,
      code: 'OPTIMISTIC_LOCK_CONFLICT',
      message: 'A fatura foi alterada. Atualize os dados e tente novamente.',
      path: `/api/v1/invoices/${invoice.id}/close`,
      fieldErrors: [],
    });
    const refreshedInvoice = { ...invoice, version: invoice.version + 1 };
    dialog.confirm.mockReturnValueOnce(of(true)).mockReturnValueOnce(of(false));
    invoiceApi.findById.mockReturnValueOnce(of(invoice)).mockReturnValueOnce(of(refreshedInvoice));
    invoiceApi.close.mockReturnValue(throwError(() => conflict));
    createPage();

    component.closeInvoice();
    fixture.detectChanges();

    expect(invoiceApi.close).toHaveBeenCalledOnce();
    expect(fixture.nativeElement.textContent).toContain(conflict.message);
    expect(component.invoice()).toEqual(refreshedInvoice);
    expect(invoiceApi.findById).toHaveBeenCalledTimes(2);
    expect(fixture.nativeElement.textContent).not.toContain('Recarregar fatura');

    component.closeInvoice();

    expect(invoiceApi.close).toHaveBeenCalledOnce();
    expect(dialog.confirm).toHaveBeenCalledTimes(2);
  });

  it('should navigate back to invoices and to the related credit card', () => {
    createPage();

    component.goBack();
    component.goToCreditCard();

    expect(router.navigate).toHaveBeenNthCalledWith(1, ['/invoices']);
    expect(router.navigate).toHaveBeenNthCalledWith(2, ['/credit-cards', creditCard.id]);
  });
});
