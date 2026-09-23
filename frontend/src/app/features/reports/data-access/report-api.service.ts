import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiUrlService } from '../../../core/http/api-url.service';
import {
  AnnualCashFlow,
  AnnualCompetenceReport,
  CompetenceReport,
  DailyCashFlow,
  MonthlyCashFlow,
  WeeklyCashFlow,
} from '../models/report.models';

@Injectable({ providedIn: 'root' })
export class ReportApiService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = inject(ApiUrlService);

  getDaily(date: string): Observable<DailyCashFlow> {
    return this.http.get<DailyCashFlow>(this.apiUrl.build('reports/cash/daily'), {
      params: new HttpParams().set('date', date),
    });
  }

  getWeekly(date: string): Observable<WeeklyCashFlow> {
    return this.http.get<WeeklyCashFlow>(this.apiUrl.build('reports/cash/weekly'), {
      params: new HttpParams().set('date', date),
    });
  }

  getMonthly(year: number, month: number): Observable<MonthlyCashFlow> {
    return this.http.get<MonthlyCashFlow>(this.apiUrl.build('reports/cash/monthly'), {
      params: new HttpParams().set('year', String(year)).set('month', String(month)),
    });
  }

  getAnnual(year: number): Observable<AnnualCashFlow> {
    return this.http.get<AnnualCashFlow>(this.apiUrl.build('reports/cash/annual'), {
      params: new HttpParams().set('year', String(year)),
    });
  }

  getCompetenceAnnual(year: number): Observable<AnnualCompetenceReport> {
    return this.http.get<AnnualCompetenceReport>(this.apiUrl.build('reports/competence/annual'), {
      params: new HttpParams().set('year', String(year)),
    });
  }

  getCompetence(startDate: string, endDate: string): Observable<CompetenceReport> {
    return this.http.get<CompetenceReport>(this.apiUrl.build('reports/competence'), {
      params: new HttpParams().set('startDate', startDate).set('endDate', endDate),
    });
  }
}
