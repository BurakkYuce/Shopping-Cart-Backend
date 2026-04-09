import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

import { OrderService } from '../../../core/services/order.service';
import { NotificationService } from '../../../core/services/notification.service';
import { Order } from '../../../core/models/order.models';
import { SpinnerComponent } from '../../../shared/components/spinner/spinner.component';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';

interface NotificationEntry {
  icon: string;
  title: string;
  body: string;
  timestamp: Date;
  link?: string[];
}

@Component({
  selector: 'app-notifications',
  standalone: true,
  imports: [CommonModule, RouterLink, SpinnerComponent, EmptyStateComponent],
  template: `
    <section class="mx-auto max-w-3xl px-4 py-10 sm:px-6 lg:px-8">
      <h1 class="text-display-md text-text-primary">Notifications</h1>
      <p class="mt-1 text-sm text-text-secondary">A quiet stream of updates from your orders and curators.</p>

      <app-spinner *ngIf="loading()"></app-spinner>
      <app-empty-state *ngIf="!loading() && entries().length === 0"
                       icon="notifications" title="All quiet" message="We'll ping you when there's something worth your attention."></app-empty-state>

      <div *ngIf="!loading() && entries().length > 0" class="mt-8 space-y-3">
        <a *ngFor="let n of entries()" [routerLink]="n.link"
           class="flex gap-4 rounded-2xl bg-background-raised p-5 shadow-atelier-sm transition-all hover:shadow-atelier">
          <div class="flex h-11 w-11 flex-shrink-0 items-center justify-center rounded-xl bg-primary-container">
            <span class="material-symbols-outlined text-primary">{{ n.icon }}</span>
          </div>
          <div class="flex-1">
            <p class="text-sm font-semibold text-text-primary">{{ n.title }}</p>
            <p class="mt-1 text-sm text-text-secondary">{{ n.body }}</p>
            <p class="mt-2 text-[11px] text-text-tertiary">{{ n.timestamp | date:'short' }}</p>
          </div>
        </a>
      </div>
    </section>
  `,
})
export class NotificationsComponent implements OnInit {
  private readonly orderService = inject(OrderService);
  private readonly toast = inject(NotificationService);

  readonly loading = signal<boolean>(true);
  readonly entries = signal<NotificationEntry[]>([]);

  ngOnInit(): void {
    // Derived notifications from recent orders
    this.orderService.list({ page: 0, size: 20, sort: 'createdAt,desc' }).subscribe({
      next: (res) => {
        const entries = res.content.map((o) => this.orderToEntry(o));
        this.entries.set(entries);
        this.loading.set(false);
      },
      error: () => {
        this.entries.set([]);
        this.loading.set(false);
        this.toast.error('Could not load notifications.');
      },
    });
  }

  private orderToEntry(order: Order): NotificationEntry {
    const status = order.shipmentStatus ?? order.status;
    let icon = 'receipt_long';
    let title = `Order #${order.id} placed`;
    let body = `Your order total is $${order.grandTotal.toFixed(2)}.`;

    switch (status) {
      case 'shipped':
      case 'in_transit':
        icon = 'local_shipping';
        title = `Order #${order.id} is on its way`;
        body = 'Track its journey from our curator to your door.';
        break;
      case 'out_for_delivery':
        icon = 'moped';
        title = `Out for delivery — Order #${order.id}`;
        body = 'Your pieces are arriving today.';
        break;
      case 'delivered':
        icon = 'check_circle';
        title = `Delivered — Order #${order.id}`;
        body = 'Enjoy your curated pieces.';
        break;
      case 'cancelled':
        icon = 'cancel';
        title = `Order #${order.id} cancelled`;
        body = 'Your refund will arrive in 3–5 business days.';
        break;
    }

    return {
      icon,
      title,
      body,
      timestamp: new Date(order.createdAt),
      link: ['/orders', String(order.id)],
    };
  }
}
