import { Component, OnInit, inject, signal, computed, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

import { CartService } from '../../../core/services/cart.service';
import { ProductService } from '../../../core/services/product.service';
import { CartItem } from '../../../core/models/cart.models';
import { Product } from '../../../core/models/product.models';
import { NotificationService } from '../../../core/services/notification.service';
import { SpinnerComponent } from '../../../shared/components/spinner/spinner.component';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';
import { ProductCardComponent } from '../../../shared/components/product-card/product-card.component';

@Component({
  selector: 'app-cart-page',
  standalone: true,
  imports: [CommonModule, RouterLink, SpinnerComponent, EmptyStateComponent, ProductCardComponent],
  templateUrl: './cart-page.component.html',
})
export class CartPageComponent implements OnInit {
  readonly cart = inject(CartService);
  private readonly productService = inject(ProductService);
  private readonly toast = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);

  readonly loading = signal<boolean>(true);
  readonly suggestions = signal<Product[]>([]);
  readonly couponCode = signal<string>('');
  readonly imageMap = signal<Record<string, string>>({});

  readonly subtotal = computed(() => this.cart.cartSig().totalPrice);
  readonly shipping = computed(() => (this.subtotal() > 100 || this.subtotal() === 0 ? 0 : 9.9));
  readonly tax = computed(() => +(this.subtotal() * 0.08).toFixed(2));
  readonly total = computed(() => +(this.subtotal() + this.shipping() + this.tax()).toFixed(2));

  ngOnInit(): void {
    this.cart.fetch().subscribe({
      next: () => {
        this.loading.set(false);
        this.enrichImages();
      },
      error: () => this.loading.set(false),
    });
    this.productService.list({ size: 4, sort: 'rating,desc' }).subscribe({
      next: (res) => this.suggestions.set(res.content),
    });
  }

  private enrichImages(): void {
    this.cart.cartSig().items.forEach((item) => {
      this.productService.get(item.productId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
        next: (p) => {
          if (p.imageUrl) {
            this.imageMap.update((m) => ({ ...m, [item.productId]: p.imageUrl! }));
          }
        },
      });
    });
  }

  increment(item: CartItem): void {
    this.cart.updateItem(item.productId, { quantity: item.quantity + 1 }).subscribe({
      error: () => this.toast.error('Could not update quantity.'),
    });
  }

  decrement(item: CartItem): void {
    if (item.quantity <= 1) return this.remove(item);
    this.cart.updateItem(item.productId, { quantity: item.quantity - 1 }).subscribe({
      error: () => this.toast.error('Could not update quantity.'),
    });
  }

  remove(item: CartItem): void {
    this.cart.removeItem(item.productId).subscribe({
      next: () => this.toast.success('Removed from bag.'),
      error: () => this.toast.error('Could not remove item.'),
    });
  }

  applyCoupon(): void {
    const code = this.couponCode().trim();
    if (!code) return;
    this.toast.info(`Coupon "${code}" will be validated at checkout.`);
  }
}
