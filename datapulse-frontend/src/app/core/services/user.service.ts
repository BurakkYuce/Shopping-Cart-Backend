import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { User, CustomerProfile, UpdateProfileRequest } from '../models/user.models';

@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.apiUrl;

  /* ---------- Current user profile ---------- */
  getMyProfile(): Observable<CustomerProfile> {
    return this.http.get<CustomerProfile>(`${this.baseUrl}/customer-profiles/me`);
  }

  updateMyProfile(body: UpdateProfileRequest): Observable<CustomerProfile> {
    return this.http.put<CustomerProfile>(`${this.baseUrl}/customer-profiles/me`, body);
  }

  getMe(): Observable<User> {
    return this.http.get<User>(`${this.baseUrl}/users/me`);
  }

  changePassword(currentPassword: string, newPassword: string): Observable<void> {
    return this.http.put<void>(`${this.baseUrl}/users/me/password`, { currentPassword, newPassword });
  }

  getUserById(id: string): Observable<User> {
    return this.http.get<User>(`${this.baseUrl}/users/${id}`);
  }

  /* ---------- Admin: user management ---------- */
  listUsers(): Observable<User[]> {
    // Backend returns plain List<UserResponse> with no pagination/filters.
    return this.http.get<User[]>(`${this.baseUrl}/users`);
  }
}
