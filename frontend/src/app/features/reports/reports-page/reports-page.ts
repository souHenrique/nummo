import { CurrencyPipe } from '@angular/common';
import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Observable, map } from 'rxjs';

import { Button } from '../../../shared/ui/button/button';
import { Card } from '../../../shared/ui/card/card';
import { ErrorState } from '../../../shared/ui/error-state/error-state';
import { InputDirective } from '../../../shared/ui/form-control/input';
import { FormField } from '../../../shared/ui/form-field/form-field';
import { Skeleton } from '../../../shared/ui/skeleton/skeleton';
import {
  ReportCategoryChartComponent,
  ReportChartEntry,
} from '../components/report-category-chart/report-category-chart';
import { ReportApiService } from '../data-access/report-api.service';
import {
  AnnualCashFlow,
  CashFlowComparison,
  CashFlowSummary,
  CompetenceReport,
} from '../models/report.models';

type ReportMode = 'daily' | 'weekly' | 'monthly' | 'annual' | 'competence';
type ReportBasis = 'CASH' | 'COMPETENCE';
type ReportState = 'loading' | 'success' | 'error';

interface ReportTab {
  id: ReportMode;
  label: string;
}

interface SummaryReportView {
  mode: Exclude<ReportMode, 'annual'>;
  title: string;
  basis: ReportBasis;
  period: string;
  summary: CashFlowSummary;
  previousPeriod?: string;
  previousSummary?: CashFlowSummary;
  comparison?: CashFlowComparison;
}

interface AnnualReportView {
  mode: 'annual';
  title: string;
  basis: 'CASH';
  period: string;
  data: AnnualCashFlow;
}

type ReportView = SummaryReportView | AnnualReportView;

const MONTHS = [
  'Janeiro',
  'Fevereiro',
  'Março',
  'Abril',
  'Maio',
  'Junho',
  'Julho',
  'Agosto',
  'Setembro',
  'Outubro',
  'Novembro',
  'Dezembro',
] as const;

@Component({
  selector: 'app-reports-page',
  imports: [
    Button,
    Card,
    CurrencyPipe,
    ErrorState,
    FormField,
    InputDirective,
    ReactiveFormsModule,
    ReportCategoryChartComponent,
    Skeleton,
  ],
  styleUrl: './reports-page.scss',
  templateUrl: './reports-page.html',
})
export class ReportsPage implements OnInit {
  private readonly reportApi = inject(ReportApiService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);
  private readonly currentDate = new Date();

  readonly activeMode = signal<ReportMode>('monthly');
  readonly state = signal<ReportState>('loading');
  readonly result = signal<ReportView | null>(null);
  readonly validationMessage = signal<string | null>(null);

  readonly tabs: readonly ReportTab[] = [
    { id: 'daily', label: 'Diário' },
    { id: 'weekly', label: 'Semanal' },
    { id: 'monthly', label: 'Mensal' },
    { id: 'annual', label: 'Anual' },
    { id: 'competence', label: 'Data da despesa' },
  ];

  readonly filters = this.formBuilder.nonNullable.group({
    date: [this.toIsoDate(this.currentDate), Validators.required],
    month: [this.toMonthInput(this.currentDate), Validators.required],
    year: [
      this.currentDate.getFullYear(),
      [Validators.required, Validators.min(1), Validators.max(9999)],
    ],
    startDate: [
      this.toIsoDate(new Date(this.currentDate.getFullYear(), this.currentDate.getMonth(), 1)),
      Validators.required,
    ],
    endDate: [this.toIsoDate(this.currentDate), Validators.required],
  });

  ngOnInit(): void {
    this.loadReport();
  }

  selectMode(mode: ReportMode): void {
    if (this.activeMode() === mode) {
      return;
    }

    this.activeMode.set(mode);
    this.loadReport();
  }

