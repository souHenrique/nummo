import { BudgetAlertStatus } from '../../budgets/models/budget.models';

export type AccountingBasis = 'CASH' | 'COMPETENCE' | 'CASH_AND_INVOICE' | 'CASH_AND_BILL';

export interface DashboardIndicator {
  basis: AccountingBasis;
  amount: number;
}

export interface DashboardBudgetItem {
  budgetId: string;
  categoryId: string;
  amountLimit: number;
  spentAmount: number;
  usagePercentage: number;
  alertStatus: BudgetAlertStatus;
}

export interface DashboardBudget {
  basis: 'COMPETENCE';
  totalLimit: number;
  totalSpent: number;
  usagePercentage: number;
  items: DashboardBudgetItem[];
}

export interface Dashboard {
  referenceDate: string;
  year: number;
  month: number;
  periodStart: string;
  periodEnd: string;
  monthlyBalance: DashboardIndicator;
  monthlyInflows: DashboardIndicator;
  totalOutflows: DashboardIndicator;
  monthlyOutflows: DashboardIndicator;
  creditCardPurchaseOutflows: DashboardIndicator;
  competenceExpenses: DashboardIndicator;
  openInvoices: DashboardIndicator;
  /**
   * Optional only while clients may still be connected to an API deployed
   * before the monthly invoice indicator was introduced.
   */
  monthlyOpenInvoices?: DashboardIndicator;
  budget: DashboardBudget;
  consolidatedBalance: DashboardIndicator;
}
