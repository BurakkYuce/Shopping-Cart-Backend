import { Component, OnInit, inject, signal, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { switchMap } from 'rxjs';
import { OrderService } from '../../../core/services/order.service';
import { Order } from '../../../core/models/order.models';
import { StatusBadgeComponent } from '../../../shared/components/status-badge/status-badge.component';
import { SpinnerComponent } from '../../../shared/components/spinner/spinner.component';
import { NotificationService } from '../../../core/services/notification.service';

@Component({
  selector: 'app-order-detail',
  standalone: true,
  imports: [CommonModule, RouterLink, StatusBadgeComponent, SpinnerComponent],
  templateUrl: './order-detail.component.html',
})
export class OrderDetailComponent implements OnInit {
  private readonly orderService = inject(OrderService);
  private readonly route = inject(ActivatedRoute);
  private readonly toast = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);

  readonly order = signal<Order | null>(null);
  readonly loading = signal<boolean>(true);
  readonly actionLoading = signal<boolean>(false);
  readonly showReturnModal = signal<boolean>(false);
  readonly returnReason = signal<string>('');

  ngOnInit(): void {
    this.route.paramMap.pipe(
      takeUntilDestroyed(this.destroyRef),
      switchMap((params) => {
        const id = params.get('id') ?? '';
        this.loading.set(true);
        this.order.set(null);
        return this.orderService.get(id);
      }),
    ).subscribe({
      next: (o) => {
        this.order.set(o);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  load(id: string): void {
    this.loading.set(true);
    this.orderService.get(id).subscribe({
      next: (o) => {
        this.order.set(o);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  cancelOrder(): void {
    const o = this.order();
    if (!o || !confirm('Cancel this order? This cannot be undone.')) return;
    this.actionLoading.set(true);
    this.orderService.cancel(o.id).subscribe({
      next: (updated) => {
        this.order.set(updated);
        this.actionLoading.set(false);
        this.toast.success('Order cancelled.');
      },
      error: () => {
        this.actionLoading.set(false);
        this.toast.error('Could not cancel order.');
      },
    });
  }

  openReturnModal(): void {
    this.showReturnModal.set(true);
  }

  closeReturnModal(): void {
    this.showReturnModal.set(false);
    this.returnReason.set('');
  }

  submitReturn(): void {
    const o = this.order();
    if (!o) return;
    this.actionLoading.set(true);
    this.orderService.requestReturn(o.id).subscribe({
      next: (updated) => {
        this.order.set(updated);
        this.actionLoading.set(false);
        this.closeReturnModal();
        this.toast.success('Return requested.');
      },
      error: () => {
        this.actionLoading.set(false);
        this.toast.error('Could not request return.');
      },
    });
  }
}
