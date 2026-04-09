import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';

import { ProductService } from '../../../core/services/product.service';
import { Product, Category } from '../../../core/models/product.models';
import { NotificationService } from '../../../core/services/notification.service';
import { SpinnerComponent } from '../../../shared/components/spinner/spinner.component';
import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';
import { StatusBadgeComponent } from '../../../shared/components/status-badge/status-badge.component';

type ModalTab = 'general' | 'assets' | 'pricing' | 'shipping';

@Component({
  selector: 'app-seller-products',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, SpinnerComponent, PaginationComponent, StatusBadgeComponent],
  templateUrl: './seller-products.component.html',
})
export class SellerProductsComponent implements OnInit {
  private readonly productService = inject(ProductService);
  private readonly fb = inject(FormBuilder);
  private readonly toast = inject(NotificationService);

  readonly Math = Math;

  readonly products = signal<Product[]>([]);
  readonly categories = signal<Category[]>([]);
  readonly loading = signal<boolean>(true);
  readonly query = signal<string>('');
  readonly currentPage = signal<number>(0);
  readonly totalPages = signal<number>(0);
  readonly totalElements = signal<number>(0);
  readonly pageSize = 12;

  readonly modalOpen = signal<boolean>(false);
  readonly modalTab = signal<ModalTab>('general');
  readonly editingId = signal<string | null>(null);
  readonly saving = signal<boolean>(false);

  readonly form = this.fb.nonNullable.group({
    name: ['', Validators.required],
    sku: [''],
    description: [''],
    categoryId: ['', Validators.required],
    storeId: ['', Validators.required],
    imageUrl: [''],
    unitPrice: [0, [Validators.required, Validators.min(0)]],
    stockQuantity: [0, [Validators.required, Validators.min(0)]],
  });

  ngOnInit(): void {
    this.productService.listCategories().subscribe({ next: (cats) => this.categories.set(cats) });
    // Seeds only populate one store today — use it as the default for new pieces
    // until we support multi-store sellers in the UI.
    this.productService.listStores().subscribe({
      next: (stores) => {
        if (stores.length) this.form.patchValue({ storeId: stores[0].id });
      },
    });
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
        error: () => this.loading.set(false),
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

  openNew(): void {
    this.editingId.set(null);
    const storeId = this.form.value.storeId ?? '';
    this.form.reset({
      name: '', sku: '', description: '', categoryId: '', storeId,
      imageUrl: '', unitPrice: 0, stockQuantity: 0,
    });
    this.modalTab.set('general');
    this.modalOpen.set(true);
  }

  edit(p: Product): void {
    this.editingId.set(p.id);
    this.form.patchValue({
      name: p.name,
      sku: p.sku,
      description: p.description ?? '',
      categoryId: p.categoryId,
      storeId: p.storeId,
      imageUrl: p.imageUrl ?? '',
      unitPrice: p.unitPrice,
      stockQuantity: p.stockQuantity,
    });
    this.modalTab.set('general');
    this.modalOpen.set(true);
  }

  closeModal(): void {
    this.modalOpen.set(false);
  }

  setTab(t: ModalTab): void {
    this.modalTab.set(t);
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.toast.error('Please complete all required fields.');
      return;
    }
    this.saving.set(true);
    const id = this.editingId();
    const payload = this.form.getRawValue();
    const obs = id ? this.productService.update(id, payload) : this.productService.create(payload);
    obs.subscribe({
      next: () => {
        this.toast.success(id ? 'Product updated.' : 'Product created.');
        this.saving.set(false);
        this.modalOpen.set(false);
        this.fetch();
      },
      error: () => {
        this.saving.set(false);
        this.toast.error('Could not save product.');
      },
    });
  }

  delete(p: Product): void {
    if (!confirm(`Delete "${p.name}"?`)) return;
    this.productService.delete(p.id).subscribe({
      next: () => {
        this.toast.success('Product deleted.');
        this.fetch();
      },
      error: () => this.toast.error('Could not delete.'),
    });
  }

  stockStatus(p: Product): 'active' | 'draft' | 'inactive' {
    if (p.stockQuantity <= 0) return 'inactive';
    if (p.stockQuantity < 5) return 'draft';
    return 'active';
  }
}
