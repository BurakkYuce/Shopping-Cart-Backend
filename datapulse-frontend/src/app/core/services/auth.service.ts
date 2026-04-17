import { Injectable, inject, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';

import { environment } from '../../../environments/environment';
import { ChatService } from './chat.service';
import {
  AuthResponse,
  LoginRequest,
  RegisterRequest,
  RefreshTokenRequest,
  ForgotPasswordRequest,
  ResetPasswordRequest,
  UserRole,
} from '../models/auth.models';

const ACCESS_TOKEN_KEY = 'dp_access_token';
const REFRESH_TOKEN_KEY = 'dp_refresh_token';
const USER_ROLE_KEY = 'dp_user_role';
const USER_ID_KEY = 'dp_user_id';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly chat = inject(ChatService);
  private readonly baseUrl = `${environment.apiUrl}/auth`;

  // Reactive auth state
  private readonly _isAuthenticated = signal<boolean>(this.hasValidToken());
  private readonly _userRole = signal<UserRole | null>(this.getUserRole());
  private readonly _userId = signal<string | null>(this.getUserId());

  readonly isAuthenticated$ = this._isAuthenticated.asReadonly();
  readonly userRole$ = this._userRole.asReadonly();
  readonly userId$ = this._userId.asReadonly();
  readonly isCustomer = computed(() => {
    const role = this._userRole();
    return role === 'INDIVIDUAL' || role === null;
  });
  readonly isSeller = computed(() => this._userRole() === 'CORPORATE');
  readonly isAdmin = computed(() => this._userRole() === 'ADMIN');

  /* ---------- Login / Register ---------- */
  login(credentials: LoginRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.baseUrl}/login`, credentials)
      .pipe(tap((res) => this.storeSession(res)));
  }

  register(data: RegisterRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.baseUrl}/register`, data)
      .pipe(tap((res) => this.storeSession(res)));
  }

  /* ---------- Token refresh ---------- */
  refreshToken(): Observable<AuthResponse> {
    const body: RefreshTokenRequest = { refreshToken: this.getRefreshToken() ?? '' };
    return this.http
      .post<AuthResponse>(`${this.baseUrl}/refresh`, body)
      .pipe(tap((res) => this.storeSession(res)));
  }

  /* ---------- Forgot / reset ---------- */
  forgotPassword(body: ForgotPasswordRequest): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/forgot-password`, body);
  }

  resetPassword(body: ResetPasswordRequest): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/reset-password`, body);
  }

  /* ---------- Logout ---------- */
  logout(): void {
    this.clearSession();
    this.router.navigate(['/auth/login']);
  }

  /* ---------- Storage helpers ---------- */
  storeSession(res: AuthResponse): void {
    localStorage.setItem(ACCESS_TOKEN_KEY, res.accessToken);
    localStorage.setItem(REFRESH_TOKEN_KEY, res.refreshToken);
    localStorage.setItem(USER_ROLE_KEY, res.userRole);
    localStorage.setItem(USER_ID_KEY, res.userId);

    this._isAuthenticated.set(true);
    this._userRole.set(res.userRole);
    this._userId.set(res.userId);
  }

  clearSession(): void {
    localStorage.removeItem(ACCESS_TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
    localStorage.removeItem(USER_ROLE_KEY);
    localStorage.removeItem(USER_ID_KEY);

    this._isAuthenticated.set(false);
    this._userRole.set(null);
    this._userId.set(null);
    this.chat.reset();
  }

  getAccessToken(): string | null {
    return typeof localStorage !== 'undefined' ? localStorage.getItem(ACCESS_TOKEN_KEY) : null;
  }

  getRefreshToken(): string | null {
    return typeof localStorage !== 'undefined' ? localStorage.getItem(REFRESH_TOKEN_KEY) : null;
  }

  getUserRole(): UserRole | null {
    if (typeof localStorage === 'undefined') return null;
    return localStorage.getItem(USER_ROLE_KEY) as UserRole | null;
  }

  getUserId(): string | null {
    if (typeof localStorage === 'undefined') return null;
    return localStorage.getItem(USER_ID_KEY);
  }

  isAuthenticated(): boolean {
    return this.hasValidToken();
  }

  private hasValidToken(): boolean {
    if (typeof localStorage === 'undefined') return false;
    return !!localStorage.getItem(ACCESS_TOKEN_KEY);
  }
}
