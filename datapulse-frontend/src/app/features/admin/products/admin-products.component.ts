import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { ProductService } from '../../../core/services/product.service';
import { Product } from '../../../core/models/product.models';
import { NotificationService } from '../../../core/services/notification.service';
import { SpinnerComponent } from '../../../shared/components/spinner/spinner.component';
import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';

@Component({
  selector: 'app-admin-products',
  standalone: true,
  imports: [CommonModule, FormsModule, SpinnerComponent, PaginationComponent],
  template: `
    <header class="sticky top-0 z-40 flex items-center justify-between border-b border-outline/40 bg-background/90 px-12 py-6 backdrop-blur-xl">
      <div class="flex items-center gap-8">
        <span class="text-2xl font-bold tracking-tight text-text-primary">DataPulse</span>
        <h2 class="border-l border-outline/40 pl-8 text-xl font-semibold text-text-primary">Product catalog</h2>
      </div>
      <div class="relative">
        <span class="material-symbols-outlined pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-text-tertiary" style="font-size: 18px">search</span>
        <input type="search"
               [ngModel]="query()"
               (ngModelChange)="query.set($event)"
               (keyup.enter)="search()"
               placeholder="Search products..."
               class="h-10 w-72 rounded-full bg-background-sub pl-11 pr-4 text-sm outline-none focus:bg-background-raised" />
      </div>
    </header>

    <section class="px-12 py-10">
      <app-spinner *ngIf="loading()"></app-spinner>

      <div *ngIf="!loading()" class="overflow-hidden rounded-[2rem] bg-background-raised shadow-atelier">
        <table class="w-full text-left">
          <thead>
            <tr class="border-b border-outline/30 text-[10px] font-bold uppercase tracking-[0.15em] text-text-tertiary">
              <th class="px-8 py-5">Product</th>
              <th class="px-6 py-5">SKU</th>
              <th class="px-6 py-5">Price</th>
              <th class="px-6 py-5">Stock</th>
              <th class="px-6 py-5">Rating</th>
              <th class="px-8 py-5 text-right">Actions</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-outline/20">
            <tr *ngFor="let p of products()" class="hover:bg-background-sub/50">
              <td class="px-8 py-5">
                <div class="flex items-center gap-4">
                  <div class="h-12 w-12 overflow-hidden rounded-xl bg-background-sub">
                    <img *ngIf="p.imageUrl" [src]="p.imageUrl" [alt]="p.name" class="h-full w-full object-cover" />
                  </div>
                  <div>
                    <p class="text-sm font-semibold text-text-primary">{{ p.name }}</p>
                    <p class="text-xs text-text-tertiary">{{ p.brand || '—' }}</p>
                  </div>
                </div>
              </td>
              <td class="px-6 py-5 text-xs font-mono text-text-secondary">{{ p.sku }}</td>
              <td class="px-6 py-5 text-sm font-bold text-text-primary">\${{ p.unitPrice | number:'1.2-2' }}</td>
              <td class="px-6 py-5">
                <span class="rounded-full px-2.5 py-1 text-[10px] font-black uppercase"
                      [class.bg-success]="p.stockQuantity > 10"
                      [class.text-white]="p.stockQuantity > 10"
                      [class.bg-warning]="p.stockQuantity > 0 && p.stockQuantity <= 10"
                      [class.text-text-primary]="p.stockQuantity > 0 && p.stockQuantity <= 10"
                      [class.bg-danger]="p.stockQuantity <= 0">
                  {{ p.stockQuantity }} left
                </span>
              </td>
              <td class="px-6 py-5">
                <div class="flex items-center gap-1">
                  <span class="material-symbols-outlined filled text-primary" style="font-size: 16px">star</span>
                  <span class="text-sm font-semibold text-text-primary">{{ p.rating ? (p.rating | number:'1.1-1') : '—' }}</span>
                </div>
              </td>
              <td class="px-8 py-5 text-right">
                <button (click)="remove(p)" class="rounded-lg p-2 text-text-tertiary hover:bg-danger/10 hover:text-danger" aria-label="Delete">
                  <span class="material-symbols-outlined" style="font-size: 18px">delete</span>
                </button>
              </td>
            </tr>
            <tr *ngIf="products().length === 0">
              <td colspan="6" class="py-16 text-center text-sm text-text-tertiary">No products match.</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div *ngIf="!loading() && totalPages() > 1" class="mt-6">
        <app-pagination [currentPage]="currentPage()" [totalPages]="totalPages()" [totalElements]="totalElements()" (pageChange)="onPageChange($event)"></app-pagination>
      </div>
    </section>
  `,
})
export class AdminProductsComponent implements OnInit {
  private readonly productService = inject(ProductService);
  private readonly toast = inject(NotificationService);

  readonly products = signal<Product[]>([]);
  readonly loading = signal<boolean>(true);
  readonly query = signal<string>('');
  readonly currentPage = signal<number>(0);
  readonly totalPages = signal<number>(0);
  readonly totalElements = signal<number>(0);
  readonly pageSize = 15;

  ngOnInit(): void {
    this.fetch();
  }

  fetch(): void {
    this.loading.set(true);
    this.productService
      .list({ q: this.query() || undefined, page: this.currentPage(), size: this.pageSize })
      .subscribe({
        next: (res) => {
          this.products.set(res.content);
          this.totalPages.set(res.totalPages);
          this.totalElements.set(res.totalElements);
          this.loading.set(false);
        },
        error: () => {
          this.products.set([]);
          this.loading.set(false);
        },
      });
  }

  search(): void {
    this.currentPage.set(0);
    this.fetch();
  }

  onPageChange(p: number): void {
    this.currentPage.set(p);
    this.fetch();
  }

  remove(p: Product): void {
    if (!confirm(`Delete "${p.name}"? This cannot be undone.`)) return;
    this.productService.delete(p.id).subscribe({
      next: () => {
        this.toast.success('Product deleted.');
        this.fetch();
      },
      error: () => this.toast.error('Could not delete product.'),
    });
  }
}
