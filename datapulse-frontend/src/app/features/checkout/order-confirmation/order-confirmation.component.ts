import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { OrderService } from '../../../core/services/order.service';
import { Order } from '../../../core/models/order.models';
import { SpinnerComponent } from '../../../shared/components/spinner/spinner.component';

@Component({
  selector: 'app-order-confirmation',
  standalone: true,
  imports: [CommonModule, RouterLink, SpinnerComponent],
  template: `
    <section class="flex min-h-[80vh] items-center justify-center bg-background-accent px-4 py-16 sm:px-6 lg:px-8">
      <app-spinner *ngIf="loading()"></app-spinner>
      <div *ngIf="!loading() && order() as o" class="w-full max-w-lg rounded-[2rem] bg-background-raised p-4 shadow-atelier animate-fade-in">
        <div class="rounded-3xl bg-background-raised p-10 text-center">
          <div class="relative mx-auto flex h-24 w-24 items-center justify-center">
            <span class="absolute inline-flex h-full w-full animate-ping rounded-full bg-success/30"></span>
            <span class="absolute inline-flex h-full w-full animate-ping-slow rounded-full bg-success/20" style="animation-delay: 0.5s"></span>
            <div class="relative flex h-20 w-20 items-center justify-center rounded-full bg-success text-white">
              <span class="material-symbols-outlined" style="font-size: 40px">check</span>
            </div>
          </div>
          <h1 class="mt-8 text-display-md text-text-primary">Order placed!</h1>
          <p class="mt-3 text-sm text-text-secondary">Your curated pieces are on their way.</p>

          <div class="mt-6 inline-flex items-center gap-2 rounded-full bg-background-sub px-4 py-2 font-mono text-sm">
            <span class="text-text-tertiary">ORDER</span>
            <span class="font-bold text-text-primary">#{{ o.id }}</span>
          </div>

          <div class="mt-8 grid grid-cols-2 gap-4 rounded-2xl bg-background-sub p-5 text-left">
            <div>
              <p class="text-[11px] font-bold uppercase tracking-wider text-text-tertiary">Shipping to</p>
              <p class="mt-1 text-sm font-semibold text-text-primary">Address on file</p>
            </div>
            <div>
              <p class="text-[11px] font-bold uppercase tracking-wider text-text-tertiary">Expected delivery</p>
              <p class="mt-1 text-sm font-semibold text-text-primary">{{ estimatedDelivery() }}</p>
            </div>
          </div>

          <div class="mt-8 flex flex-col gap-3">
            <a [routerLink]="['/orders', o.id]" class="btn-primary w-full">
              <span class="material-symbols-outlined" style="font-size: 20px">local_shipping</span>
              Track order
            </a>
            <a routerLink="/products" class="btn-secondary w-full">Continue shopping</a>
          </div>

          <div class="mt-8 grid grid-cols-3 gap-4 border-t border-outline/60 pt-6 text-center">
            <div>
              <span class="material-symbols-outlined text-primary">verified</span>
              <p class="mt-1 text-[11px] font-semibold text-text-secondary">Secure</p>
            </div>
            <div>
              <span class="material-symbols-outlined text-primary">local_shipping</span>
              <p class="mt-1 text-[11px] font-semibold text-text-secondary">Fast delivery</p>
            </div>
            <div>
              <span class="material-symbols-outlined text-primary">eco</span>
              <p class="mt-1 text-[11px] font-semibold text-text-secondary">Offset</p>
            </div>
          </div>
        </div>
      </div>
    </section>
  `,
})
export class OrderConfirmationComponent implements OnInit {
  private readonly orderService = inject(OrderService);
  private readonly route = inject(ActivatedRoute);

  readonly order = signal<Order | null>(null);
  readonly loading = signal<boolean>(true);

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.orderService.get(id).subscribe({
        next: (o) => {
          this.order.set(o);
          this.loading.set(false);
        },
        error: () => this.loading.set(false),
      });
    } else {
      this.loading.set(false);
    }
  }

  estimatedDelivery(): string {
    const d = new Date();
    d.setDate(d.getDate() + 5);
    return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
  }
}
