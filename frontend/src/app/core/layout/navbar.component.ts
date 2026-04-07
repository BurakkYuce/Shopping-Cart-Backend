import { CommonModule } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  template: `
    <header class="navbar">
      <div class="navbar__brand">
        <a routerLink="/" class="brand-link">DataPulse</a>
      </div>

      <nav class="navbar__nav">
        <a routerLink="/" routerLinkActive="active" [routerLinkActiveOptions]="{ exact: true }">Home</a>
        <a routerLink="/products" routerLinkActive="active">Products</a>

        @if (isLoggedIn()) {
          <a routerLink="/cart" routerLinkActive="active">Cart</a>
          <a routerLink="/wishlist" routerLinkActive="active">Wishlist</a>
          <a routerLink="/orders" routerLinkActive="active">Orders</a>
          <a routerLink="/profile" routerLinkActive="active">Profile</a>
          <a routerLink="/addresses" routerLinkActive="active">Addresses</a>
          <a routerLink="/chat" routerLinkActive="active">Chat</a>
          <a routerLink="/dashboard/me" routerLinkActive="active">My Dashboard</a>
        }

        @if (isCorporate()) {
          <a routerLink="/dashboard/corporate" routerLinkActive="active">Corporate Dashboard</a>
          <a routerLink="/corporate" routerLinkActive="active">Stores</a>
          <a routerLink="/corporate/inventory" routerLinkActive="active">Inventory</a>
          <a routerLink="/corporate/orders" routerLinkActive="active">Manage Orders</a>
        }

        @if (isAdmin()) {
          <a routerLink="/dashboard/admin" routerLinkActive="active">Admin Dashboard</a>
          <a routerLink="/admin/users" routerLinkActive="active">Users</a>
          <a routerLink="/admin/stores" routerLinkActive="active">Stores</a>
          <a routerLink="/admin/categories" routerLinkActive="active">Categories</a>
        }
      </nav>

      <div class="navbar__actions">
        @if (!isLoggedIn()) {
          <a routerLink="/login" class="btn btn--ghost">Login</a>
          <a routerLink="/register" class="btn">Register</a>
        } @else {
          <span class="role-badge">{{ currentRole() }}</span>
          <button class="btn" type="button" (click)="logout()">Logout</button>
        }
      </div>
    </header>
  `,
  styles: [`
    .navbar {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 1rem;
      padding: 1rem 1.5rem;
      border-bottom: 1px solid #e5e7eb;
      background: #ffffff;
      position: sticky;
      top: 0;
      z-index: 1000;
      flex-wrap: wrap;
    }

    .navbar__brand {
      display: flex;
      align-items: center;
    }

    .brand-link {
      text-decoration: none;
      font-size: 1.25rem;
      font-weight: 700;
      color: #111827;
    }

    .navbar__nav {
      display: flex;
      gap: 1rem;
      flex-wrap: wrap;
      align-items: center;
    }

    .navbar__nav a {
      text-decoration: none;
      color: #374151;
      font-weight: 500;
    }

    .navbar__nav a.active {
      color: #2563eb;
    }

    .navbar__actions {
      display: flex;
      align-items: center;
      gap: 0.75rem;
    }

    .btn {
      border: none;
      background: #2563eb;
      color: white;
      padding: 0.6rem 1rem;
      border-radius: 10px;
      cursor: pointer;
      text-decoration: none;
      font-weight: 600;
    }

    .btn--ghost {
      background: #eef2ff;
      color: #1d4ed8;
    }

    .role-badge {
      padding: 0.4rem 0.75rem;
      border-radius: 999px;
      background: #f3f4f6;
      color: #111827;
      font-size: 0.85rem;
      font-weight: 700;
    }
  `]
})
export class NavbarComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly currentRole = computed(() => this.authService.getUserRole());

  isLoggedIn(): boolean {
    return this.authService.isLoggedIn();
  }

  isAdmin(): boolean {
    return this.authService.isAdmin();
  }

  isCorporate(): boolean {
    return this.authService.isCorporate();
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/']);
  }
}
