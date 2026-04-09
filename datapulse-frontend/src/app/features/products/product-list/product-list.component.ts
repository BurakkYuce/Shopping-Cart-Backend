import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';

import { ProductService } from '../../../core/services/product.service';
import { Product, Category, ProductFilters } from '../../../core/models/product.models';
import { PagedResponse } from '../../../core/models/common.models';
import { ProductCardComponent } from '../../../shared/components/product-card/product-card.component';
import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';
import { SpinnerComponent } from '../../../shared/components/spinner/spinner.component';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';

type ViewMode = 'grid' | 'list';

@Component({
  selector: 'app-product-list',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    FormsModule,
    ProductCardComponent,
    PaginationComponent,
    SpinnerComponent,
    EmptyStateComponent,
  ],
  templateUrl: './product-list.component.html',
})
export class ProductListComponent implements OnInit {
  private readonly productService = inject(ProductService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly products = signal<Product[]>([]);
  readonly categories = signal<Category[]>([]);
  readonly loading = signal<boolean>(true);
  readonly totalElements = signal<number>(0);
  readonly totalPages = signal<number>(0);
  readonly currentPage = signal<number>(0);
  readonly pageSize = 12;

  readonly view = signal<ViewMode>('grid');
  readonly sort = signal<string>('createdAt,desc');
  readonly query = signal<string>('');
  readonly categoryId = signal<string | null>(null);
  readonly brandFilter = signal<string>('');
  readonly priceMin = signal<number | null>(null);
  readonly priceMax = signal<number | null>(null);
  readonly minRating = signal<number | null>(null);

  readonly sortOptions = [
    { value: 'createdAt,desc', label: 'Newest' },
    { value: 'rating,desc', label: 'Top rated' },
    { value: 'unitPrice,asc', label: 'Price: low to high' },
    { value: 'unitPrice,desc', label: 'Price: high to low' },
    { value: 'name,asc', label: 'Name: A–Z' },
  ];

  readonly activeCategory = computed<Category | null>(() => {
    const id = this.categoryId();
    if (id == null) return null;
    return this.categories().find((c) => c.id === id) ?? null;
  });

  ngOnInit(): void {
    this.productService.listCategories().subscribe({
      next: (cats) => this.categories.set(cats),
      error: () => {},
    });
    this.route.queryParams.subscribe((params) => {
      this.query.set(params['q'] ?? '');
      this.categoryId.set(params['categoryId'] ?? null);
      this.brandFilter.set(params['brand'] ?? '');
      this.priceMin.set(params['minPrice'] ? Number(params['minPrice']) : null);
      this.priceMax.set(params['maxPrice'] ? Number(params['maxPrice']) : null);
      this.minRating.set(params['minRating'] ? Number(params['minRating']) : null);
      this.currentPage.set(params['page'] ? Number(params['page']) : 0);
      this.sort.set(params['sort'] ?? 'createdAt,desc');
      this.fetch();
    });
  }

  fetch(): void {
    this.loading.set(true);
    const filters: ProductFilters & { page?: number; size?: number; sort?: string } = {
      q: this.query() || undefined,
      categoryId: this.categoryId() ?? undefined,
      brand: this.brandFilter() || undefined,
      minPrice: this.priceMin() ?? undefined,
      maxPrice: this.priceMax() ?? undefined,
      minRating: this.minRating() ?? undefined,
      page: this.currentPage(),
      size: this.pageSize,
      sort: this.sort(),
    };
    this.productService.list(filters).subscribe({
      next: (res: PagedResponse<Product>) => {
        // Bring products with real imagery to the front of the page so the
        // grid doesn't lead with placeholder tiles — the seed CSVs only
        // populate imageUrl on a fraction of rows.
        const withImage = res.content.filter((p) => !!p.imageUrl);
        const withoutImage = res.content.filter((p) => !p.imageUrl);
        this.products.set([...withImage, ...withoutImage]);
        this.totalElements.set(res.totalElements);
        this.totalPages.set(res.totalPages);
        this.loading.set(false);
      },
      error: () => {
        this.products.set([]);
        this.totalElements.set(0);
        this.totalPages.set(0);
        this.loading.set(false);
      },
    });
  }

  setView(v: ViewMode): void {
    this.view.set(v);
  }

  applySort(value: string): void {
    this.sort.set(value);
    this.updateQueryParams();
  }

  applyCategory(id: string | null): void {
    this.categoryId.set(id);
    this.currentPage.set(0);
    this.updateQueryParams();
  }

  applyFilters(): void {
    this.currentPage.set(0);
    this.updateQueryParams();
  }

  clearFilters(): void {
    this.brandFilter.set('');
    this.priceMin.set(null);
    this.priceMax.set(null);
    this.minRating.set(null);
    this.categoryId.set(null);
    this.currentPage.set(0);
    this.updateQueryParams();
  }

  onPageChange(page: number): void {
    this.currentPage.set(page);
    this.updateQueryParams();
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  private updateQueryParams(): void {
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: {
        q: this.query() || null,
        categoryId: this.categoryId() ?? null,
        brand: this.brandFilter() || null,
        minPrice: this.priceMin() ?? null,
        maxPrice: this.priceMax() ?? null,
        minRating: this.minRating() ?? null,
        page: this.currentPage() || null,
        sort: this.sort() === 'createdAt,desc' ? null : this.sort(),
      },
      queryParamsHandling: 'merge',
    });
  }
}