  loadReport(): void {
    this.validationMessage.set(null);
    const request = this.buildRequest();

    if (!request) {
      return;
    }

    this.state.set('loading');
    this.result.set(null);

    request.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (result) => {
        this.result.set(result);
        this.state.set('success');
      },
      error: () => this.state.set('error'),
    });
  }

  incomeLabel(report: SummaryReportView): string {
    return report.basis === 'CASH' ? 'Entradas' : 'Receitas';
  }

  expenseLabel(report: SummaryReportView): string {
    return report.basis === 'CASH' ? 'Saídas' : 'Despesas';
  }

  categoryEntries(summary: CashFlowSummary): ReportChartEntry[] {
    return [
      ...summary.incomeCategories.map((category) => ({
        label: category.name,
        amount: category.amount,
        type: 'Receita' as const,
      })),
      ...summary.expenseCategories.map((category) => ({
        label: category.name,
        amount: category.amount,
        type: 'Despesa' as const,
      })),
    ];
  }

  annualMaximum(report: AnnualReportView): number {
    return Math.max(
      0,
      ...report.data.evolution.flatMap((month) => [
        month.totals.inflows,
        month.totals.outflows,
        Math.abs(month.totals.net),
      ]),
    );
  }

  annualBarWidth(amount: number, maximum: number): number {
    return maximum === 0 ? 0 : (Math.abs(amount) / maximum) * 100;
  }

  annualChartDescription(report: AnnualReportView): string {
    return `Fluxo de caixa anual de ${report.data.year}: ${report.data.evolution
      .map(
        (month) =>
          `${this.monthLabel(month.month)}: entradas ${month.totals.inflows.toFixed(2)}, saídas ${month.totals.outflows.toFixed(2)}, resultado ${month.totals.net.toFixed(2)}`,
      )
      .join('; ')}.`;
  }

  monthLabel(month: number): string {
    return MONTHS[month - 1] ?? 'Mês inválido';
  }

  private buildRequest(): Observable<ReportView> | null {
    const values = this.filters.getRawValue();

    if (this.filters.invalid) {
      this.filters.markAllAsTouched();
      this.validationMessage.set('Preencha um período válido antes de gerar o relatório.');
      return null;
    }

    switch (this.activeMode()) {
      case 'daily':
        if (!this.isIsoDate(values.date)) {
          return this.invalidPeriod();
        }

        return this.reportApi
          .getDaily(values.date)
          .pipe(map((data) => this.cashView('daily', 'Relatório diário', data.date, data.summary)));

      case 'weekly':
        if (!this.isIsoDate(values.date)) {
          return this.invalidPeriod();
        }

        return this.reportApi.getWeekly(values.date).pipe(
          map((data) =>
            this.cashView(
              'weekly',
              'Relatório semanal',
              `${data.currentWeek.startDate} a ${data.currentWeek.endDate}`,
              data.currentWeek.summary,
              {
                previousPeriod: `${data.previousWeek.startDate} a ${data.previousWeek.endDate}`,
                previousSummary: data.previousWeek.summary,
                comparison: data.comparison,
              },
            ),
          ),
        );

      case 'monthly': {
        const monthFilter = /^(\d{4})-(\d{2})$/.exec(values.month);

        if (!monthFilter) {
          return this.invalidPeriod();
        }

        const year = Number(monthFilter[1]);
        const month = Number(monthFilter[2]);

        if (year < 1 || year > 9999 || month < 1 || month > 12) {
          return this.invalidPeriod();
        }

        return this.reportApi
          .getMonthly(year, month)
          .pipe(
            map((data) =>
              this.cashView(
                'monthly',
                'Relatório mensal',
                `${this.monthLabel(data.month)} de ${data.year}`,
                data.summary,
              ),
            ),
          );
      }

      case 'annual':
        return this.reportApi.getAnnual(values.year).pipe(
          map((data) => ({
            mode: 'annual' as const,
            title: 'Relatório anual',
            basis: 'CASH' as const,
            period: String(data.year),
            data,
          })),
        );

      case 'competence':
        if (!this.isIsoDate(values.startDate) || !this.isIsoDate(values.endDate)) {
          return this.invalidPeriod();
        }

        if (values.startDate > values.endDate) {
          this.validationMessage.set('A data inicial deve ser anterior ou igual à data final.');
          return null;
        }

        return this.reportApi
          .getCompetence(values.startDate, values.endDate)
          .pipe(map((data) => this.competenceView(data)));
    }
  }

  private cashView(
    mode: Exclude<ReportMode, 'annual' | 'competence'>,
    title: string,
    period: string,
    summary: CashFlowSummary,
    comparison: Pick<SummaryReportView, 'previousPeriod' | 'previousSummary' | 'comparison'> = {},
  ): SummaryReportView {
    return {
      mode,
      title,
      basis: 'CASH',
      period,
      summary,
      ...comparison,
    };
  }

  private competenceView(data: CompetenceReport): SummaryReportView {
    return {
      mode: 'competence',
      title: 'Relatório por data da despesa',
      basis: 'COMPETENCE',
      period: `${data.startDate} a ${data.endDate}`,
      summary: {
        inflows: data.totalIncome,
        outflows: data.totalExpenses,
        net: data.result,
        invoiceOutflows: 0,
        incomeCategories: data.incomeCategories,
        expenseCategories: data.expenseCategories,
      },
    };
  }

  private invalidPeriod(): null {
    this.validationMessage.set('Preencha um período válido antes de gerar o relatório.');
    return null;
  }

  private isIsoDate(value: string): boolean {
    return /^\d{4}-\d{2}-\d{2}$/.test(value);
  }

  private toIsoDate(date: Date): string {
    const year = String(date.getFullYear()).padStart(4, '0');
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');

    return `${year}-${month}-${day}`;
  }

  private toMonthInput(date: Date): string {
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;
  }
}
