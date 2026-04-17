import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

import { AuthService } from '../../../core/services/auth.service';
import { CartService } from '../../../core/services/cart.service';
import { CouponService } from '../../../core/services/coupon.service';
import { ProductService } from '../../../core/services/product.service';
import { CartItem, StoreGroup } from '../../../core/models/cart.models';
import { CouponValidation } from '../../../core/models/coupon.models';
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
  private readonly auth = inject(AuthService);
  readonly cart = inject(CartService);
  private readonly couponService = inject(CouponService);
  private readonly productService = inject(ProductService);
  private readonly toast = inject(NotificationService);

  readonly isCustomer = this.auth.isCustomer;

  readonly loading = signal<boolean>(true);
  readonly couponLoading = signal<boolean>(false);
  readonly suggestions = signal<Product[]>([]);
  readonly couponCode = signal<string>('');
  readonly appliedCoupon = signal<CouponValidation | null>(null);
  readonly couponError = signal<string | null>(null);

  readonly subtotal = computed(() => this.cart.cartSig().totalPrice);
  readonly discount = computed(() => this.appliedCoupon()?.discountAmount ?? 0);
  readonly shipping = computed(() => {
    const groups = this.storeGroups();
    if (groups.length === 0) return 0;
    return groups.reduce((sum, g) => sum + (g.subtotal > 100 ? 0 : 9.9), 0);
  });
  readonly storeShippingCount = computed(() => this.storeGroups().filter(g => g.subtotal <= 100).length);
  readonly tax = computed(() => +((this.subtotal() - this.discount()) * 0.08).toFixed(2));
  readonly total = computed(() => +(this.subtotal() - this.discount() + this.shipping() + this.tax()).toFixed(2));

  readonly storeGroups = computed<StoreGroup[]>(() => {
    const items = this.cart.cartSig().items;
    const map = new Map<string, StoreGroup>();
    for (const item of items) {
      const key = item.storeId || 'unknown';
      if (!map.has(key)) {
        map.set(key, { storeId: key, storeName: item.storeName || 'Unknown Store', items: [], subtotal: 0 });
      }
      const group = map.get(key)!;
      group.items.push(item);
      group.subtotal += item.lineTotal;
    }
    return Array.from(map.values());
  });

  ngOnInit(): void {
    if (!this.isCustomer()) {
      this.loading.set(false);
      return;
    }
    this.cart.fetch().subscribe({
      next: () => this.loading.set(false),
      error: () => this.loading.set(false),
    });
    this.productService.list({ size: 4, sort: 'rating,desc' }).subscribe({
      next: (res) => this.suggestions.set(res.content),
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
    this.couponLoading.set(true);
    this.couponError.set(null);
    this.couponService.validate(code, this.subtotal()).subscribe({
      next: (result) => {
        this.appliedCoupon.set(result);
        this.cart.setCouponCode(result.code);
        this.couponError.set(null);
        this.couponLoading.set(false);
        this.toast.success(`Coupon "${result.code}" applied!`);
      },
      error: (err) => {
        this.appliedCoupon.set(null);
        this.couponError.set(err?.error?.message ?? 'Invalid coupon code');
        this.couponLoading.set(false);
      },
    });
  }

  removeCoupon(): void {
    this.appliedCoupon.set(null);
    this.cart.setCouponCode(null);
    this.couponCode.set('');
    this.couponError.set(null);
  }
}
