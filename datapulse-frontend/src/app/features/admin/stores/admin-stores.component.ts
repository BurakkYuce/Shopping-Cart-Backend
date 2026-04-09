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
        <h2 class="border-l border-outline/40 pl-8 text-xl font-semibold text-text-primary">Curator stores</h2>
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
                  [class.bg-success/10]="s.status === 'active'"
                  [class.text-success]="s.status === 'active'"
                  [class.bg-warning/10]="s.status !== 'active'"
                  [class.text-warning]="s.status !== 'active'">
              {{ s.status }}
            </span>
            <code class="rounded-md bg-background-sub px-2 py-1 text-[10px] font-mono text-text-tertiary">{{ s.id }}</code>
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
}
