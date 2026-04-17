import { Component, OnInit, inject, signal, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { switchMap } from 'rxjs';
import { OrderService } from '../../../core/services/order.service';
import { ProductService } from '../../../core/services/product.service';
import { Order } from '../../../core/models/order.models';
import { Store } from '../../../core/models/product.models';
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
  private readonly productService = inject(ProductService);
  private readonly route = inject(ActivatedRoute);
  private readonly toast = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);

  readonly order = signal<Order | null>(null);
  readonly store = signal<Store | null>(null);
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
        this.store.set(null);
        return this.orderService.get(id);
      }),
    ).subscribe({
      next: (o) => {
        this.order.set(o);
        this.loading.set(false);
        this.loadStore(o.storeId);
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
        this.loadStore(o.storeId);
      },
      error: () => this.loading.set(false),
    });
  }

  private loadStore(storeId: string | undefined): void {
    if (!storeId) { this.store.set(null); return; }
    this.productService.getStore(storeId).subscribe({
      next: (s) => this.store.set(s),
      error: () => this.store.set(null),
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
    const reason = this.returnReason().trim();
    if (!reason) {
      this.toast.error('Please tell us why you\'re returning this order.');
      return;
    }
    this.actionLoading.set(true);
    this.orderService.requestReturn(o.id, reason).subscribe({
      next: () => {
        // Backend returns the new ReturnRequest; refetch the order to get its updated status.
        this.orderService.get(o.id).subscribe({
          next: (fresh) => this.order.set(fresh),
        });
        this.actionLoading.set(false);
        this.closeReturnModal();
        this.toast.success('Return requested.');
      },
      error: (err) => {
        this.actionLoading.set(false);
        const msg = err?.error?.message ?? err?.error?.error ?? 'Could not request return.';
        this.toast.error(msg);
      },
    });
  }
}
