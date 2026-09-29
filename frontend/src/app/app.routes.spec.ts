import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { of } from 'rxjs';

import { routes } from './app.routes';
import { SessionService } from './core/auth/session.service';
import { AppDialogService } from './core/feedback/dialog/dialog.service';
import { ToastService } from './core/feedback/toast/toast.service';
import { AccountApiService } from './features/accounts/data-access/account-api.service';
import { AuthService } from './features/auth/services/auth.service';
import { CategoryApiService } from './features/categories/data-access/category-api.service';
import { BudgetApiService } from './features/budgets/data-access/budget-api.service';
import { CreditCardApiService } from './features/credit-cards/data-access/credit-card-api.service';
import { DashboardApiService } from './features/dashboard/data-access/dashboard-api.service';
import { InvoiceApiService } from './features/invoices/data-access/invoice-api.service';
import { ProfileApiService } from './features/profile/data-access/profile-api.service';
import { ReportApiService } from './features/reports/data-access/report-api.service';
import { TransactionApiService } from './features/transactions/data-access/transaction-api.service';
import { TransactionExportApiService } from './features/transactions/data-access/transaction-export-api.service';
import { TransferApiService } from './features/transfers/data-access/transfer-api.service';

describe('Application routes', () => {
  let session: { hasValidSession: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    session = {
      hasValidSession: vi.fn().mockReturnValue(true),
    };

    TestBed.configureTestingModule({
      providers: [
        provideRouter(routes),
        {
          provide: SessionService,
          useValue: session,
        },
        {
          provide: AuthService,
          useValue: {
            logout: vi.fn(),
          },
        },
        {
          provide: AppDialogService,
          useValue: {
            confirm: vi.fn().mockReturnValue(of(false)),
          },
        },
        {
          provide: ToastService,
          useValue: {
            show: vi.fn(),
          },
        },
        {
          provide: ProfileApiService,
          useValue: {
            getCurrentUser: () =>
              of({
                id: '2a1fbc5b-cbb9-4879-b0c5-42f034d64261',
                name: 'Jesse Pinkman',
                email: 'jesse.pinkman@example.com',
                createdAt: '2026-09-02T12:00:00Z',
                updatedAt: '2026-09-02T12:30:00Z',
              }),
            updateCurrentUser: vi.fn(),
          },
        },
        {
          provide: ReportApiService,
          useValue: {
            getDaily: vi.fn(),
            getWeekly: vi.fn(),
            getMonthly: vi.fn().mockReturnValue(
              of({
                year: 2026,
                month: 9,
                startDate: '2026-09-01',
                endDate: '2026-09-30',
                summary: {
                  inflows: 0,
                  outflows: 0,
                  net: 0,
                  invoiceOutflows: 0,
                  incomeCategories: [],
                  expenseCategories: [],
                },
              }),
            ),
            getAnnual: vi.fn(),
            getCompetence: vi.fn(),
          },
        },
        {
          provide: AccountApiService,
          useValue: {
            findAll: vi.fn().mockReturnValue(of([])),
            findById: vi.fn().mockReturnValue(
              of({
                id: '123',
                name: 'Conta Walter',
                type: 'CHECKING',
                institution: 'Banco Albuquerque',
                initialBalance: 1000,
                currentBalance: 1000,
                status: 'ACTIVE',
                version: 0,
                createdAt: '2026-09-15T10:00:00Z',
                updatedAt: '2026-09-15T10:00:00Z',
              }),
            ),
          },
        },
        {
          provide: CategoryApiService,
          useValue: {
            findAll: vi.fn().mockReturnValue(of([])),
          },
        },
        {
          provide: BudgetApiService,
          useValue: {
            create: vi.fn(),
            delete: vi.fn(),
            findAll: vi.fn().mockReturnValue(of([])),
            update: vi.fn(),
          },
        },
        {
          provide: DashboardApiService,
          useValue: {
            get: vi.fn().mockReturnValue(
              of({
                referenceDate: '2026-09-16',
                year: 2026,
                month: 9,
                periodStart: '2026-09-01',
                periodEnd: '2026-09-30',
                monthlyBalance: { basis: 'CASH_AND_INVOICE', amount: 0 },
                monthlyInflows: { basis: 'CASH', amount: 0 },
                totalOutflows: { basis: 'CASH', amount: 0 },
                monthlyOutflows: { basis: 'CASH', amount: 0 },
                creditCardPurchaseOutflows: { basis: 'COMPETENCE', amount: 0 },
                competenceExpenses: { basis: 'COMPETENCE', amount: 0 },
                openInvoices: { basis: 'COMPETENCE', amount: 0 },
                monthlyOpenInvoices: { basis: 'COMPETENCE', amount: 0 },
                budget: {
                  basis: 'COMPETENCE',
                  totalLimit: 0,
                  totalSpent: 0,
                  usagePercentage: 0,
                  items: [],
                },
                consolidatedBalance: { basis: 'CASH', amount: 0 },
              }),
            ),
          },
        },
        {
          provide: CreditCardApiService,
          useValue: {
            findAll: vi.fn().mockReturnValue(of([])),
            findById: vi.fn().mockReturnValue(
              of({
                id: '123',
                name: 'Cartão Heisenberg',
                creditLimit: 5000,
                availableLimit: 3200,
                closingDay: 10,
                dueDay: 17,
                defaultAccountId: '123',
                status: 'ACTIVE',
                version: 0,
              }),
            ),
            create: vi.fn(),
            createPurchase: vi.fn(),
            update: vi.fn(),
          },
        },
        {
          provide: InvoiceApiService,
          useValue: {
            findAll: vi.fn().mockReturnValue(
              of({
                content: [],
                page: 0,
                size: 20,
                totalElements: 0,
                totalPages: 0,
                first: true,
                last: true,
              }),
            ),
            findById: vi.fn().mockReturnValue(
              of({
                id: '123',
                creditCardId: '123',
                referenceMonth: 9,
                referenceYear: 2026,
                closingDate: '2026-09-20',
                dueDate: '2026-09-28',
                totalAmount: 850.75,
                status: 'OPEN',
                paidAt: null,
                version: 0,
                transactions: [],
                creditAppliedAmount: 0,
              }),
            ),
            close: vi.fn(),
            pay: vi.fn(),
          },
        },
        {
          provide: TransactionApiService,
          useValue: {
            findAll: vi.fn().mockReturnValue(
              of({
                content: [],
                page: 0,
                size: 20,
                totalElements: 0,
                totalPages: 0,
                first: true,
                last: true,
              }),
            ),
            findById: vi.fn().mockReturnValue(
              of({
                id: '123',
                description: 'Transação de Jesse Pinkman',
                amount: 150,
                competenceDate: '2026-09-15',
                effectiveDate: '2026-09-15',
                dueDate: null,
                type: 'EXPENSE',
                status: 'COMPLETED',
                paymentMethod: 'PIX',
                sourceAccountId: null,
                destinationAccountId: null,
                categoryId: null,
                creditCardId: null,
                invoiceId: null,
                installmentGroupId: null,
                installmentNumber: null,
                installmentCount: null,
                createdAt: '2026-09-15T10:00:00Z',
                updatedAt: '2026-09-15T10:00:00Z',
              }),
            ),
          },
        },
        {
          provide: TransactionExportApiService,
          useValue: { download: vi.fn() },
        },
        {
          provide: TransferApiService,
          useValue: {
            create: vi.fn(),
          },
        },
      ],
    });
  });

  it.each(['/login', '/register'])(
    'should render %s without the authenticated shell',
    async (url) => {
      const harness = await RouterTestingHarness.create();

      await harness.navigateByUrl(url);

      expect(TestBed.inject(Router).url).toBe(url);
      expect(harness.routeNativeElement?.querySelector('app-header')).toBeNull();
      expect(harness.routeNativeElement?.querySelector('app-sidebar')).toBeNull();
    },
  );

  it('should render authenticated routes inside the shell', async () => {
    const harness = await RouterTestingHarness.create();

    await harness.navigateByUrl('/dashboard');

    expect(harness.routeNativeElement?.querySelector('app-header')).not.toBeNull();
    expect(harness.routeNativeElement?.querySelector('app-sidebar')).not.toBeNull();
  });

  it('should redirect an unauthenticated user away from a private route', async () => {
    session.hasValidSession.mockReturnValue(false);
    const harness = await RouterTestingHarness.create();

    await harness.navigateByUrl('/dashboard');

    expect(TestBed.inject(Router).url).toBe('/login?returnUrl=%2Fdashboard');
    expect(harness.routeNativeElement?.querySelector('app-header')).toBeNull();
  });

  it.each([
    '/dashboard',
    '/transactions',
    '/transactions/new',
    '/transactions/123',
    '/transfers/new',
    '/accounts',
    '/accounts/123',
    '/categories',
    '/credit-cards',
    '/credit-cards/new',
    '/credit-cards/123',
    '/credit-cards/123/edit',
    '/credit-cards/123/purchases/new',
    '/invoices',
    '/invoices/123',
    '/budgets',
    '/reports',
    '/profile',
  ])(
    'should navigate successfully to %s',
    async (url) => {
      const harness = await RouterTestingHarness.create();

      await harness.navigateByUrl(url);

      expect(TestBed.inject(Router).url).toBe(url);
      expect(harness.routeNativeElement?.textContent).not.toContain('Erro 404');
    },
    15_000,
  );

  it('should render the 404 page for an unknown route', async () => {
    const harness = await RouterTestingHarness.create();

    await harness.navigateByUrl('/rota-inexistente');

    expect(harness.routeNativeElement?.textContent).toContain('Erro 404');
    expect(harness.routeNativeElement?.textContent).toContain('Página não encontrada');
  });
});
