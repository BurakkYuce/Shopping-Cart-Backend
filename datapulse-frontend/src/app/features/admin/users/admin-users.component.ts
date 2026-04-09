import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { UserService } from '../../../core/services/user.service';
import { NotificationService } from '../../../core/services/notification.service';
import { User } from '../../../core/models/user.models';
import { UserRole } from '../../../core/models/auth.models';
import { SpinnerComponent } from '../../../shared/components/spinner/spinner.component';

type UserTab = 'all' | 'admin' | 'corporate' | 'individual';

@Component({
  selector: 'app-admin-users',
  standalone: true,
  imports: [CommonModule, FormsModule, SpinnerComponent],
  template: `
    <!-- Top bar -->
    <header class="sticky top-0 z-40 flex items-center justify-between border-b border-outline/40 bg-background/90 px-12 py-6 backdrop-blur-xl">
      <div class="flex items-center gap-8">
        <span class="text-2xl font-bold tracking-tight text-text-primary">DataPulse</span>
        <h2 class="border-l border-outline/40 pl-8 text-xl font-semibold text-text-primary">User Management</h2>
      </div>
      <div class="flex items-center gap-4">
        <div class="relative">
          <span class="material-symbols-outlined pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-text-tertiary" style="font-size: 18px">search</span>
          <input type="search"
                 [ngModel]="query()"
                 (ngModelChange)="query.set($event)"
                 placeholder="Search by email..."
                 class="h-10 w-72 rounded-full bg-background-sub pl-11 pr-4 text-sm outline-none focus:w-80 focus:bg-background-raised" />
        </div>
      </div>
    </header>

    <section class="px-12 py-10">
      <!-- Stats overview -->
      <div class="mb-10 grid grid-cols-12 gap-6">
        <div class="relative col-span-12 overflow-hidden rounded-3xl bg-background-sub p-8 lg:col-span-8">
          <div class="relative z-10">
            <h3 class="text-xs font-bold uppercase tracking-widest text-text-tertiary">Total registered members</h3>
            <p class="mt-2 text-4xl font-black tracking-tight text-text-primary">
              {{ allUsers().length | number }}
            </p>
          </div>
          <div class="mt-8 flex gap-3">
            <span class="rounded-full bg-background-raised px-4 py-1.5 text-[11px] font-bold uppercase tracking-wider text-text-secondary">
              {{ adminCount() | number }} Admin
            </span>
            <span class="rounded-full bg-background-raised px-4 py-1.5 text-[11px] font-bold uppercase tracking-wider text-text-secondary">
              {{ corporateCount() | number }} Corporate
            </span>
            <span class="rounded-full bg-background-raised px-4 py-1.5 text-[11px] font-bold uppercase tracking-wider text-text-tertiary">
              {{ individualCount() | number }} Individual
            </span>
          </div>
          <div class="absolute -right-16 -bottom-16 h-56 w-56 rounded-full bg-primary/10 blur-3xl"></div>
        </div>
        <div class="col-span-12 flex flex-col justify-between rounded-3xl bg-primary p-8 text-white lg:col-span-4">
          <h3 class="text-xs font-bold uppercase tracking-widest text-white/70">Corporate sellers</h3>
          <p class="text-6xl font-black tracking-tight">{{ corporateCount() }}</p>
          <button (click)="applyTab('corporate')" class="mt-4 flex items-center gap-2 text-sm font-semibold transition-all hover:gap-3">
            View sellers
            <span class="material-symbols-outlined" style="font-size: 18px">arrow_forward</span>
          </button>
        </div>
      </div>

      <!-- User table -->
      <div class="overflow-hidden rounded-[2rem] bg-background-raised shadow-atelier">
        <div class="flex items-center justify-between border-b border-outline/40 px-8 py-6">
          <div class="flex gap-2">
            <button *ngFor="let t of tabs"
                    (click)="applyTab(t.key)"
                    class="rounded-xl px-4 py-2 text-sm font-semibold transition-all"
                    [class.border-b-2]="tab() === t.key"
                    [class.border-primary]="tab() === t.key"
                    [class.text-primary]="tab() === t.key"
                    [class.text-text-tertiary]="tab() !== t.key">
              {{ t.label }}
            </button>
          </div>
        </div>

        <app-spinner *ngIf="loading()"></app-spinner>

        <table *ngIf="!loading()" class="w-full border-collapse text-left">
          <thead>
            <tr class="border-b border-outline/30 text-[10px] font-bold uppercase tracking-[0.15em] text-text-tertiary">
              <th class="px-8 py-5">Member</th>
              <th class="px-6 py-5">Email</th>
              <th class="px-6 py-5">Role</th>
              <th class="px-6 py-5">Gender</th>
              <th class="px-8 py-5 text-right">User ID</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-outline/20">
            <tr *ngFor="let u of filteredUsers()" class="group transition-colors hover:bg-background-sub/50">
              <td class="px-8 py-5">
                <div class="flex items-center gap-4">
                  <div class="flex h-10 w-10 items-center justify-center rounded-xl bg-primary/10 text-primary ring-2 ring-white shadow-sm">
                    <span class="text-sm font-bold">{{ initials(u) }}</span>
                  </div>
                  <div>
                    <p class="text-sm font-semibold text-text-primary">{{ u.email.split('@')[0] }}</p>
                  </div>
                </div>
              </td>
              <td class="px-6 py-5">
                <p class="text-sm text-text-secondary">{{ u.email }}</p>
              </td>
              <td class="px-6 py-5">
                <span class="rounded-full px-3 py-1 text-[10px] font-black uppercase"
                      [ngClass]="rolePillClasses(u.roleType)">{{ u.roleType }}</span>
              </td>
              <td class="px-6 py-5">
                <p class="text-xs font-medium text-text-tertiary">{{ u.gender || '—' }}</p>
              </td>
              <td class="px-8 py-5 text-right">
                <code class="rounded-md bg-background-sub px-2 py-1 text-[10px] font-mono text-text-tertiary">{{ u.id }}</code>
              </td>
            </tr>
            <tr *ngIf="filteredUsers().length === 0">
              <td colspan="5" class="py-16 text-center text-sm text-text-tertiary">No users match the current filter.</td>
            </tr>
          </tbody>
        </table>

        <div class="flex items-center justify-between bg-background-raised px-8 py-6 text-xs font-semibold uppercase tracking-widest text-text-tertiary">
          <div>Showing {{ filteredUsers().length }} of {{ allUsers().length | number }} users</div>
        </div>
      </div>
    </section>
  `,
})
export class AdminUsersComponent implements OnInit {
  private readonly userService = inject(UserService);
  private readonly toast = inject(NotificationService);

