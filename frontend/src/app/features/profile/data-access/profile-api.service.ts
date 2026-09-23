import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiUrlService } from '../../../core/http/api-url.service';
import { User } from '../../../shared/models/user.models';
import {
  ChangePasswordRequest,
  ConfirmCurrentPasswordRequest,
  UpdateProfileRequest,
} from '../models/profile.models';

@Injectable({ providedIn: 'root' })
export class ProfileApiService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = inject(ApiUrlService);

  getCurrentUser(): Observable<User> {
    return this.http.get<User>(this.apiUrl.build('users/me'));
  }

  updateCurrentUser(request: UpdateProfileRequest): Observable<User> {
    return this.http.patch<User>(this.apiUrl.build('users/me'), request);
  }

  changePassword(request: ChangePasswordRequest): Observable<void> {
    return this.http.patch<void>(this.apiUrl.build('users/me/password'), request);
  }

  deleteCurrentUser(request: ConfirmCurrentPasswordRequest): Observable<void> {
    return this.http.delete<void>(this.apiUrl.build('users/me'), { body: request });
  }
}
