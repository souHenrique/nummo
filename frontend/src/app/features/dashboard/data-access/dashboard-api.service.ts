import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiUrlService } from '../../../core/http/api-url.service';
import { AnnualCompetenceReport, CompetenceReport } from '../../reports/models/report.models';
import { Dashboard } from '../models/dashboard.models';

@Injectable({ providedIn: 'root' })
export class DashboardApiService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = inject(ApiUrlService);

  get(): Observable<Dashboard> {
    return this.http.get<Dashboard>(this.apiUrl.build('dashboard'));
  }

  getMonthlyChart(year: number, month: number): Observable<CompetenceReport> {
    return this.http.get<CompetenceReport>(this.apiUrl.build('dashboard/charts/monthly'), {
      params: new HttpParams().set('year', String(year)).set('month', String(month)),
    });
  }

  getAnnualChart(year: number): Observable<AnnualCompetenceReport> {
    return this.http.get<AnnualCompetenceReport>(this.apiUrl.build('dashboard/charts/annual'), {
      params: new HttpParams().set('year', String(year)),
    });
  }
}