  readonly allUsers = signal<User[]>([]);
  readonly loading = signal<boolean>(true);
  readonly query = signal<string>('');
  readonly tab = signal<UserTab>('all');

  readonly tabs: Array<{ key: UserTab; label: string }> = [
    { key: 'all', label: 'All users' },
    { key: 'admin', label: 'Admin' },
    { key: 'corporate', label: 'Corporate' },
    { key: 'individual', label: 'Individual' },
  ];

  readonly adminCount = computed(() => this.allUsers().filter((u) => u.roleType === 'ADMIN').length);
  readonly corporateCount = computed(() => this.allUsers().filter((u) => u.roleType === 'CORPORATE').length);
  readonly individualCount = computed(() => this.allUsers().filter((u) => u.roleType === 'INDIVIDUAL').length);

  readonly filteredUsers = computed(() => {
    const q = this.query().trim().toLowerCase();
    const tab = this.tab();
    return this.allUsers().filter((u) => {
      if (tab === 'admin' && u.roleType !== 'ADMIN') return false;
      if (tab === 'corporate' && u.roleType !== 'CORPORATE') return false;
      if (tab === 'individual' && u.roleType !== 'INDIVIDUAL') return false;
      if (q && !u.email.toLowerCase().includes(q)) return false;
      return true;
    });
  });

  ngOnInit(): void {
    this.fetch();
  }

  fetch(): void {
    this.loading.set(true);
    this.userService.listUsers().subscribe({
      next: (users) => {
        this.allUsers.set(users);
        this.loading.set(false);
      },
      error: () => {
        this.allUsers.set([]);
        this.loading.set(false);
        this.toast.error('Could not load users.');
      },
    });
  }

  applyTab(t: UserTab): void {
    this.tab.set(t);
  }

  initials(u: User): string {
    return u.email.slice(0, 2).toUpperCase();
  }

  rolePillClasses(role: UserRole): Record<string, boolean> {
    return {
      'bg-primary/10 text-primary': role === 'ADMIN',
      'bg-warning/10 text-warning': role === 'CORPORATE',
      'bg-success/10 text-success': role === 'INDIVIDUAL',
    };
  }
}
