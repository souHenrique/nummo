import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { API_BASE_URL } from '../../../core/config/api-base-url';
import {
  AnnualCashFlow,
  AnnualCompetenceReport,
  CashFlowSummary,
  CompetenceReport,
  DailyCashFlow,
  MonthlyCashFlow,
  WeeklyCashFlow,
} from '../models/report.models';
import { ReportApiService } from './report-api.service';

describe('ReportApiService', () => {
  let service: ReportApiService;
  let httpMock: HttpTestingController;

  const summary: CashFlowSummary = {
    inflows: 5000,
    outflows: 1500,
    net: 3500,
    invoicePayments: 1200,
    incomeCategories: [],
    expenseCategories: [],
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        ReportApiService,
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: API_BASE_URL,
          useValue: '/api/v1',
        },
      ],
    });

    service = TestBed.inject(ReportApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('deve buscar o relatório diário', () => {
    const response: DailyCashFlow = {
      date: '2026-09-03',
      summary,
    };

    service.getDaily(response.date).subscribe((result) => {
      expect(result).toEqual(response);
    });

    const request = httpMock.expectOne(
      (candidate) => candidate.url === '/api/v1/reports/cash/daily',
    );

    expect(request.request.method).toBe('GET');
    expect(request.request.params.get('date')).toBe(response.date);

    request.flush(response);
  });

  it('deve buscar o relatório semanal', () => {
    const response: WeeklyCashFlow = {
      currentWeek: {
        startDate: '2026-08-31',
        endDate: '2026-09-06',
        summary,
      },
      previousWeek: {
        startDate: '2026-08-24',
        endDate: '2026-08-30',
        summary,
      },
      comparison: {
        inflowsDifference: 500,
        outflowsDifference: 200,
        netDifference: 300,
      },
    };

    service.getWeekly('2026-09-03').subscribe((result) => {
      expect(result).toEqual(response);
    });

    const request = httpMock.expectOne(
      (candidate) => candidate.url === '/api/v1/reports/cash/weekly',
    );

    expect(request.request.method).toBe('GET');
    expect(request.request.params.get('date')).toBe('2026-09-03');

    request.flush(response);
  });

  it('deve buscar o relatório mensal', () => {
    const response: MonthlyCashFlow = {
      year: 2026,
      month: 9,
      startDate: '2026-09-01',
      endDate: '2026-09-30',
      summary,
    };

    service.getMonthly(2026, 9).subscribe((result) => {
      expect(result).toEqual(response);
    });

    const request = httpMock.expectOne(
      (candidate) => candidate.url === '/api/v1/reports/cash/monthly',
    );

    expect(request.request.method).toBe('GET');
    expect(request.request.params.get('year')).toBe('2026');
    expect(request.request.params.get('month')).toBe('9');

    request.flush(response);
  });

  it('deve buscar o relatório anual', () => {
    const response: AnnualCashFlow = {
      year: 2026,
      startDate: '2026-01-01',
      endDate: '2026-12-31',
      evolution: [
        {
          month: 9,
          totals: {
            inflows: 5000,
            outflows: 1500,
            net: 3500,
          },
        },
      ],
    };

    service.getAnnual(2026).subscribe((result) => {
      expect(result).toEqual(response);
    });

    const request = httpMock.expectOne(
      (candidate) => candidate.url === '/api/v1/reports/cash/annual',
    );

    expect(request.request.method).toBe('GET');
    expect(request.request.params.get('year')).toBe('2026');

    request.flush(response);
  });

  it('deve buscar o relatório por competência', () => {
    const response: CompetenceReport = {
      startDate: '2026-09-01',
      endDate: '2026-09-30',
      totalIncome: 5000,
      totalExpenses: 1800,
      result: 3200,
      incomeCategories: [],
      expenseCategories: [],
    };

    service.getCompetence(response.startDate, response.endDate).subscribe((result) => {
      expect(result).toEqual(response);
    });

    const request = httpMock.expectOne(
      (candidate) => candidate.url === '/api/v1/reports/competence',
    );

    expect(request.request.method).toBe('GET');
    expect(request.request.params.get('startDate')).toBe(response.startDate);
    expect(request.request.params.get('endDate')).toBe(response.endDate);

    request.flush(response);
  });

  it('deve buscar a evolução anual por competência', () => {
    const response: AnnualCompetenceReport = {
      year: 2026,
      startDate: '2026-01-01',
      endDate: '2026-12-31',
      evolution: [],
    };

    service.getCompetenceAnnual(2026).subscribe((result) => {
      expect(result).toEqual(response);
    });

    const request = httpMock.expectOne(
      (candidate) => candidate.url === '/api/v1/reports/competence/annual',
    );

    expect(request.request.method).toBe('GET');
    expect(request.request.params.get('year')).toBe('2026');

    request.flush(response);
  });
});
