import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router } from '@angular/router';
import { catchError, forkJoin, of, switchMap } from 'rxjs';

import { Badge } from '../../../shared/ui/badge/badge';
import { Button } from '../../../shared/ui/button/button';
import { Card } from '../../../shared/ui/card/card';
import { ErrorState } from '../../../shared/ui/error-state/error-state';
import { Skeleton } from '../../../shared/ui/skeleton/skeleton';
import type { FeedbackTone } from '../../../shared/ui/types/feedback-tone';
import { IsoDatePipe } from '../../../shared/pipes/iso-date.pipe';
import { BudgetAlertStatus } from '../../budgets/models/budget.models';
import { CategoryApiService } from '../../categories/data-access/category-api.service';
import { Category } from '../../categories/models/category.models';
import { ReportApiService } from '../../reports/data-access/report-api.service';
import {
  AnnualCashFlow,
  AnnualCashFlowMonth,
  CategoryCashFlow,
  MonthlyCashFlow,
} from '../../reports/models/report.models';
import { DashboardApiService } from '../data-access/dashboard-api.service';
import { Dashboard, DashboardBudgetItem, DashboardIndicator } from '../models/dashboard.models';

type DashboardState = 'loading' | 'success' | 'error';

interface IndicatorCard {
  title: string;
  description: string;
  indicator: DashboardIndicator;
}

interface FlowTooltip {
  month: AnnualCashFlowMonth;
  type: 'income' | 'outflow';
}

