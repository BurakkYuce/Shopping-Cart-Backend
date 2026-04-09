import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

import { AnalyticsService, AnalyticsSales } from '../../../core/services/analytics.service';
import { OrderService } from '../../../core/services/order.service';
import { Order } from '../../../core/models/order.models';
import { StatusBadgeComponent } from '../../../shared/components/status-badge/status-badge.component';
import { SpinnerComponent } from '../../../shared/components/spinner/spinner.component';

interface RevenueBar {
  period: string;
  value: number;
}

@Component({
  selector: 'app-seller-overview',
  standalone: true,
  imports: [CommonModule, RouterLink, StatusBadgeComponent, SpinnerComponent],
  templateUrl: './overview.component.html',
})
export class SellerOverviewComponent implements OnInit {
  private readonly analytics = inject(AnalyticsService);
  private readonly orderService = inject(OrderService);

  readonly sales = signal<AnalyticsSales | null>(null);
  readonly recentOrders = signal<Order[]>([]);
  readonly loading = signal<boolean>(true);
  readonly todayLabel = new Date().toLocaleDateString('en-US', { month: 'long', day: 'numeric', year: 'numeric' });

  readonly revenueBars = computed<RevenueBar[]>(() => {
    const byDay = this.sales()?.revenueByDay ?? {};
    return Object.entries(byDay)
      .sort(([a], [b]) => a.localeCompare(b))
      .slice(-7)
      .map(([period, value]) => ({
        period: period.slice(5), // MM-DD
        value,
      }));
  });

  readonly pendingOrders = computed(
    () => this.recentOrders().filter((o) => o.status === 'pending').length,
  );

  ngOnInit(): void {
    this.analytics.getSales().subscribe({
      next: (s) => {
        this.sales.set(s);
        this.loading.set(false);
      },
      error: () => {
        this.sales.set({
          totalRevenue: 0,
          orderCount: 0,
          averageOrderValue: 0,
          revenueByDay: {},
        });
        this.loading.set(false);
      },
    });
    this.orderService.listSellerOrders({ size: 5, sort: 'createdAt,desc' }).subscribe({
      next: (res) => this.recentOrders.set(res.content),
      error: () => this.recentOrders.set([]),
    });
  }

  maxRevenue(): number {
    return this.revenueBars().reduce((max, b) => Math.max(max, b.value), 0) || 1;
  }
}
