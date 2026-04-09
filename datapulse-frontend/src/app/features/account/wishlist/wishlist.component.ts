import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

import { WishlistService } from '../../../core/services/wishlist.service';
import { CartService } from '../../../core/services/cart.service';
import { NotificationService } from '../../../core/services/notification.service';
import { SpinnerComponent } from '../../../shared/components/spinner/spinner.component';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';

@Component({
  selector: 'app-wishlist',
  standalone: true,
  imports: [CommonModule, RouterLink, SpinnerComponent, EmptyStateComponent],
  template: `
    <section class="mx-auto max-w-6xl px-4 py-10 sm:px-6 lg:px-8">
      <h1 class="text-display-md text-text-primary">Your wishlist</h1>
      <p class="mt-1 text-sm text-text-secondary">Pieces you're considering. Save them here, act when ready.</p>

      <app-spinner *ngIf="loading()"></app-spinner>
      <app-empty-state *ngIf="!loading() && wishlist.items().length === 0"
                       icon="favorite" title="No saved pieces yet"
                       message="Tap the heart on any piece to start curating your wishlist.">
        <a routerLink="/products" class="btn-primary mt-6">Explore the Edit</a>
      </app-empty-state>

      <div *ngIf="!loading() && wishlist.items().length > 0" class="mt-10 grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-3">
        <article *ngFor="let item of wishlist.items()" class="group flex gap-4 rounded-2xl bg-background-raised p-4 shadow-atelier-sm transition-all hover:shadow-atelier">
          <a [routerLink]="['/products', item.productId]" class="h-28 w-28 flex-shrink-0 overflow-hidden rounded-xl bg-background-sub">
            <img *ngIf="item.imageUrl" [src]="item.imageUrl" [alt]="item.productName" class="h-full w-full object-cover" />
          </a>
          <div class="flex flex-1 flex-col">
            <a [routerLink]="['/products', item.productId]" class="text-sm font-semibold text-text-primary hover:text-primary">{{ item.productName }}</a>
            <p class="mt-2 text-lg font-bold text-primary">\${{ item.unitPrice | number:'1.2-2' }}</p>
            <p class="mt-auto text-[11px] text-text-tertiary">Added {{ item.addedAt | date:'mediumDate' }}</p>
            <div class="mt-3 flex gap-2">
              <button (click)="moveToCart(item.productId, item.productName)" class="btn-primary text-xs px-3 py-2 flex-1">
                <span class="material-symbols-outlined" style="font-size: 14px">shopping_bag</span>To bag
              </button>
              <button (click)="remove(item.productId)" class="flex h-9 w-9 items-center justify-center rounded-xl bg-background-sub text-text-tertiary hover:text-danger" aria-label="Remove">
                <span class="material-symbols-outlined" style="font-size: 18px">delete</span>
              </button>
            </div>
          </div>
        </article>
      </div>
    </section>
  `,
})
export class WishlistComponent implements OnInit {
  readonly wishlist = inject(WishlistService);
  private readonly cart = inject(CartService);
  private readonly toast = inject(NotificationService);

  readonly loading = signal(true);

  ngOnInit(): void {
    this.wishlist.list().subscribe({
      next: () => this.loading.set(false),
      error: () => this.loading.set(false),
    });
  }

  moveToCart(productId: string, name: string | undefined): void {
    this.cart.add({ productId, quantity: 1 }).subscribe({
      next: () => {
        this.wishlist.remove(productId).subscribe();
        this.toast.success(`${name ?? 'Item'} moved to your bag.`);
      },
      error: () => this.toast.error('Could not move to bag.'),
    });
  }

  remove(productId: string): void {
    this.wishlist.remove(productId).subscribe({
      next: () => this.toast.success('Removed from wishlist.'),
      error: () => this.toast.error('Could not remove.'),
    });
  }
}
