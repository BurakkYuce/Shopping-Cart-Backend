import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';

import {
  AnalyticsService,
  AnalyticsSales,
  AnalyticsProducts,
  TopProduct,
} from '../../../core/services/analytics.service';
import { SpinnerComponent } from '../../../shared/components/spinner/spinner.component';

@Component({
  selector: 'app-seller-analytics',
  standalone: true,
  imports: [CommonModule, SpinnerComponent],
  template: `
    <section class="p-8">
      <div>
        <p class="text-xs font-bold uppercase tracking-wider text-primary">Deeper insight</p>
        <h1 class="mt-2 text-display-md text-text-primary">Analytics</h1>
      </div>

      <app-spinner *ngIf="loading()"></app-spinner>

      <div *ngIf="!loading() && sales() as s" class="mt-8 grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
        <div class="rounded-2xl bg-background-raised p-6 shadow-atelier-sm">
          <p class="text-xs font-bold uppercase tracking-wider text-text-tertiary">Revenue</p>
          <p class="mt-3 text-3xl font-bold text-text-primary">\${{ s.totalRevenue | number:'1.2-2' }}</p>
        </div>
        <div class="rounded-2xl bg-background-raised p-6 shadow-atelier-sm">
          <p class="text-xs font-bold uppercase tracking-wider text-text-tertiary">Orders</p>
          <p class="mt-3 text-3xl font-bold text-text-primary">{{ s.orderCount }}</p>
        </div>
        <div class="rounded-2xl bg-background-raised p-6 shadow-atelier-sm">
          <p class="text-xs font-bold uppercase tracking-wider text-text-tertiary">Avg order value</p>
          <p class="mt-3 text-3xl font-bold text-text-primary">\${{ s.averageOrderValue | number:'1.2-2' }}</p>
        </div>
      </div>

      <div *ngIf="!loading() && topProducts().length > 0" class="mt-8 rounded-3xl bg-background-raised p-8 shadow-atelier-sm">
        <h3 class="text-xs font-bold uppercase tracking-wider text-text-primary">Top sellers</h3>
        <ul class="mt-4 space-y-3">
          <li *ngFor="let p of topProducts()" class="flex items-center justify-between rounded-xl bg-background-sub p-3">
            <div>
              <p class="text-sm font-semibold text-text-primary">{{ p.productName }}</p>
              <p class="text-xs text-text-tertiary">{{ p.totalQuantity }} sold</p>
            </div>
            <p class="text-sm font-semibold text-text-primary">\${{ p.totalRevenue | number:'1.2-2' }}</p>
          </li>
        </ul>
      </div>

      <div *ngIf="!loading()" class="mt-8 rounded-3xl bg-background-raised p-8 shadow-atelier-sm">
        <h3 class="text-xs font-bold uppercase tracking-wider text-text-primary">Ask the assistant</h3>
        <p class="mt-3 text-sm text-text-secondary">For deeper, conversational insight, open the DataPulse Assistant in the bottom-right corner. Ask things like "which category sold best last month?" or "how does my store compare to similar curators?"</p>
      </div>
    </section>
  `,
})
export class SellerAnalyticsComponent implements OnInit {
  private readonly analytics = inject(AnalyticsService);

  readonly sales = signal<AnalyticsSales | null>(null);
  readonly products = signal<AnalyticsProducts | null>(null);
  readonly loading = signal<boolean>(true);

  readonly topProducts = computed<TopProduct[]>(
    () => this.products()?.topSellingProducts?.slice(0, 5) ?? [],
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
    this.analytics.getProducts().subscribe({
      next: (p) => this.products.set(p),
      error: () => this.products.set({ topSellingProducts: [], avgRatingByCategory: {} }),
    });
  }
}
