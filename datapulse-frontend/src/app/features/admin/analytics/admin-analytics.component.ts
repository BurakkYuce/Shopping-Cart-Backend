import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';

import {
  AnalyticsService,
  AnalyticsSales,
  AnalyticsCustomers,
  AnalyticsProducts,
} from '../../../core/services/analytics.service';
import { SpinnerComponent } from '../../../shared/components/spinner/spinner.component';

interface KV {
  key: string;
  value: number;
}

@Component({
  selector: 'app-admin-analytics',
  standalone: true,
  imports: [CommonModule, SpinnerComponent],
  template: `
    <header class="sticky top-0 z-40 flex items-center justify-between border-b border-outline/40 bg-background/90 px-12 py-6 backdrop-blur-xl">
      <div class="flex items-center gap-8">
        <span class="text-2xl font-bold tracking-tight text-text-primary">DataPulse</span>
        <h2 class="border-l border-outline/40 pl-8 text-xl font-semibold text-text-primary">Platform analytics</h2>
      </div>
    </header>

    <section class="px-12 py-10">
      <app-spinner *ngIf="loading()"></app-spinner>

      <div *ngIf="!loading()">
        <div class="grid gap-6 sm:grid-cols-2 lg:grid-cols-4">
          <div class="rounded-2xl bg-background-raised p-6 shadow-atelier-sm">
            <p class="text-xs font-bold uppercase tracking-wider text-text-tertiary">Total revenue</p>
            <p class="mt-3 text-3xl font-bold text-text-primary">\${{ sales()?.totalRevenue ?? 0 | number:'1.2-2' }}</p>
          </div>
          <div class="rounded-2xl bg-background-raised p-6 shadow-atelier-sm">
            <p class="text-xs font-bold uppercase tracking-wider text-text-tertiary">Orders</p>
            <p class="mt-3 text-3xl font-bold text-text-primary">{{ sales()?.orderCount ?? 0 | number }}</p>
          </div>
          <div class="rounded-2xl bg-background-raised p-6 shadow-atelier-sm">
            <p class="text-xs font-bold uppercase tracking-wider text-text-tertiary">Avg order value</p>
            <p class="mt-3 text-3xl font-bold text-text-primary">\${{ sales()?.averageOrderValue ?? 0 | number:'1.2-2' }}</p>
          </div>
          <div class="rounded-2xl bg-background-raised p-6 shadow-atelier-sm">
            <p class="text-xs font-bold uppercase tracking-wider text-text-tertiary">Avg customer age</p>
            <p class="mt-3 text-3xl font-bold text-text-primary">{{ customers()?.averageAge ?? 0 | number:'1.0-0' }}</p>
          </div>
        </div>

        <div class="mt-8 grid gap-6 lg:grid-cols-2">
          <div class="rounded-3xl bg-background-raised p-8 shadow-atelier-sm">
            <h3 class="text-xs font-bold uppercase tracking-widest text-text-primary">Spend by membership</h3>
            <ul class="mt-5 space-y-3">
              <li *ngFor="let row of spendByMembership()" class="flex items-center justify-between rounded-xl bg-background-sub p-3">
                <span class="text-sm font-semibold text-text-primary">{{ row.key }}</span>
                <span class="text-sm font-semibold text-text-primary">\${{ row.value | number:'1.2-2' }}</span>
              </li>
              <li *ngIf="spendByMembership().length === 0" class="py-6 text-center text-xs text-text-tertiary">
                No membership data yet.
              </li>
            </ul>
          </div>

          <div class="rounded-3xl bg-background-raised p-8 shadow-atelier-sm">
            <h3 class="text-xs font-bold uppercase tracking-widest text-text-primary">Top cities</h3>
            <ul class="mt-5 space-y-3">
              <li *ngFor="let row of topCities()" class="flex items-center justify-between rounded-xl bg-background-sub p-3">
                <span class="text-sm font-semibold text-text-primary">{{ row.key }}</span>
                <span class="text-sm font-semibold text-text-primary">{{ row.value | number }}</span>
              </li>
              <li *ngIf="topCities().length === 0" class="py-6 text-center text-xs text-text-tertiary">
                No city data yet.
              </li>
            </ul>
          </div>
        </div>

        <div class="mt-8 rounded-3xl bg-background-raised p-8 shadow-atelier-sm">
          <h3 class="text-xs font-bold uppercase tracking-widest text-text-primary">Avg rating by category</h3>
          <ul class="mt-5 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
            <li *ngFor="let row of ratingByCategory()" class="flex items-center justify-between rounded-xl bg-background-sub p-3">
              <span class="text-sm font-semibold text-text-primary">{{ row.key }}</span>
              <span class="text-sm font-semibold text-text-primary">{{ row.value | number:'1.1-1' }}</span>
            </li>
            <li *ngIf="ratingByCategory().length === 0" class="py-6 text-center text-xs text-text-tertiary sm:col-span-2 lg:col-span-3">
              No rating data yet.
            </li>
          </ul>
        </div>

        <div class="mt-8 rounded-3xl bg-background-raised p-8 shadow-atelier-sm">
          <h3 class="text-xs font-bold uppercase tracking-widest text-text-primary">Ask the assistant</h3>
          <p class="mt-3 text-sm text-text-secondary">For deeper, conversational insights across the entire platform, open the DataPulse Assistant. Ask things like "what were the top 5 performing curators last quarter?" or "which categories have the highest return rate?"</p>
        </div>
      </div>
    </section>
  `,
})
export class AdminAnalyticsComponent implements OnInit {
  private readonly analytics = inject(AnalyticsService);

  readonly sales = signal<AnalyticsSales | null>(null);
  readonly customers = signal<AnalyticsCustomers | null>(null);
  readonly products = signal<AnalyticsProducts | null>(null);
  readonly loading = signal<boolean>(true);

  readonly spendByMembership = computed<KV[]>(() =>
    Object.entries(this.customers()?.spendByMembership ?? {})
      .sort(([, a], [, b]) => b - a)
      .map(([key, value]) => ({ key, value })),
  );

  readonly topCities = computed<KV[]>(() =>
    Object.entries(this.customers()?.topCities ?? {})
      .sort(([, a], [, b]) => b - a)
      .slice(0, 8)
      .map(([key, value]) => ({ key, value })),
  );

  readonly ratingByCategory = computed<KV[]>(() =>
    Object.entries(this.products()?.avgRatingByCategory ?? {})
      .sort(([, a], [, b]) => b - a)
      .map(([key, value]) => ({ key, value })),
  );

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
    this.analytics.getProducts().subscribe({
      next: (p) => this.products.set(p),
      error: () => this.products.set({ topSellingProducts: [], avgRatingByCategory: {} }),
    });
  }
}