interface CategorySegment {
  category: CategoryCashFlow;
  color: string;
  percentage: number;
  path: string;
}

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
  selector: 'app-dashboard-page',
  imports: [Badge, Button, Card, CurrencyPipe, DecimalPipe, ErrorState, IsoDatePipe, Skeleton],
  styleUrl: './dashboard-page.scss',
  templateUrl: './dashboard-page.html',
})
export class DashboardPage implements OnInit {
  private readonly dashboardApi = inject(DashboardApiService);
  private readonly categoryApi = inject(CategoryApiService);
  private readonly reportApi = inject(ReportApiService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  readonly state = signal<DashboardState>('loading');
  readonly dashboard = signal<Dashboard | null>(null);
  readonly categories = signal<Category[]>([]);
  readonly annualCashFlow = signal<AnnualCashFlow | null>(null);
  readonly monthlyCashFlow = signal<MonthlyCashFlow | null>(null);
  readonly selectedFlow = signal<FlowTooltip | null>(null);
  readonly selectedCategory = signal<CategorySegment | null>(null);

  readonly categoryExpenses = computed(
    () => this.monthlyCashFlow()?.summary.expenseCategories ?? [],
  );

  readonly categorySegments = computed<CategorySegment[]>(() => {
    const total = this.categoryTotal();

    if (total === 0) {
      return [];
    }

    let startPercentage = 0;

    return this.categoryExpenses().map((category, index) => {
      const percentage = (category.amount / total) * 100;
      const segment: CategorySegment = {
        category,
        color: this.categoryColor(index),
        percentage,
        path: this.categoryDonutPath(startPercentage, percentage),
      };

      startPercentage += percentage;
      return segment;
    });
  });

  readonly indicatorCards = computed<IndicatorCard[]>(() => {
    const dashboard = this.dashboard();

    if (!dashboard) {
      return [];
    }

    const cards: IndicatorCard[] = [
      {
        title: 'Saldo',
        description: 'Entradas menos todas as saídas do mês, incluindo a fatura de referência.',
        indicator: dashboard.monthlyBalance,
      },
      {
        title: 'Entradas mensais',
        description: 'Total acumulado de entradas efetivadas no mês de referência.',
        indicator: dashboard.monthlyInflows,
      },
      {
        title: 'Saídas mensais',
        description: 'Saídas de caixa efetivadas no mês de referência.',
        indicator: dashboard.monthlyOutflows,
      },
      {
        title: 'Total de faturas abertas',
        description: 'Valor atual de todas as faturas em aberto.',
        indicator: dashboard.openInvoices,
      },
    ];

    if (dashboard.monthlyOpenInvoices) {
      cards.push({
        title: 'Faturas abertas do mês',
        description: 'Valor em aberto das faturas referentes ao mês exibido.',
        indicator: dashboard.monthlyOpenInvoices,
      });
    }

    return cards;
  });

  ngOnInit(): void {
    this.loadDashboard();
  }

  loadDashboard(): void {
    this.state.set('loading');

    this.dashboardApi
      .get()
      .pipe(
        switchMap((dashboard) =>
          forkJoin({
            dashboard: of(dashboard),
            categories: this.categoryApi.findAll().pipe(catchError(() => of([]))),
            annualCashFlow: this.reportApi
              .getAnnual(dashboard.year)
              .pipe(catchError(() => of(null))),
            monthlyCashFlow: this.reportApi
              .getMonthly(dashboard.year, dashboard.month)
              .pipe(catchError(() => of(null))),
          }),
        ),
      )
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ({ dashboard, categories, annualCashFlow, monthlyCashFlow }) => {
          this.dashboard.set(dashboard);
          this.categories.set(categories);
          this.annualCashFlow.set(annualCashFlow);
          this.monthlyCashFlow.set(monthlyCashFlow);
          this.state.set('success');
        },
        error: () => this.state.set('error'),
      });
  }

  goToReports(): void {
    void this.router.navigate(['/reports']);
  }

  periodLabel(): string {
    const dashboard = this.dashboard();

    if (!dashboard) {
      return '';
    }

    return `${MONTHS[dashboard.month - 1]} de ${dashboard.year}`;
  }

  categoryName(categoryId: string): string {
    return (
      this.categories().find((category) => category.id === categoryId)?.name ??
      'Categoria indisponível'
    );
  }

  alertLabel(item: DashboardBudgetItem): string {
    if (item.alertStatus === 'NORMAL') {
      return 'Consumo normal';
    }

    if (item.alertStatus === 'ALERT') {
      return 'Alerta de consumo';
    }

    return item.usagePercentage > 100 ? 'Limite excedido' : 'Limite atingido';
  }

  alertMessage(item: DashboardBudgetItem): string {
    const percentage = this.formatPercentage(item.usagePercentage);

    if (item.alertStatus === 'NORMAL') {
      return `Consumo normal: ${percentage} do limite utilizado.`;
    }

    if (item.alertStatus === 'ALERT') {
      return `Alerta: ${percentage} do limite utilizado. Acompanhe os próximos gastos.`;
    }

    return item.usagePercentage > 100
      ? `Limite excedido: ${percentage} do limite utilizado.`
      : `Limite atingido: ${percentage} do limite utilizado.`;
  }

  alertTone(status: BudgetAlertStatus): FeedbackTone {
    const tones: Record<BudgetAlertStatus, FeedbackTone> = {
      NORMAL: 'success',
      ALERT: 'warning',
      LIMIT_REACHED: 'danger',
    };

    return tones[status];
  }

  progressValue(percentage: number): number {
    return Math.max(0, Math.min(percentage, 100));
  }

  monthlyFlow(): AnnualCashFlowMonth[] {
    return this.annualCashFlow()?.evolution ?? [];
  }

  flowBarHeight(amount: number): number {
    const maximum = Math.max(
      0,
      ...this.monthlyFlow().flatMap((month) => [month.totals.inflows, month.totals.outflows]),
    );

    if (amount <= 0 || maximum === 0) {
      return 0;
    }

    return Math.max(3, (amount / maximum) * 46);
  }

  monthShortLabel(month: number): string {
    return (MONTHS[month - 1] ?? 'Mês').slice(0, 3);
  }

  categoryTotal(): number {
    return this.categoryExpenses().reduce((total, category) => total + category.amount, 0);
  }

  categoryPercentage(amount: number): number {
    const total = this.categoryTotal();

    return total === 0 ? 0 : (amount / total) * 100;
  }

  categoryColor(index: number): string {
    return `hsl(${(210 + index * 137.508) % 360} 62% 48%)`;
  }

  selectFlow(month: AnnualCashFlowMonth, type: FlowTooltip['type']): void {
    this.selectedFlow.set({ month, type });
  }

  clearFlowSelection(): void {
    this.selectedFlow.set(null);
  }

  flowTooltipText(): string {
    const selected = this.selectedFlow();

    if (!selected) {
      return 'Passe o mouse, use Tab ou toque em uma barra para consultar o valor.';
    }

    const label = selected.type === 'income' ? 'Entradas' : 'Saídas';
    const amount =
      selected.type === 'income' ? selected.month.totals.inflows : selected.month.totals.outflows;

    return `${this.monthShortLabel(selected.month.month)} · ${label}: ${this.formatCurrency(amount)}`;
  }

  selectCategory(segment: CategorySegment): void {
    this.selectedCategory.set(segment);
  }

  clearCategorySelection(): void {
    this.selectedCategory.set(null);
  }

  categoryTooltipText(): string {
    const selected = this.selectedCategory();

    if (!selected) {
      return 'Passe o mouse, use Tab ou toque em uma fatia para consultar a categoria e o valor.';
    }

    return `${selected.category.name}: ${this.formatCurrency(selected.category.amount)} (${this.formatPercentage(selected.percentage)})`;
  }

  monthlyFlowDescription(): string {
    return `Fluxo mensal de ${this.annualCashFlow()?.year ?? ''}: ${this.monthlyFlow()
      .map(
        (month) =>
          `${this.monthShortLabel(month.month)}: entradas ${month.totals.inflows.toFixed(2)} e saídas ${month.totals.outflows.toFixed(2)}`,
      )
      .join('; ')}.`;
  }

  private formatPercentage(value: number): string {
    return `${value.toLocaleString('pt-BR', { maximumFractionDigits: 2 })}%`;
  }

  private formatCurrency(value: number): string {
    return value.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
  }

  private categoryDonutPath(startPercentage: number, percentage: number): string {
    if (percentage >= 100) {
      return 'M 50 50 L 50 0 A 50 50 0 1 1 49.999 0 Z';
    }

    const startAngle = startPercentage * 3.6 - 90;
    const endAngle = (startPercentage + percentage) * 3.6 - 90;
    const start = this.pointOnCircle(startAngle);
    const end = this.pointOnCircle(endAngle);
    const largeArc = percentage > 50 ? 1 : 0;

    return `M 50 50 L ${start.x} ${start.y} A 50 50 0 ${largeArc} 1 ${end.x} ${end.y} Z`;
  }

  private pointOnCircle(angle: number): { x: number; y: number } {
    const radians = (angle * Math.PI) / 180;

    return {
      x: 50 + 50 * Math.cos(radians),
      y: 50 + 50 * Math.sin(radians),
    };
  }
}
