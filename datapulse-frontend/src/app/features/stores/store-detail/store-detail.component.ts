import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { ProductService } from '../../../core/services/product.service';
import { Product, Store } from '../../../core/models/product.models';
import { ProductCardComponent } from '../../../shared/components/product-card/product-card.component';
import { SpinnerComponent } from '../../../shared/components/spinner/spinner.component';
import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';

@Component({
  selector: 'app-store-detail',
  standalone: true,
  imports: [CommonModule, RouterLink, ProductCardComponent, SpinnerComponent, PaginationComponent, EmptyStateComponent],
  template: `
    <app-spinner *ngIf="loading()" />

    <div *ngIf="!loading() && store()" class="mx-auto max-w-7xl px-4 py-8">
      <!-- Store Header -->
      <div class="mb-8 flex items-center gap-6 rounded-xl bg-white p-6 shadow-sm">
        <div class="flex h-20 w-20 flex-shrink-0 items-center justify-center overflow-hidden rounded-full bg-bg-secondary">
          <img *ngIf="store()!.logoUrl" [src]="store()!.logoUrl" [alt]="store()!.name" class="h-full w-full object-cover" />
          <span *ngIf="!store()!.logoUrl" class="material-symbols-outlined text-text-tertiary" style="font-size:40px">storefront</span>
        </div>
        <div>
          <h1 class="text-2xl font-bold text-text-primary">{{ store()!.name }}</h1>
          <p *ngIf="store()!.description" class="mt-1 text-sm text-text-secondary">{{ store()!.description }}</p>
          <p class="mt-1 text-xs text-text-tertiary">{{ totalProducts() }} products</p>
        </div>
      </div>

      <!-- Products Grid -->
      <div *ngIf="products().length > 0" class="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4">
        <app-product-card *ngFor="let product of products()" [product]="product" />
      </div>

      <app-empty-state *ngIf="products().length === 0 && !loading()"
        title="No products yet"
        subtitle="This store hasn't listed any products." />

      <div *ngIf="totalPages() > 1" class="mt-8">
        <app-pagination
          [currentPage]="currentPage()"
          [totalPages]="totalPages()"
          (pageChange)="onPageChange($event)" />
      </div>
    </div>
  `,
})
export class StoreDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly productService = inject(ProductService);

  readonly store = signal<Store | null>(null);
  readonly products = signal<Product[]>([]);
  readonly loading = signal<boolean>(true);
  readonly totalProducts = signal<number>(0);
  readonly totalPages = signal<number>(0);
  readonly currentPage = signal<number>(0);
  private readonly pageSize = 12;

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id')!;
    this.productService.getStore(id).subscribe({
      next: (store) => this.store.set(store),
    });
    this.fetchProducts(id);
  }

  fetchProducts(storeId?: string): void {
    const id = storeId ?? this.store()?.id;
    if (!id) return;
    this.loading.set(true);
    this.productService.list({ storeId: id, page: this.currentPage(), size: this.pageSize }).subscribe({
      next: (res) => {
        this.products.set(res.content);
        this.totalProducts.set(res.totalElements);
        this.totalPages.set(res.totalPages);
        this.loading.set(false);
      },
      error: () => {
        this.products.set([]);
        this.loading.set(false);
      },
    });
  }

  onPageChange(page: number): void {
    this.currentPage.set(page);
    this.fetchProducts();
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }
}
