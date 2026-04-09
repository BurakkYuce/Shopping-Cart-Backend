import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { AuthService } from '../../../core/services/auth.service';
import { ToastComponent } from '../../../shared/components/toast/toast.component';

@Component({
  selector: 'app-admin-shell',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive, RouterOutlet, ToastComponent],
  template: `
    <div class="flex min-h-screen bg-background">
      <!-- Sidebar -->
      <aside class="fixed left-0 top-0 flex h-screen w-64 flex-col bg-background-sub p-6 text-sm">
        <div class="mb-10 px-2">
          <h1 class="text-lg font-bold tracking-tight text-text-primary">DataPulse Atelier</h1>
          <p class="mt-1 text-[10px] font-semibold uppercase tracking-widest text-text-tertiary">Admin console</p>
        </div>

        <nav class="flex-1 space-y-1">
          <a *ngFor="let n of nav"
             [routerLink]="n.path"
             [routerLinkActive]="'bg-background-raised text-primary shadow-atelier-sm font-semibold'"
             [routerLinkActiveOptions]="{exact: n.exact === true}"
             class="flex items-center gap-3 rounded-xl px-4 py-3 text-text-secondary transition-all hover:-translate-x-0 hover:translate-x-1 hover:bg-background-raised/50">
            <span class="material-symbols-outlined" style="font-size: 20px">{{ n.icon }}</span>
            {{ n.label }}
          </a>
        </nav>

        <div class="mt-auto border-t border-outline/40 pt-6">
          <div class="mb-4 flex items-center gap-3 px-2">
            <div class="flex h-10 w-10 items-center justify-center rounded-full bg-primary/10 text-primary">
              <span class="material-symbols-outlined filled" style="font-size: 20px">shield_person</span>
            </div>
            <div>
              <p class="text-sm font-semibold leading-none text-text-primary">Admin</p>
              <p class="mt-1 text-[10px] text-text-tertiary">Platform access</p>
            </div>
          </div>
          <button (click)="logout()" class="flex w-full items-center justify-center gap-2 rounded-xl bg-background-raised py-3 text-xs font-semibold uppercase tracking-widest text-danger hover:shadow-atelier-sm">
            <span class="material-symbols-outlined" style="font-size: 16px">logout</span>
            Sign out
          </button>
        </div>
      </aside>

      <!-- Main -->
      <main class="ml-64 flex-1 bg-background">
        <router-outlet />
      </main>
      <app-toast />
    </div>
  `,
})
export class AdminShellComponent {
  private readonly auth = inject(AuthService);

  readonly nav = [
    { path: '/admin', icon: 'dashboard', label: 'Overview', exact: true },
    { path: '/admin/users', icon: 'group', label: 'Users' },
    { path: '/admin/products', icon: 'inventory_2', label: 'Products' },
    { path: '/admin/orders', icon: 'receipt_long', label: 'Orders' },
    { path: '/admin/stores', icon: 'storefront', label: 'Stores' },
    { path: '/admin/analytics', icon: 'insights', label: 'Analytics' },
  ];

  logout(): void {
    this.auth.logout();
  }
}
