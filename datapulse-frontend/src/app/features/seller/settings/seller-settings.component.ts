import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { UserService } from '../../../core/services/user.service';
import { SpinnerComponent } from '../../../shared/components/spinner/spinner.component';

@Component({
  selector: 'app-seller-settings',
  standalone: true,
  imports: [CommonModule, FormsModule, SpinnerComponent],
  template: `
    <section class="p-8">
      <div>
        <p class="text-xs font-bold uppercase tracking-wider text-primary">Store settings</p>
        <h1 class="mt-2 text-display-md text-text-primary">Settings</h1>
      </div>

      <app-spinner *ngIf="loading()"></app-spinner>

      <div *ngIf="!loading()" class="mt-8 grid gap-6 lg:grid-cols-3">
        <!-- Left: nav -->
        <aside class="rounded-3xl bg-background-raised p-6 shadow-atelier-sm lg:col-span-1 h-fit">
          <nav class="space-y-1">
            <button *ngFor="let t of tabs" (click)="tab.set(t.key)"
                    class="flex w-full items-center gap-3 rounded-xl px-4 py-3 text-left text-sm font-semibold transition-colors"
                    [class.bg-background-accent]="tab() === t.key"
                    [class.text-primary]="tab() === t.key"
                    [class.text-text-secondary]="tab() !== t.key">
              <span class="material-symbols-outlined" style="font-size: 18px">{{ t.icon }}</span>
              {{ t.label }}
            </button>
          </nav>
        </aside>

        <!-- Right: content -->
        <div class="lg:col-span-2">
          <!-- Profile tab -->
          <div *ngIf="tab() === 'profile'" class="rounded-3xl bg-background-raised p-8 shadow-atelier-sm">
            <h2 class="text-display-sm text-text-primary">Business profile</h2>
            <p class="mt-1 text-sm text-text-secondary">Your account information.</p>

            <div class="mt-6 grid gap-4 sm:grid-cols-2">
              <div class="sm:col-span-2">
                <label class="label-base">Contact email</label>
                <input [value]="email()" type="email" disabled class="input-base opacity-60" />
              </div>
              <div>
                <label class="label-base">Role</label>
                <input [value]="role()" type="text" disabled class="input-base opacity-60" />
              </div>
            </div>
          </div>

          <!-- Shipping tab -->
          <div *ngIf="tab() === 'shipping'" class="rounded-3xl bg-background-raised p-8 shadow-atelier-sm">
            <h2 class="text-display-sm text-text-primary">Shipping defaults</h2>
            <p class="mt-1 text-sm text-text-secondary">Configure default carriers and fulfilment preferences.</p>
            <div class="mt-6 space-y-4">
              <div class="flex items-center justify-between rounded-2xl bg-background-sub p-5">
                <div>
                  <p class="text-sm font-semibold text-text-primary">Express delivery</p>
                  <p class="text-xs text-text-tertiary">Offer 1-2 day shipping on eligible pieces</p>
                </div>
                <button (click)="express.set(!express())" class="relative h-7 w-12 rounded-full transition-colors"
                        [class.bg-primary]="express()" [class.bg-outline]="!express()">
                  <span class="absolute top-0.5 h-6 w-6 rounded-full bg-white shadow transition-all"
                        [class.left-0.5]="!express()" [class.left-[22px]]="express()"></span>
                </button>
              </div>
              <div class="flex items-center justify-between rounded-2xl bg-background-sub p-5">
                <div>
                  <p class="text-sm font-semibold text-text-primary">Free shipping threshold</p>
                  <p class="text-xs text-text-tertiary">Orders above this value ship free</p>
                </div>
                <div class="flex items-center gap-2">
                  <span class="text-sm text-text-secondary">$</span>
                  <input type="number" [ngModel]="threshold()" (ngModelChange)="threshold.set($event)"
                         class="h-10 w-24 rounded-xl bg-background-raised px-3 text-sm outline-none" />
                </div>
              </div>
            </div>
          </div>

          <!-- Notifications tab -->
          <div *ngIf="tab() === 'notifications'" class="rounded-3xl bg-background-raised p-8 shadow-atelier-sm">
            <h2 class="text-display-sm text-text-primary">Notifications</h2>
            <p class="mt-1 text-sm text-text-secondary">Choose which events trigger alerts.</p>
            <div class="mt-6 space-y-3">
              <label *ngFor="let n of notifList" class="flex items-center justify-between rounded-2xl bg-background-sub p-5 cursor-pointer">
                <div>
                  <p class="text-sm font-semibold text-text-primary">{{ n.label }}</p>
                  <p class="text-xs text-text-tertiary">{{ n.hint }}</p>
                </div>
                <input type="checkbox" [checked]="n.enabled" (change)="n.enabled = !n.enabled"
                       class="h-5 w-5 accent-primary" />
              </label>
            </div>
          </div>

          <!-- Danger tab -->
          <div *ngIf="tab() === 'danger'" class="rounded-3xl bg-background-raised p-8 shadow-atelier-sm ring-1 ring-danger/20">
            <h2 class="text-display-sm text-danger">Danger zone</h2>
            <p class="mt-1 text-sm text-text-secondary">Actions in this area cannot be undone.</p>
            <div class="mt-6 flex items-center justify-between rounded-2xl bg-danger/5 p-5">
              <div>
                <p class="text-sm font-semibold text-text-primary">Deactivate store</p>
                <p class="text-xs text-text-tertiary">Temporarily remove your creations from the marketplace.</p>
              </div>
              <button class="rounded-xl border border-danger px-4 py-2 text-sm font-semibold text-danger hover:bg-danger/10">Deactivate</button>
            </div>
          </div>
        </div>
      </div>
    </section>
  `,
})
export class SellerSettingsComponent implements OnInit {
  private readonly userService = inject(UserService);

  readonly tab = signal<'profile' | 'shipping' | 'notifications' | 'danger'>('profile');
  readonly loading = signal<boolean>(true);
  readonly express = signal<boolean>(true);
  readonly threshold = signal<number>(100);
  readonly email = signal<string>('');
  readonly role = signal<string>('');

  readonly tabs = [
    { key: 'profile' as const, label: 'Business profile', icon: 'storefront' },
    { key: 'shipping' as const, label: 'Shipping', icon: 'local_shipping' },
    { key: 'notifications' as const, label: 'Notifications', icon: 'notifications' },
    { key: 'danger' as const, label: 'Danger zone', icon: 'warning' },
  ];

  readonly notifList = [
    { label: 'New order', hint: 'Email me when a customer places an order', enabled: true },
    { label: 'Low stock', hint: 'Alert when stock falls below threshold', enabled: true },
    { label: 'New review', hint: 'Notify me when a customer leaves a review', enabled: false },
    { label: 'Weekly digest', hint: 'Summary of your store performance', enabled: true },
  ];

  ngOnInit(): void {
    this.userService.getMe().subscribe({
      next: (user) => {
        this.email.set(user.email ?? '');
        this.role.set(user.roleType ?? '');
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }
}
