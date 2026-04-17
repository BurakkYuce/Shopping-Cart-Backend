import { Component, OnInit, inject, signal, computed, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule, Location } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { switchMap } from 'rxjs';
import { ProductService } from '../../../core/services/product.service';
import { CartService } from '../../../core/services/cart.service';
import { WishlistService } from '../../../core/services/wishlist.service';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';
import { Product, Review, Store } from '../../../core/models/product.models';
import { StarRatingComponent } from '../../../shared/components/star-rating/star-rating.component';
import { SpinnerComponent } from '../../../shared/components/spinner/spinner.component';
import { ProductCardComponent } from '../../../shared/components/product-card/product-card.component';

type Tab = 'description' | 'specs' | 'reviews' | 'qa';

@Component({
  selector: 'app-product-detail',
  standalone: true,
  imports: [CommonModule, RouterLink, StarRatingComponent, SpinnerComponent, ProductCardComponent],
  templateUrl: './product-detail.component.html',
})
export class ProductDetailComponent implements OnInit {
  private readonly productService = inject(ProductService);
  private readonly cart = inject(CartService);
  private readonly wishlist = inject(WishlistService);
  private readonly auth = inject(AuthService);
  private readonly toast = inject(NotificationService);
  private readonly location = inject(Location);

  goBack(): void {
    this.location.back();
  }
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);

  readonly product = signal<Product | null>(null);
  readonly store = signal<Store | null>(null);
  readonly related = signal<Product[]>([]);
  readonly reviews = signal<Review[]>([]);
  readonly loading = signal<boolean>(true);
  readonly tab = signal<Tab>('description');
  readonly quantity = signal<number>(1);
  readonly selectedImage = signal<number>(0);
  readonly addingToCart = signal<boolean>(false);
  readonly reviewRating = signal<number>(0);
  readonly reviewHeadline = signal<string>('');
  readonly reviewText = signal<string>('');
  readonly submittingReview = signal<boolean>(false);
  readonly reviewError = signal<string | null>(null);
  readonly reviewSuccess = signal<boolean>(false);

  // Image zoom state
  readonly zooming = signal<boolean>(false);
  readonly zoomOrigin = signal<string>('center center');
  readonly lightboxOpen = signal<boolean>(false);
  readonly lightboxZoomed = signal<boolean>(false);
  readonly lightboxZoomOrigin = signal<string>('center center');

  readonly discountPct = computed(() => {
    const p = this.product();
    if (!p || !p.retailPrice || p.retailPrice <= p.unitPrice) return null;
    return Math.round(((p.retailPrice - p.unitPrice) / p.retailPrice) * 100);
  });

  readonly avgRating = computed(() => {
    const r = this.reviews();
    if (r.length === 0) return this.product()?.rating ?? 0;
    return r.reduce((sum, rev) => sum + (rev.starRating ?? 0), 0) / r.length;
  });

  readonly ratingDistribution = computed(() => {
    const r = this.reviews();
    const dist: Record<number, number> = { 5: 0, 4: 0, 3: 0, 2: 0, 1: 0 };
    r.forEach((rev) => {
      const s = Math.round(rev.starRating ?? 0);
      if (dist[s] !== undefined) dist[s]++;
    });
    const total = r.length || 1;
    return [5, 4, 3, 2, 1].map((star) => ({ star, count: dist[star], pct: (dist[star] / total) * 100 }));
  });

  readonly isInWishlist = computed(() => {
    const p = this.product();
    return p ? this.wishlist.isInWishlist(p.id) : false;
  });

  ngOnInit(): void {
    this.route.paramMap.pipe(
      takeUntilDestroyed(this.destroyRef),
      switchMap((params) => {
        const id = params.get('id') ?? '';
        this.loading.set(true);
        this.quantity.set(1);
        this.selectedImage.set(0);
        this.product.set(null);
        this.store.set(null);
        return this.productService.get(id);
      }),
    ).subscribe({
      next: (p) => {
        this.product.set(p);
        this.loading.set(false);
        this.loadRelated(p);
        this.loadReviews(p.id);
        this.loadStore(p.storeId);
      },
      error: () => this.loading.set(false),
    });
  }

  setTab(t: Tab): void {
    this.tab.set(t);
  }

  selectImage(i: number): void {
    this.selectedImage.set(i);
  }

  incrementQty(): void {
    const max = this.product()?.stockQuantity ?? 1;
    this.quantity.update((q) => Math.min(q + 1, max));
  }

  decrementQty(): void {
    this.quantity.update((q) => Math.max(q - 1, 1));
  }

  addToCart(): void {
    const p = this.product();
    if (!p) return;
    if (!this.auth.isAuthenticated()) {
      this.toast.info('Please sign in to add items to your bag.');
      return;
    }
    this.addingToCart.set(true);
    this.cart.add({ productId: p.id, quantity: this.quantity() }).subscribe({
      next: () => {
        this.toast.success(`${p.name} added to bag.`);
        this.addingToCart.set(false);
      },
      error: () => {
        this.toast.error('Could not add to bag.');
        this.addingToCart.set(false);
      },
    });
  }

  setReviewRating(star: number): void {
    this.reviewRating.set(star);
  }

  submitReview(): void {
    const p = this.product();
    if (!p || this.reviewRating() === 0) return;
    this.submittingReview.set(true);
    this.reviewError.set(null);
    this.productService.createReview({
      productId: p.id,
      starRating: this.reviewRating(),
      reviewHeadline: this.reviewHeadline(),
      reviewText: this.reviewText(),
    }).subscribe({
      next: (rev) => {
        this.reviews.update((r) => [rev, ...r]);
        this.reviewSuccess.set(true);
        this.reviewRating.set(0);
        this.reviewHeadline.set('');
        this.reviewText.set('');
        this.submittingReview.set(false);
      },
      error: (err) => {
        const msg = err?.error?.message ?? 'Could not submit review.';
        this.reviewError.set(msg);
        this.submittingReview.set(false);
      },
    });
  }

  onImageMouseMove(event: MouseEvent): void {
    const target = event.currentTarget as HTMLElement;
    const rect = target.getBoundingClientRect();
    const x = ((event.clientX - rect.left) / rect.width) * 100;
    const y = ((event.clientY - rect.top) / rect.height) * 100;
    this.zoomOrigin.set(`${x}% ${y}%`);
    this.zooming.set(true);
  }

  onImageMouseLeave(): void {
    this.zooming.set(false);
  }

  openLightbox(): void {
    this.zooming.set(false);
    this.lightboxOpen.set(true);
    this.lightboxZoomed.set(false);
  }

  closeLightbox(): void {
    this.lightboxOpen.set(false);
    this.lightboxZoomed.set(false);
  }

  toggleLightboxZoom(event: MouseEvent): void {
    if (!this.lightboxZoomed()) {
      const target = event.target as HTMLElement;
      const rect = target.getBoundingClientRect();
      const x = ((event.clientX - rect.left) / rect.width) * 100;
      const y = ((event.clientY - rect.top) / rect.height) * 100;
      this.lightboxZoomOrigin.set(`${x}% ${y}%`);
    }
    this.lightboxZoomed.update((v) => !v);
  }

  toggleWishlist(): void {
    const p = this.product();
    if (!p) return;
    if (!this.auth.isAuthenticated()) {
      this.toast.info('Please sign in to save favourites.');
      return;
    }
    if (this.isInWishlist()) {
      this.wishlist.remove(p.id).subscribe({ next: () => this.toast.success('Removed from wishlist.') });
    } else {
      this.wishlist.add(p.id).subscribe({ next: () => this.toast.success('Saved to wishlist.') });
    }
  }

  private loadRelated(p: Product): void {
    this.productService.list({ categoryId: p.categoryId, size: 4 }).subscribe({
      next: (res) => this.related.set(res.content.filter((r) => r.id !== p.id)),
      error: () => this.related.set([]),
    });
  }

  private loadReviews(id: string): void {
    this.productService.listReviews(id).subscribe({
      next: (revs) => this.reviews.set(revs),
      error: () => this.reviews.set([]),
    });
  }

  private loadStore(id: string): void {
    if (!id) return;
    this.productService.getStore(id).subscribe({
      next: (s) => this.store.set(s),
      error: () => this.store.set(null),
    });
  }
}
