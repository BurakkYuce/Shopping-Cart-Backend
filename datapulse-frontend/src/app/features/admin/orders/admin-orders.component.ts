import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { OrderService } from '../../../core/services/order.service';
import { Order, OrderStatus } from '../../../core/models/order.models';
import { StatusBadgeComponent } from '../../../shared/components/status-badge/status-badge.component';
import { SpinnerComponent } from '../../../shared/components/spinner/spinner.component';
import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';

@Component({
  selector: 'app-admin-orders',
  standalone: true,
  imports: [CommonModule, FormsModule, StatusBadgeComponent, SpinnerComponent, PaginationComponent],
  template: `
    <header class="sticky top-0 z-40 flex items-center justify-between border-b border-outline/40 bg-background/90 px-12 py-6 backdrop-blur-xl">
      <div class="flex items-center gap-8">
        <span class="text-2xl font-bold tracking-tight text-text-primary">DataPulse</span>
        <h2 class="border-l border-outline/40 pl-8 text-xl font-semibold text-text-primary">Order stream</h2>
      </div>
      <select [ngModel]="statusFilter()" (ngModelChange)="applyFilter($event)"
              class="rounded-xl bg-background-sub px-4 py-2.5 text-sm font-semibold outline-none">
        <option value="">All statuses</option>
        <option value="pending">Pending</option>
        <option value="confirmed">Confirmed</option>
        <option value="shipped">Shipped</option>
        <option value="delivered">Delivered</option>
        <option value="cancelled">Cancelled</option>
        <option value="return_requested">Return requested</option>
      </select>
    </header>

    <section class="px-12 py-10">
      <app-spinner *ngIf="loading()"></app-spinner>

      <div *ngIf="!loading()" class="overflow-hidden rounded-[2rem] bg-background-raised shadow-atelier">
        <table class="w-full text-left">
          <thead>
            <tr class="border-b border-outline/30 text-[10px] font-bold uppercase tracking-[0.15em] text-text-tertiary">
              <th class="px-8 py-5">Order</th>
              <th class="px-6 py-5">Customer</th>
              <th class="px-6 py-5">Items</th>
              <th class="px-6 py-5">Total</th>
              <th class="px-8 py-5">Status</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-outline/20">
            <tr *ngFor="let o of orders()" class="hover:bg-background-sub/50">
              <td class="px-8 py-5">
                <p class="font-mono text-sm font-bold text-text-primary">#{{ o.id }}</p>
                <p class="text-xs text-text-tertiary">{{ o.createdAt | date:'mediumDate' }}</p>
              </td>
              <td class="px-6 py-5 text-sm text-text-secondary">User #{{ o.userId }}</td>
              <td class="px-6 py-5 text-sm text-text-secondary">{{ o.items.length }} pieces</td>
              <td class="px-6 py-5 text-sm font-bold text-text-primary">\${{ o.grandTotal | number:'1.2-2' }}</td>
              <td class="px-8 py-5"><app-status-badge [status]="o.status"></app-status-badge></td>
            </tr>
            <tr *ngIf="orders().length === 0">
              <td colspan="5" class="py-16 text-center text-sm text-text-tertiary">No orders match.</td>
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
export class AdminOrdersComponent implements OnInit {
  private readonly orderService = inject(OrderService);

  readonly orders = signal<Order[]>([]);
  readonly loading = signal<boolean>(true);
  readonly statusFilter = signal<string>('');
  readonly currentPage = signal<number>(0);
  readonly totalPages = signal<number>(0);
  readonly totalElements = signal<number>(0);
  readonly pageSize = 15;

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

  private fetch(): void {
    this.loading.set(true);
    const params: { page: number; size: number; sort: string; status?: OrderStatus } = {
      page: this.currentPage(),
      size: this.pageSize,
      sort: 'createdAt,desc',
    };
    if (this.statusFilter()) params.status = this.statusFilter() as OrderStatus;
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
}
