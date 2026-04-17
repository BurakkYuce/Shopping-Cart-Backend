import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';

import { ProductService } from '../../../core/services/product.service';
import { NotificationService } from '../../../core/services/notification.service';
import { Product, Review } from '../../../core/models/product.models';
import { SpinnerComponent } from '../../../shared/components/spinner/spinner.component';

interface ReviewWithProduct extends Review {
  product?: Product;
  replyDraft?: string;
  replying?: boolean;
}

@Component({
  selector: 'app-seller-reviews',
  standalone: true,
  imports: [CommonModule, FormsModule, SpinnerComponent],
  template: `
    <section class="p-8">
      <div>
        <p class="text-xs font-bold uppercase tracking-wider text-primary">Customer feedback</p>
        <h1 class="mt-2 text-display-md text-text-primary">Reviews</h1>
        <p class="mt-1 text-sm text-text-secondary">View and respond to customer reviews on your products.</p>
      </div>

      <app-spinner *ngIf="loading()"></app-spinner>

      <div *ngIf="!loading()" class="mt-8 space-y-4">
        <div *ngFor="let r of reviews()" class="rounded-2xl bg-background-raised p-6 shadow-atelier-sm">
          <div class="flex items-start justify-between gap-4">
            <div class="flex-1">
              <div class="flex items-center gap-3">
                <p class="text-sm font-semibold text-text-primary">{{ r.product?.name ?? 'Product #' + r.productId }}</p>
                <div class="flex items-center gap-0.5">
                  <span *ngFor="let star of [1,2,3,4,5]"
                        class="material-symbols-outlined"
                        [class.text-amber-400]="star <= r.starRating"
                        [class.text-zinc-600]="star > r.starRating"
                        [class.filled]="star <= r.starRating"
                        style="font-size: 16px">
                    star
                  </span>
                </div>
              </div>
              <p *ngIf="r.reviewHeadline" class="mt-2 text-sm font-semibold text-text-primary">{{ r.reviewHeadline }}</p>
              <p *ngIf="r.reviewText" class="mt-1 text-sm text-text-secondary">{{ r.reviewText }}</p>
              <div class="mt-2 flex items-center gap-4 text-xs text-text-tertiary">
                <span>User {{ r.userId }}</span>
                <span *ngIf="r.reviewDate">{{ r.reviewDate | date:'MMM d, y' }}</span>
                <span *ngIf="r.verifiedPurchase === 'Y'" class="flex items-center gap-1 text-success">
                  <span class="material-symbols-outlined" style="font-size: 14px">verified</span>
                  Verified
                </span>
              </div>
            </div>
            <span class="rounded-full px-3 py-1 text-xs font-bold"
                  [ngClass]="{
                    'bg-success/10 text-success': r.sentiment === 'positive',
                    'bg-amber-500/10 text-amber-500': r.sentiment === 'neutral',
                    'bg-danger/10 text-danger': r.sentiment === 'negative'
                  }">
              {{ r.sentiment }}
            </span>
          </div>

          <!-- Existing seller response -->
          <div *ngIf="r.sellerResponse" class="mt-4 rounded-xl bg-primary/5 p-4 border-l-4 border-primary">
            <p class="text-xs font-bold uppercase tracking-wider text-primary">Your Response</p>
            <p class="mt-1 text-sm text-text-secondary">{{ r.sellerResponse }}</p>
            <p *ngIf="r.sellerResponseDate" class="mt-1 text-xs text-text-tertiary">{{ r.sellerResponseDate | date:'MMM d, y HH:mm' }}</p>
          </div>

          <!-- Reply form -->
          <div *ngIf="!r.sellerResponse" class="mt-4">
            <div *ngIf="!r.replying" class="flex">
              <button (click)="startReply(r)" class="flex items-center gap-1.5 text-xs font-semibold text-primary hover:text-primary-hover">
                <span class="material-symbols-outlined" style="font-size: 16px">reply</span>
                Write a response
              </button>
            </div>
            <div *ngIf="r.replying" class="space-y-3">
              <textarea [(ngModel)]="r.replyDraft" rows="3" class="input-base w-full bg-background-sub"
                        placeholder="Write your response to this review..."></textarea>
              <div class="flex items-center gap-2">
                <button (click)="submitResponse(r)" [disabled]="!r.replyDraft?.trim()"
                        class="btn-primary h-9 text-xs">Submit Response</button>
                <button (click)="cancelReply(r)" class="btn-secondary h-9 text-xs">Cancel</button>
              </div>
            </div>
          </div>
        </div>

        <div *ngIf="reviews().length === 0" class="rounded-3xl bg-background-raised p-16 text-center shadow-atelier-sm">
          <span class="material-symbols-outlined text-text-tertiary" style="font-size: 48px">rate_review</span>
          <p class="mt-4 text-sm font-semibold text-text-primary">No reviews yet</p>
          <p class="mt-1 text-sm text-text-secondary">Customer reviews will appear here once they start rating your products.</p>
        </div>
      </div>
    </section>
  `,
})
export class SellerReviewsComponent implements OnInit {
  private readonly productService = inject(ProductService);
  private readonly toast = inject(NotificationService);

  readonly reviews = signal<ReviewWithProduct[]>([]);
  readonly loading = signal<boolean>(true);

  ngOnInit(): void {
    this.loadReviews();
  }

  private loadReviews(): void {
    this.loading.set(true);
    this.productService.list({ size: 100 }).subscribe({
      next: (res) => {
        const products = res.content;
        if (products.length === 0) {
          this.reviews.set([]);
          this.loading.set(false);
          return;
        }

        const reviewCalls = products.map(p => this.productService.listReviews(p.id));
        forkJoin(reviewCalls).subscribe({
          next: (reviewArrays) => {
            const allReviews: ReviewWithProduct[] = [];
            reviewArrays.forEach((reviews, idx) => {
              reviews.forEach(r => {
                allReviews.push({ ...r, product: products[idx], replyDraft: '', replying: false });
              });
            });
            allReviews.sort((a, b) => (b.reviewDate ?? '').localeCompare(a.reviewDate ?? ''));
            this.reviews.set(allReviews);
            this.loading.set(false);
          },
          error: () => {
            this.reviews.set([]);
            this.loading.set(false);
          },
        });
      },
      error: () => {
        this.reviews.set([]);
        this.loading.set(false);
        this.toast.error('Could not load reviews.');
      },
    });
  }

  startReply(r: ReviewWithProduct): void {
    this.reviews.update(list =>
      list.map(rev => rev.id === r.id ? { ...rev, replying: true } : rev),
    );
  }

  cancelReply(r: ReviewWithProduct): void {
    this.reviews.update(list =>
      list.map(rev => rev.id === r.id ? { ...rev, replying: false, replyDraft: '' } : rev),
    );
  }

  submitResponse(r: ReviewWithProduct): void {
    const text = r.replyDraft?.trim();
    if (!text) return;

    this.productService.respondToReview(r.id, text).subscribe({
      next: (updated) => {
        this.reviews.update(list =>
          list.map(rev =>
            rev.id === r.id
              ? { ...rev, sellerResponse: updated.sellerResponse, sellerResponseDate: updated.sellerResponseDate, replying: false }
              : rev,
          ),
        );
        this.toast.success('Response submitted.');
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Could not submit response.');
      },
    });
  }
}
