import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

import { OrderService } from '../../../core/services/order.service';
import { Order, OrderStatus } from '../../../core/models/order.models';
import { StatusBadgeComponent } from '../../../shared/components/status-badge/status-badge.component';
import { SpinnerComponent } from '../../../shared/components/spinner/spinner.component';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';
import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';

type TabFilter = 'all' | 'active' | 'delivered' | 'cancelled';

@Component({
  selector: 'app-orders-list',
  standalone: true,
  imports: [CommonModule, RouterLink, StatusBadgeComponent, SpinnerComponent, EmptyStateComponent, PaginationComponent],
  templateUrl: './orders-list.component.html',
})
export class OrdersListComponent implements OnInit {
  private readonly orderService = inject(OrderService);

  readonly orders = signal<Order[]>([]);
  readonly loading = signal<boolean>(true);
  readonly tab = signal<TabFilter>('all');
  readonly currentPage = signal<number>(0);
  readonly totalPages = signal<number>(0);
  readonly totalElements = signal<number>(0);
  readonly pageSize = 10;

  readonly tabs: { id: TabFilter; label: string }[] = [
    { id: 'all', label: 'All' },
    { id: 'active', label: 'Active' },
    { id: 'delivered', label: 'Delivered' },
    { id: 'cancelled', label: 'Cancelled' },
  ];

  ngOnInit(): void {
    this.fetch();
  }

  setTab(t: TabFilter): void {
    this.tab.set(t);
    this.currentPage.set(0);
    this.fetch();
  }

  onPageChange(p: number): void {
    this.currentPage.set(p);
    this.fetch();
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  private fetch(): void {
    this.loading.set(true);
    const params: any = {
      page: this.currentPage(),
      size: this.pageSize,
      sort: 'createdAt,desc',
    };
    const t = this.tab();
    if (t !== 'all') {
      if (t === 'active') params.status = 'pending';
      else params.status = t as OrderStatus;
    }
    this.orderService.list(params).subscribe({
      next: (res) => {
        this.orders.set(res.content);
        this.totalPages.set(res.totalPages);
        this.totalElements.set(res.totalElements);
        this.loading.set(false);
      },
      error: () => {
        this.orders.set([]);
        this.loading.set(false);
      },
    });
  }

  timelineStages(): { key: string; label: string; icon: string }[] {
    return [
      { key: 'pending', label: 'Confirmed', icon: 'check_circle' },
      { key: 'processing', label: 'Processing', icon: 'inventory_2' },
      { key: 'shipped', label: 'Shipped', icon: 'local_shipping' },
      { key: 'out_for_delivery', label: 'Out for delivery', icon: 'moped' },
      { key: 'delivered', label: 'Delivered', icon: 'home' },
    ];
  }

  stageIndex(order: Order): number {
    const stage = order.shipmentStatus ?? order.status;
    const order_stages = ['pending', 'processing', 'shipped', 'out_for_delivery', 'delivered'];
    return Math.max(0, order_stages.indexOf(stage as string));
  }
}
