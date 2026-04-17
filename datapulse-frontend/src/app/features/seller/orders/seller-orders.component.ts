import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { OrderService } from '../../../core/services/order.service';
import { Order, OrderStatus } from '../../../core/models/order.models';
import { StatusBadgeComponent } from '../../../shared/components/status-badge/status-badge.component';
import { SpinnerComponent } from '../../../shared/components/spinner/spinner.component';
import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';
import { NotificationService } from '../../../core/services/notification.service';

@Component({
  selector: 'app-seller-orders',
  standalone: true,
  imports: [CommonModule, FormsModule, StatusBadgeComponent, SpinnerComponent, PaginationComponent],
  template: `
    <section class="p-8">
      <header class="flex items-center justify-between">
        <div>
          <p class="text-xs font-bold uppercase tracking-wider text-primary">Fulfilment</p>
          <h1 class="mt-2 text-display-md text-text-primary">Orders</h1>
        </div>
        <select [ngModel]="statusFilter()" (ngModelChange)="applyFilter($event)"
                class="rounded-xl bg-background-sub px-4 py-2.5 text-sm font-semibold outline-none">
          <option value="">All statuses</option>
          <option value="pending">Pending</option>
          <option value="processing">Processing</option>
          <option value="shipped">Shipped</option>
          <option value="delivered">Delivered</option>
          <option value="cancelled">Cancelled</option>
        </select>
      </header>

      <app-spinner *ngIf="loading()"></app-spinner>

      <!-- Action-required banner so sellers see the queue length at a glance -->
      <div *ngIf="!loading() && needsActionCount() > 0 && currentPage() === 0"
           class="mt-6 flex items-center gap-3 rounded-2xl border border-warning/30 bg-warning/10 px-5 py-4 text-sm text-text-primary">
        <span class="material-symbols-outlined text-warning" style="font-size: 22px">priority_high</span>
        <p><span class="font-bold">{{ needsActionCount() }}</span> order{{ needsActionCount() === 1 ? '' : 's' }} on this page need your attention — shipping-ready and pending-confirmation orders are listed first.</p>
      </div>

      <div *ngIf="!loading()" class="mt-6 overflow-hidden rounded-3xl bg-background-raised shadow-atelier-sm">
        <table class="w-full">
          <thead>
            <tr class="border-b border-outline/60">
              <th class="px-6 py-4 text-left text-[11px] font-bold uppercase tracking-wider text-text-tertiary">Order</th>
              <th class="px-6 py-4 text-left text-[11px] font-bold uppercase tracking-wider text-text-tertiary">Customer</th>
              <th class="px-6 py-4 text-left text-[11px] font-bold uppercase tracking-wider text-text-tertiary">Items</th>
              <th class="px-6 py-4 text-left text-[11px] font-bold uppercase tracking-wider text-text-tertiary">Total</th>
              <th class="px-6 py-4 text-left text-[11px] font-bold uppercase tracking-wider text-text-tertiary">Status</th>
              <th class="px-6 py-4 text-right text-[11px] font-bold uppercase tracking-wider text-text-tertiary">Action</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let o of orders()"
                class="border-b border-outline/30 last:border-0 hover:bg-background-sub/50"
                [ngClass]="needsAction(o) ? 'bg-warning/5' : ''">
              <td class="px-6 py-4">
                <p class="font-mono text-sm font-bold text-text-primary">#{{ o.id }}</p>
                <p class="text-xs text-text-tertiary">{{ o.createdAt | date:'short' }}</p>
              </td>
              <td class="px-6 py-4 text-sm text-text-secondary">User #{{ o.userId }}</td>
              <td class="px-6 py-4 text-sm text-text-secondary">{{ o.items.length }} pieces</td>
              <td class="px-6 py-4 text-sm font-bold text-text-primary">\${{ o.grandTotal | number:'1.2-2' }}</td>
              <td class="px-6 py-4"><app-status-badge [status]="o.status"></app-status-badge></td>
              <td class="px-6 py-4 text-right">
                <div class="flex items-center justify-end gap-2">
                  <button *ngIf="o.status === 'pending'" (click)="confirmOrder(o)"
                          class="flex items-center gap-1 rounded-lg bg-success/10 px-3 py-2 text-xs font-semibold text-success hover:bg-success/20">
                    <span class="material-symbols-outlined" style="font-size: 14px">check_circle</span>Confirm
                  </button>
                  <button *ngIf="o.status === 'processing'" (click)="markShipped(o)"
                          class="btn-primary text-xs px-3 py-2">
                    <span class="material-symbols-outlined" style="font-size: 14px">local_shipping</span>Ship
                  </button>
                  <button *ngIf="o.status === 'pending' || o.status === 'processing'" (click)="cancelOrder(o)"
                          class="flex items-center gap-1 rounded-lg bg-danger/10 px-3 py-2 text-xs font-semibold text-danger hover:bg-danger/20">
                    <span class="material-symbols-outlined" style="font-size: 14px">close</span>Cancel
                  </button>
                </div>
              </td>
            </tr>
            <tr *ngIf="orders().length === 0">
              <td colspan="6" class="py-16 text-center text-sm text-text-tertiary">No orders match.</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div *ngIf="!loading() && totalPages() > 1" class="mt-6">
        <app-pagination [currentPage]="currentPage()" [totalPages]="totalPages()" [totalElements]="totalElements()" (pageChange)="onPageChange($event)"></app-pagination>
      </div>
    </section>
  `,
})
export class SellerOrdersComponent implements OnInit {
  private readonly orderService = inject(OrderService);
  private readonly toast = inject(NotificationService);

  readonly orders = signal<Order[]>([]);
  readonly loading = signal<boolean>(true);
  readonly statusFilter = signal<string>('');
  readonly currentPage = signal<number>(0);
  readonly totalPages = signal<number>(0);
  readonly totalElements = signal<number>(0);
  readonly pageSize = 15;

  readonly needsActionCount = computed(
    () => this.orders().filter((o) => this.needsAction(o)).length,
  );

  needsAction(o: Order): boolean {
    return o.status === 'pending' || o.status === 'processing';
  }

  ngOnInit(): void {
    this.fetch();
  }

  applyFilter(status: string): void {
    this.statusFilter.set(status);
    this.currentPage.set(0);
    this.fetch();
  }

  onPageChange(p: number): void {
    this.currentPage.set(p);
    this.fetch();
  }

  confirmOrder(order: Order): void {
    this.orderService.markProcessing(order.id).subscribe({
      next: () => {
        this.toast.success(`Order #${order.id} confirmed.`);
        this.fetch();
      },
      error: () => this.toast.error('Could not confirm order.'),
    });
  }

  markShipped(order: Order): void {
    this.orderService.markShipped(order.id).subscribe({
      next: () => {
        this.toast.success(`Order #${order.id} marked as shipped.`);
        this.fetch();
      },
      error: () => this.toast.error('Could not mark as shipped.'),
    });
  }

  cancelOrder(order: Order): void {
    this.orderService.cancel(order.id).subscribe({
      next: () => {
        this.toast.success(`Order #${order.id} cancelled.`);
        this.fetch();
      },
      error: () => this.toast.error('Could not cancel order.'),
    });
  }

  private fetch(): void {
    this.loading.set(true);
    const params: any = {
      page: this.currentPage(),
      size: this.pageSize,
      sort: 'createdAt,desc',
    };
    if (this.statusFilter()) params.status = this.statusFilter() as OrderStatus;
    this.orderService.listSellerOrders(params).subscribe({
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
}
