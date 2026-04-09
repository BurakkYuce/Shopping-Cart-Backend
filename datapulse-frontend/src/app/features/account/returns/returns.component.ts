import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

import { OrderService } from '../../../core/services/order.service';
import { Order } from '../../../core/models/order.models';
import { SpinnerComponent } from '../../../shared/components/spinner/spinner.component';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';
import { StatusBadgeComponent } from '../../../shared/components/status-badge/status-badge.component';

@Component({
  selector: 'app-returns',
  standalone: true,
  imports: [CommonModule, RouterLink, SpinnerComponent, EmptyStateComponent, StatusBadgeComponent],
  template: `
    <section class="mx-auto max-w-4xl px-4 py-10 sm:px-6 lg:px-8">
      <h1 class="text-display-md text-text-primary">Returns</h1>
      <p class="mt-1 text-sm text-text-secondary">Track the status of pieces you've sent back.</p>

      <app-spinner *ngIf="loading()"></app-spinner>
      <app-empty-state *ngIf="!loading() && returns().length === 0"
                       icon="assignment_return" title="No returns" message="Requested returns will appear here."></app-empty-state>

      <div *ngIf="!loading() && returns().length > 0" class="mt-8 space-y-4">
        <article *ngFor="let o of returns()" class="rounded-2xl bg-background-raised p-6 shadow-atelier-sm">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-[11px] font-bold uppercase tracking-wider text-text-tertiary">Order</p>
              <p class="mt-1 font-mono text-sm font-bold text-text-primary">#{{ o.id }}</p>
            </div>
            <app-status-badge [status]="o.status"></app-status-badge>
          </div>
          <div class="mt-4 flex items-center gap-3">
            <div class="flex -space-x-3">
              <div *ngFor="let item of o.items.slice(0,3)" class="h-14 w-14 overflow-hidden rounded-xl border-4 border-background-raised bg-background-sub">
                <img *ngIf="item.imageUrl" [src]="item.imageUrl" [alt]="item.productName" class="h-full w-full object-cover" />
              </div>
            </div>
            <div class="flex-1">
              <p class="text-sm text-text-secondary">{{ o.items.length }} items · \${{ o.grandTotal | number:'1.2-2' }}</p>
            </div>
            <a [routerLink]="['/orders', o.id]" class="btn-ghost text-sm">Details</a>
          </div>
        </article>
      </div>
    </section>
  `,
})
export class ReturnsComponent implements OnInit {
  private readonly orderService = inject(OrderService);

  readonly loading = signal<boolean>(true);
  readonly returns = signal<Order[]>([]);

  ngOnInit(): void {
    this.orderService.list({ size: 50, sort: 'createdAt,desc' }).subscribe({
      next: (res) => {
        this.returns.set(res.content.filter((o) => o.status === 'return_requested' || o.status === 'returned'));
        this.loading.set(false);
      },
      error: () => {
        this.returns.set([]);
        this.loading.set(false);
      },
    });
  }
}
