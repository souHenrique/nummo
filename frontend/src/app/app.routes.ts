import { Routes } from '@angular/router';
import { authChildGuard, authGuard } from './core/guards/auth.guards';

export const routes: Routes = [
  {
    path: 'login',
    title: 'Entrar | Nummo',
    loadComponent: () =>
      import('./features/auth/login-page/login-page').then(({ LoginPage }) => LoginPage),
  },
  {
    path: 'register',
    title: 'Criar conta | Nummo',
    loadComponent: () =>
      import('./features/auth/register-page/register-page').then(
        ({ RegisterPage }) => RegisterPage,
      ),
  },
  {
    path: '',
    canActivate: [authGuard],
    canActivateChild: [authChildGuard],
    loadComponent: () => import('./layout/shell/shell').then(({ Shell }) => Shell),
    children: [
      {
        path: '',
        pathMatch: 'full',
        redirectTo: 'dashboard',
      },
      {
        path: 'dashboard',
        loadChildren: () =>
          import('./features/dashboard/dashboard.routes').then(
            ({ DASHBOARD_ROUTES }) => DASHBOARD_ROUTES,
          ),
      },
      {
        path: 'transactions',
        loadChildren: () =>
          import('./features/transactions/transactions.routes').then(
            ({ TRANSACTION_ROUTES }) => TRANSACTION_ROUTES,
          ),
      },
      {
        path: 'transfers',
        loadChildren: () =>
          import('./features/transfers/transfers.routes').then(
            ({ TRANSFER_ROUTES }) => TRANSFER_ROUTES,
          ),
      },
      {
        path: 'accounts',
        loadChildren: () =>
          import('./features/accounts/accounts.routes').then(
            ({ ACCOUNT_ROUTES }) => ACCOUNT_ROUTES,
          ),
      },
      {
        path: 'categories',
        loadChildren: () =>
          import('./features/categories/categories.routes').then(
            ({ CATEGORY_ROUTES }) => CATEGORY_ROUTES,
          ),
      },
      {
        path: 'credit-cards',
        loadChildren: () =>
          import('./features/credit-cards/credit-cards.routes').then(
            ({ CREDIT_CARD_ROUTES }) => CREDIT_CARD_ROUTES,
          ),
      },
      {
        path: 'invoices',
        loadChildren: () =>
          import('./features/invoices/invoices.routes').then(
            ({ INVOICE_ROUTES }) => INVOICE_ROUTES,
          ),
      },
      {
        path: 'budgets',
        loadChildren: () =>
          import('./features/budgets/budgets.routes').then(({ BUDGET_ROUTES }) => BUDGET_ROUTES),
      },
      {
        path: 'bills',
        loadChildren: () =>
          import('./features/bills/bills.routes').then(({ BILL_ROUTES }) => BILL_ROUTES),
      },
      {
        path: 'reports',
        loadChildren: () =>
          import('./features/reports/reports.routes').then(({ REPORT_ROUTES }) => REPORT_ROUTES),
      },
      {
        path: 'profile',
        loadChildren: () =>
          import('./features/profile/profile.routes').then(({ PROFILE_ROUTES }) => PROFILE_ROUTES),
      },
      {
        path: '**',
        title: 'Página não encontrada | Nummo',
        loadComponent: () =>
          import('./features/not-found/not-found-page/not-found-page').then(
            ({ NotFoundPage }) => NotFoundPage,
          ),
      },
    ],
  },
];
