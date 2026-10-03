import { Routes } from '@angular/router';

export const BILL_ROUTES: Routes = [
  {
    path: '',
    title: 'Boletos | Nummo',
    loadComponent: () =>
      import('./bill-list-page/bill-list-page').then(({ BillListPage }) => BillListPage),
  },
];
