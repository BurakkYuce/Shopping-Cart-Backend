import { Injectable, computed, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AuthResponse,
  ForgotPasswordRequest,
  LoginRequest,
  RefreshTokenRequest,
  RegisterRequest,
  ResetPasswordRequest,
  UserRole
} from '../models/auth.model';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly baseUrl = `${environment.apiUrl}/auth`;

  private readonly roleSignal = signal<UserRole | null>(this.getStoredRole());
  private readonly userIdSignal = signal<string | null>(this.getStoredUserId());
  private readonly loggedInSignal = signal<boolean>(this.hasAccessToken());

  readonly currentRole = computed(() => this.roleSignal());
  readonly currentUserId = computed(() => this.userIdSignal());
  readonly isAuthenticated = computed(() => this.loggedInSignal());

  constructor(private http: HttpClient) {}

  login(payload: LoginRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.baseUrl}/login`, payload)
      .pipe(tap((response) => this.storeAuth(response)));
  }

  register(payload: RegisterRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.baseUrl}/register`, payload)
      .pipe(tap((response) => this.storeAuth(response)));
  }

  refreshAccessToken(): Observable<AuthResponse> {
    const refreshToken = this.getRefreshToken();

    const payload: RefreshTokenRequest = {
      refreshToken: refreshToken ?? ''
    };

    return this.http
      .post<AuthResponse>(`${this.baseUrl}/refresh`, payload)
      .pipe(tap((response) => this.storeAuth(response)));
  }

  forgotPassword(email: string): Observable<unknown> {
    const payload: ForgotPasswordRequest = { email };
    return this.http.post(`${this.baseUrl}/forgot-password`, payload);
  }

  resetPassword(token: string, newPassword: string): Observable<unknown> {
    const payload: ResetPasswordRequest = { token, newPassword };
    return this.http.post(`${this.baseUrl}/reset-password`, payload);
  }

  logout(): void {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('userRole');
    localStorage.removeItem('userId');

    this.loggedInSignal.set(false);
    this.roleSignal.set(null);
    this.userIdSignal.set(null);
  }

  getAccessToken(): string | null {
    return localStorage.getItem('accessToken');
  }

  getRefreshToken(): string | null {
    return localStorage.getItem('refreshToken');
  }

  getUserRole(): UserRole | null {
    return this.roleSignal();
  }

  getUserId(): string | null {
    return this.userIdSignal();
  }

  isLoggedIn(): boolean {
    return this.loggedInSignal();
  }

  isAdmin(): boolean {
    return this.getUserRole() === 'ADMIN';
  }

  isCorporate(): boolean {
    return this.getUserRole() === 'CORPORATE';
  }

  isIndividual(): boolean {
    return this.getUserRole() === 'INDIVIDUAL';
  }

  private storeAuth(response: AuthResponse): void {
    localStorage.setItem('accessToken', response.accessToken);
    localStorage.setItem('refreshToken', response.refreshToken);
    localStorage.setItem('userRole', response.userRole);
    localStorage.setItem('userId', response.userId);

    this.loggedInSignal.set(true);
    this.roleSignal.set(response.userRole);
    this.userIdSignal.set(response.userId);
  }

  private hasAccessToken(): boolean {
    return !!localStorage.getItem('accessToken');
  }

  private getStoredRole(): UserRole | null {
    const role = localStorage.getItem('userRole');
    if (role === 'ADMIN' || role === 'CORPORATE' || role === 'INDIVIDUAL') {
      return role;
    }
    return null;
  }

  private getStoredUserId(): string | null {
    return localStorage.getItem('userId');
  }
}
