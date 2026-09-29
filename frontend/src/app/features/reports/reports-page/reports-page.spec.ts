import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, Subject, throwError } from 'rxjs';

import { ReportApiService } from '../data-access/report-api.service';
import {
  AnnualCashFlow,
  CashFlowSummary,
  CompetenceReport,
  DailyCashFlow,
  MonthlyCashFlow,
  WeeklyCashFlow,
} from '../models/report.models';
import { ReportsPage } from './reports-page';

describe('ReportsPage', () => {
  let fixture: ComponentFixture<ReportsPage>;
  let component: ReportsPage;
  let reportApi: {
    getAnnual: ReturnType<typeof vi.fn>;
    getCompetence: ReturnType<typeof vi.fn>;
    getDaily: ReturnType<typeof vi.fn>;
    getMonthly: ReturnType<typeof vi.fn>;
    getWeekly: ReturnType<typeof vi.fn>;
  };

  const summary: CashFlowSummary = {
    inflows: 5000,
    outflows: 1500,
    net: 3500,
    invoiceOutflows: 1200,
    incomeCategories: [
      { categoryId: '1ad6caa5-b046-41d9-a6cf-2725aa68a0de', name: 'Salário', amount: 5000 },
    ],
    expenseCategories: [
      { categoryId: '67c5aac1-8db4-4c88-8ea2-488d42027f82', name: 'Mercado', amount: 800 },
    ],
  };

  const daily: DailyCashFlow = { date: '2026-09-03', summary };
  const weekly: WeeklyCashFlow = {
    currentWeek: { startDate: '2026-08-31', endDate: '2026-09-06', summary },
    previousWeek: {
      startDate: '2026-08-24',
      endDate: '2026-08-30',
      summary: { ...summary, inflows: 4500, outflows: 1300, net: 3200 },
    },
    comparison: { inflowsDifference: 500, outflowsDifference: 200, netDifference: 300 },
  };
  const monthly: MonthlyCashFlow = {
    year: 2026,
    month: 9,
    startDate: '2026-09-01',
    endDate: '2026-09-30',
    summary,
  };
  const annual: AnnualCashFlow = {
    year: 2026,
    startDate: '2026-01-01',
    endDate: '2026-12-31',
    evolution: [
      { month: 1, totals: { inflows: 4500, outflows: 1800, net: 2700 } },
      { month: 2, totals: { inflows: 5000, outflows: 1500, net: 3500 } },
    ],
  };
  const competence: CompetenceReport = {
    startDate: '2026-09-01',
    endDate: '2026-09-30',
    totalIncome: 5000,
    totalExpenses: 1800,
    result: 3200,
    incomeCategories: summary.incomeCategories,
    expenseCategories: [
      { categoryId: '67c5aac1-8db4-4c88-8ea2-488d42027f82', name: 'Mercado', amount: 800 },
      {
        categoryId: 'f0cb1223-78be-441f-b806-69fd054e16d9',
        name: 'Compra no cartão',
        amount: 1000,
      },
    ],
  };

  beforeEach(async () => {
    reportApi = {
      getDaily: vi.fn().mockReturnValue(of(daily)),
      getWeekly: vi.fn().mockReturnValue(of(weekly)),
      getMonthly: vi.fn().mockReturnValue(of(monthly)),
      getAnnual: vi.fn().mockReturnValue(of(annual)),
      getCompetence: vi.fn().mockReturnValue(of(competence)),
    };

    await TestBed.configureTestingModule({
      imports: [ReportsPage],
      providers: [{ provide: ReportApiService, useValue: reportApi }],
    }).compileComponents();

    fixture = TestBed.createComponent(ReportsPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should load the monthly report without exposing its technical calculation basis', () => {
    const [year, month] = component.filters.controls.month.value.split('-').map(Number);
    const element = fixture.nativeElement as HTMLElement;

    expect(reportApi.getMonthly).toHaveBeenCalledWith(year, month);
    expect(element.textContent).toContain('Relatório mensal');
    expect(element.textContent).not.toContain('CASH');
    expect(element.textContent).not.toContain('COMPETENCE');
    expect(element.textContent).toContain('Pagamentos de fatura');
    expect(element.textContent).toContain('Já incluídos nas saídas.');
    expect(element.querySelector('[role="img"]')).not.toBeNull();
    expect(element.querySelector('table')).not.toBeNull();
  });

  it('should call the selected daily, weekly, annual and expense-date report endpoints', () => {
    component.filters.controls.date.setValue('2026-09-03');
    component.selectMode('daily');
    expect(reportApi.getDaily).toHaveBeenCalledWith('2026-09-03');

    component.selectMode('weekly');
    expect(reportApi.getWeekly).toHaveBeenCalledWith('2026-09-03');

    component.filters.controls.year.setValue(2026);
    component.selectMode('annual');
    expect(reportApi.getAnnual).toHaveBeenCalledWith(2026);

    component.filters.patchValue({ startDate: '2026-09-01', endDate: '2026-09-30' });
    component.selectMode('competence');
    expect(reportApi.getCompetence).toHaveBeenCalledWith('2026-09-01', '2026-09-30');
  });

  it('should render expense-date figures without treating invoice payments as new expenses', () => {
    component.filters.patchValue({ startDate: '2026-09-01', endDate: '2026-09-30' });
    component.selectMode('competence');
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;

    expect(element.textContent).toContain('Relatório por data da despesa');
    expect(element.textContent).not.toContain('COMPETENCE');
    expect(element.textContent).toContain('Compra no cartão');
    expect(element.textContent).not.toContain('Pagamentos de fatura');
  });

  it('should render the annual chart and its equivalent table', () => {
    component.filters.controls.year.setValue(2026);
    component.selectMode('annual');
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;

    expect(element.textContent).toContain('Relatório anual');
    expect(element.textContent).toContain('Gráfico da evolução mensal');
    expect(element.querySelector('.reports__annual-chart[role="img"]')).not.toBeNull();
    expect(element.querySelector('table caption')?.textContent).toContain(
      'Tabela alternativa ao gráfico anual',
    );
    expect(element.textContent).toContain('Janeiro');
  });

  it('should render loading, error and retry states', () => {
    const pending = new Subject<MonthlyCashFlow>();
    reportApi.getMonthly.mockReturnValueOnce(pending);

    component.loadReport();
    fixture.detectChanges();
    expect(
      fixture.nativeElement.querySelector('[aria-label="Carregando relatório"]'),
    ).not.toBeNull();

    pending.next(monthly);
    pending.complete();
    fixture.detectChanges();
    expect(component.state()).toBe('success');

    reportApi.getMonthly.mockReturnValueOnce(throwError(() => new Error('Falha ao buscar')));
    component.loadReport();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Não foi possível carregar o relatório');

    component.loadReport();
    fixture.detectChanges();
    expect(component.state()).toBe('success');
  });

  it('should reject an invalid competence period without calling the API', () => {
    component.selectMode('competence');
    reportApi.getCompetence.mockClear();
    component.filters.patchValue({ startDate: '2026-10-01', endDate: '2026-09-30' });

    component.loadReport();
    fixture.detectChanges();

    expect(reportApi.getCompetence).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain(
      'A data inicial deve ser anterior ou igual à data final.',
    );
  });
});
