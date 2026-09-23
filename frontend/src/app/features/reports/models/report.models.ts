export interface CategoryCashFlow {
  categoryId: string | null;
  name: string;
  amount: number;
}

export interface CashFlowSummary {
  inflows: number;
  outflows: number;
  net: number;
  invoicePayments: number;
  incomeCategories: CategoryCashFlow[];
  expenseCategories: CategoryCashFlow[];
}

export interface DailyCashFlow {
  date: string;
  summary: CashFlowSummary;
}

export interface CashFlowPeriod {
  startDate: string;
  endDate: string;
  summary: CashFlowSummary;
}

export interface CashFlowComparison {
  inflowsDifference: number;
  outflowsDifference: number;
  netDifference: number;
}

export interface WeeklyCashFlow {
  currentWeek: CashFlowPeriod;
  previousWeek: CashFlowPeriod;
  comparison: CashFlowComparison;
}

export interface MonthlyCashFlow {
  year: number;
  month: number;
  startDate: string;
  endDate: string;
  summary: CashFlowSummary;
}

export interface CashFlowTotals {
  inflows: number;
  outflows: number;
  net: number;
}

export interface AnnualCashFlowMonth {
  month: number;
  totals: CashFlowTotals;
}

export interface AnnualCashFlow {
  year: number;
  startDate: string;
  endDate: string;
  evolution: AnnualCashFlowMonth[];
}

export interface AnnualCompetenceReport {
  year: number;
  startDate: string;
  endDate: string;
  evolution: AnnualCashFlowMonth[];
}

export interface CompetenceReport {
  startDate: string;
  endDate: string;
  totalIncome: number;
  totalExpenses: number;
  result: number;
  incomeCategories: CategoryCashFlow[];
  expenseCategories: CategoryCashFlow[];
}
