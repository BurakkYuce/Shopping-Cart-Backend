import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

import {
  AnalyticsService,
  AnalyticsSales,
  AnalyticsCustomers,
} from '../../../core/services/analytics.service';
import { SpinnerComponent } from '../../../shared/components/spinner/spinner.component';

interface RevenueBar {
  period: string;
  value: number;
}

interface CityRow {
  name: string;
  count: number;
}

@Component({
  selector: 'app-admin-overview',
  standalone: true,
  imports: [CommonModule, RouterLink, SpinnerComponent],
  template: `
    <header class="sticky top-0 z-40 border-b border-outline/40 bg-background/90 px-12 py-6 backdrop-blur-xl">
      <div class="flex items-center gap-8">
        <span class="text-2xl font-bold tracking-tight text-text-primary">DataPulse</span>
        <h2 class="border-l border-outline/40 pl-8 text-xl font-semibold text-text-primary">Overview</h2>
      </div>
    </header>

    <section class="px-12 py-10">
      <app-spinner *ngIf="loading()"></app-spinner>

      <div *ngIf="!loading()" class="grid grid-cols-12 gap-6">
        <!-- Total revenue (8 cols) -->
        <div class="relative col-span-12 overflow-hidden rounded-3xl bg-background-sub p-8 lg:col-span-8">
          <div class="relative z-10">
            <h3 class="text-xs font-bold uppercase tracking-widest text-text-tertiary">Total marketplace revenue</h3>
            <p class="mt-2 text-5xl font-black tracking-tight text-text-primary">
              \${{ sales()?.totalRevenue ?? 0 | number:'1.2-2' }}
            </p>
          </div>
          <div class="mt-8 flex flex-wrap gap-3">
            <span class="rounded-full bg-background-raised px-4 py-1.5 text-[11px] font-bold uppercase tracking-wider text-text-secondary">
              {{ sales()?.orderCount ?? 0 | number }} Orders
            </span>
            <span class="rounded-full bg-background-raised px-4 py-1.5 text-[11px] font-bold uppercase tracking-wider text-text-tertiary">
              \${{ sales()?.averageOrderValue ?? 0 | number:'1.2-2' }} Avg order
            </span>
          </div>
          <div class="absolute -right-16 -bottom-16 h-56 w-56 rounded-full bg-primary/10 blur-3xl"></div>
        </div>

        <!-- Customer avg age (4 cols) -->
        <div class="col-span-12 flex flex-col justify-between rounded-3xl bg-primary p-8 text-white lg:col-span-4">
          <h3 class="text-xs font-bold uppercase tracking-widest text-white/70">Avg customer age</h3>
          <p class="text-6xl font-black tracking-tight">{{ customers()?.averageAge ?? 0 | number:'1.0-0' }}</p>
          <a routerLink="/admin/users" class="mt-4 flex items-center gap-2 text-sm font-semibold transition-all hover:gap-3">
            Browse users
            <span class="material-symbols-outlined" style="font-size: 18px">arrow_forward</span>
          </a>
        </div>

        <!-- Top cities -->
        <div class="col-span-12 grid grid-cols-1 gap-6 sm:grid-cols-3 lg:col-span-12 lg:grid-cols-3">
          <div *ngFor="let city of topCities()" class="rounded-2xl bg-background-raised p-6 shadow-atelier-sm">
            <div class="flex items-center justify-between">
              <p class="text-xs font-bold uppercase tracking-wider text-text-tertiary">{{ city.name }}</p>
              <span class="material-symbols-outlined text-primary">location_city</span>
            </div>
            <p class="mt-3 text-3xl font-bold text-text-primary">{{ city.count | number }}</p>
          </div>
          <div *ngIf="topCities().length === 0" class="col-span-3 rounded-2xl bg-background-raised p-6 text-center text-sm text-text-tertiary shadow-atelier-sm">
            No city data yet.
          </div>
        </div>

        <!-- Revenue chart -->
        <div class="col-span-12 rounded-3xl bg-background-raised p-8 shadow-atelier-sm">
          <div class="flex items-center justify-between">
            <div>
              <h3 class="text-xs font-bold uppercase tracking-widest text-text-primary">Revenue by day</h3>
              <p class="mt-1 text-xs text-text-tertiary">Last 14 periods</p>
            </div>
          </div>
          <div class="mt-6 flex h-48 items-end gap-4">
            <ng-container *ngIf="revenueBars().length > 0">
              <div *ngFor="let bar of revenueBars()" class="flex flex-1 flex-col items-center gap-2">
                <div class="w-full rounded-t-xl bg-primary transition-all hover:bg-primary-hover"
                     [style.height.%]="(bar.value / maxRevenue()) * 100"
                     [title]="bar.period + ': $' + bar.value"></div>
                <span class="text-[11px] text-text-tertiary">{{ bar.period }}</span>
              </div>
            </ng-container>
            <div *ngIf="revenueBars().length === 0" class="flex w-full flex-col items-center justify-center text-sm text-text-tertiary">
              <span class="material-symbols-outlined" style="font-size: 32px">bar_chart</span>
              <p class="mt-2">No revenue data yet</p>
            </div>
          </div>
        </div>
      </div>
    </section>
  `,
})
export class AdminOverviewComponent implements OnInit {
  private readonly analytics = inject(AnalyticsService);

  readonly sales = signal<AnalyticsSales | null>(null);
  readonly customers = signal<AnalyticsCustomers | null>(null);
  readonly loading = signal<boolean>(true);

  readonly revenueBars = computed<RevenueBar[]>(() => {
    const byDay = this.sales()?.revenueByDay ?? {};
    return Object.entries(byDay)
      .sort(([a], [b]) => a.localeCompare(b))
      .slice(-14)
      .map(([period, value]) => ({
        period: period.slice(5),
        value,
      }));
  });

  readonly maxRevenue = computed(
    () => Math.max(1, ...this.revenueBars().map((b) => b.value)),
  );

  readonly topCities = computed<CityRow[]>(() => {
    const cities = this.customers()?.topCities ?? {};
    return Object.entries(cities)
      .sort(([, a], [, b]) => b - a)
      .slice(0, 3)
      .map(([name, count]) => ({ name, count }));
  });

  ngOnInit(): void {
    this.analytics.getSales().subscribe({
      next: (s) => {
        this.sales.set(s);
        this.loading.set(false);
      },
      error: () => {
        this.sales.set({ totalRevenue: 0, orderCount: 0, averageOrderValue: 0, revenueByDay: {} });
        this.loading.set(false);
      },
    });
    this.analytics.getCustomers().subscribe({
      next: (c) => this.customers.set(c),
      error: () =>
        this.customers.set({
          averageAge: 0,
          spendByMembership: {},
          satisfactionDistribution: {},
          topCities: {},
        }),
    });
  }
}
