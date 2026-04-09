import { Component, Input, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

import { Product } from '../../../core/models/product.models';
import { CartService } from '../../../core/services/cart.service';
import { WishlistService } from '../../../core/services/wishlist.service';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';
import { StarRatingComponent } from '../star-rating/star-rating.component';

@Component({
  selector: 'app-product-card',
  standalone: true,
  imports: [CommonModule, RouterLink, StarRatingComponent],
  templateUrl: './product-card.component.html',
})
export class ProductCardComponent {
  @Input({ required: true }) product!: Product;
  @Input() compact: boolean = false;

  private readonly cart = inject(CartService);
  private readonly wishlist = inject(WishlistService);
  private readonly auth = inject(AuthService);
  private readonly toast = inject(NotificationService);

  readonly loadingAdd = signal<boolean>(false);

  // computed so wishlist icon updates reactively when service state changes
  readonly inWishlist = computed(() => this.wishlist.isInWishlist(this.product?.id ?? ''));

  get discountPct(): number | null {
    if (!this.product?.retailPrice || this.product.retailPrice <= this.product.unitPrice) return null;
    return Math.round(((this.product.retailPrice - this.product.unitPrice) / this.product.retailPrice) * 100);
  }

  addToCart(ev: Event): void {
    ev.preventDefault();
    ev.stopPropagation();
    if (!this.auth.isAuthenticated()) {
      this.toast.info('Please sign in to add items to your bag.');
      return;
    }
    this.loadingAdd.set(true);
    this.cart.add({ productId: this.product.id, quantity: 1 }).subscribe({
      next: () => {
        this.toast.success(`${this.product.name} added to bag.`);
        this.loadingAdd.set(false);
      },
      error: () => {
        this.toast.error('Could not add to bag.');
        this.loadingAdd.set(false);
      },
    });
  }

  toggleWishlist(ev: Event): void {
    ev.preventDefault();
    ev.stopPropagation();
    if (!this.auth.isAuthenticated()) {
      this.toast.info('Please sign in to save favourites.');
      return;
    }
    if (this.inWishlist()) {
      this.wishlist.remove(this.product.id).subscribe({ next: () => this.toast.success('Removed from wishlist.') });
    } else {
      this.wishlist.add(this.product.id).subscribe({ next: () => this.toast.success('Saved to wishlist.') });
    }
  }
}
