import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';

import { ProductService } from '../../../core/services/product.service';
import { NotificationService } from '../../../core/services/notification.service';
import { Store } from '../../../core/models/product.models';
import { SpinnerComponent } from '../../../shared/components/spinner/spinner.component';

@Component({
  selector: 'app-admin-stores',
  standalone: true,
  imports: [CommonModule, SpinnerComponent],
  template: `
    <header class="sticky top-0 z-40 flex items-center justify-between border-b border-outline/40 bg-background/90 px-12 py-6 backdrop-blur-xl">
      <div class="flex items-center gap-8">
        <span class="text-2xl font-bold tracking-tight text-text-primary">DataPulse</span>
        <h2 class="border-l border-outline/40 pl-8 text-xl font-semibold text-text-primary">Curator Stores</h2>
      </div>
      <div class="text-xs font-semibold uppercase tracking-wider text-text-tertiary">
        {{ stores().length }} store{{ stores().length === 1 ? '' : 's' }}
      </div>
    </header>

    <section class="px-12 py-10">
      <app-spinner *ngIf="loading()"></app-spinner>

      <div *ngIf="!loading()" class="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
        <div *ngFor="let s of stores()" class="rounded-3xl bg-background-raised p-7 shadow-atelier-sm">
          <div class="flex items-center gap-4">
            <div class="flex h-14 w-14 items-center justify-center overflow-hidden rounded-2xl bg-background-sub">
              <span class="material-symbols-outlined text-text-tertiary">storefront</span>
            </div>
            <div class="flex-1 min-w-0">
              <p class="truncate text-sm font-semibold text-text-primary">{{ s.name }}</p>
              <p class="text-xs text-text-tertiary">Owner <code class="text-[10px]">{{ s.ownerId }}</code></p>
            </div>
          </div>

          <div class="mt-5 flex items-center justify-between border-t border-outline/30 pt-4">
            <span class="rounded-full px-3 py-1 text-[10px] font-black uppercase"
                  [ngClass]="{
                    'bg-success/10 text-success': s.status === 'active' || s.status === 'ACTIVE',
                    'bg-amber-500/10 text-amber-500': s.status === 'pending' || s.status === 'PENDING' || s.status === 'PENDING_APPROVAL',
                    'bg-danger/10 text-danger': s.status === 'suspended' || s.status === 'SUSPENDED',
                    'bg-zinc-500/10 text-zinc-500': s.status === 'closed' || s.status === 'CLOSED'
                  }">
              {{ s.status }}
            </span>
            <code class="rounded-md bg-background-sub px-2 py-1 text-[10px] font-mono text-text-tertiary">{{ s.id }}</code>
          </div>

          <!-- Management Actions -->
          <div class="mt-4 flex flex-wrap gap-2">
            <button *ngIf="isApprovalPending(s)"
                    (click)="approve(s)" [disabled]="actionLoading()"
                    class="flex items-center gap-1.5 rounded-lg bg-success/10 px-3 py-1.5 text-xs font-semibold text-success hover:bg-success/20">
              <span class="material-symbols-outlined" style="font-size: 16px">check_circle</span>
              Approve
            </button>
            <button *ngIf="isActive(s)"
                    (click)="suspend(s)" [disabled]="actionLoading()"
                    class="flex items-center gap-1.5 rounded-lg bg-amber-500/10 px-3 py-1.5 text-xs font-semibold text-amber-500 hover:bg-amber-500/20">
              <span class="material-symbols-outlined" style="font-size: 16px">pause_circle</span>
              Suspend
            </button>
            <button *ngIf="!isClosed(s)"
                    (click)="close(s)" [disabled]="actionLoading()"
                    class="flex items-center gap-1.5 rounded-lg bg-danger/10 px-3 py-1.5 text-xs font-semibold text-danger hover:bg-danger/20">
              <span class="material-symbols-outlined" style="font-size: 16px">cancel</span>
              Close
            </button>
          </div>
        </div>

        <div *ngIf="stores().length === 0" class="col-span-full py-16 text-center text-sm text-text-tertiary">
          No stores on the platform yet.
        </div>
      </div>
    </section>
  `,
})
export class AdminStoresComponent implements OnInit {
  private readonly productService = inject(ProductService);
  private readonly toast = inject(NotificationService);

  readonly stores = signal<Store[]>([]);
  readonly loading = signal<boolean>(true);
  readonly actionLoading = signal<boolean>(false);

  ngOnInit(): void {
    this.fetch();
  }

  fetch(): void {
    this.loading.set(true);
    this.productService.listStores().subscribe({
      next: (stores) => {
        this.stores.set(stores);
        this.loading.set(false);
      },
      error: () => {
        this.stores.set([]);
        this.loading.set(false);
        this.toast.error('Could not load stores.');
      },
    });
  }

  isApprovalPending(s: Store): boolean {
    const status = s.status?.toUpperCase();
    return status === 'PENDING' || status === 'PENDING_APPROVAL';
  }

  isActive(s: Store): boolean {
    return s.status?.toUpperCase() === 'ACTIVE';
  }

  isClosed(s: Store): boolean {
    return s.status?.toUpperCase() === 'CLOSED';
  }

  approve(s: Store): void {
    this.actionLoading.set(true);
    this.productService.approveStore(s.id).subscribe({
      next: () => {
        this.toast.success(`Store "${s.name}" approved.`);
        this.actionLoading.set(false);
        this.fetch();
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Could not approve store.');
        this.actionLoading.set(false);
      },
    });
  }

  suspend(s: Store): void {
    this.actionLoading.set(true);
    this.productService.suspendStore(s.id).subscribe({
      next: () => {
        this.toast.success(`Store "${s.name}" suspended.`);
        this.actionLoading.set(false);
        this.fetch();
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Could not suspend store.');
        this.actionLoading.set(false);
      },
    });
  }

  close(s: Store): void {
    this.actionLoading.set(true);
    this.productService.closeStore(s.id).subscribe({
      next: () => {
        this.toast.success(`Store "${s.name}" closed.`);
        this.actionLoading.set(false);
        this.fetch();
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Could not close store.');
        this.actionLoading.set(false);
      },
    });
  }
}
