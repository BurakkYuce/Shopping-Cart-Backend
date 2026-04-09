import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  Product,
  CreateProductRequest,
  UpdateProductRequest,
  ProductFilters,
  Category,
  Store,
  Review,
  CreateReviewRequest,
} from '../models/product.models';
import { PagedResponse, PageParams } from '../models/common.models';

@Injectable({ providedIn: 'root' })
export class ProductService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.apiUrl;

  /* ---------- Products ---------- */
  list(filters: ProductFilters & PageParams = {}): Observable<PagedResponse<Product>> {
    let params = new HttpParams();
    Object.entries(filters).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') {
        params = params.set(key, String(value));
      }
    });
    return this.http.get<PagedResponse<Product>>(`${this.baseUrl}/products`, { params });
  }

  get(id: string): Observable<Product> {
    return this.http.get<Product>(`${this.baseUrl}/products/${id}`);
  }

  create(body: CreateProductRequest): Observable<Product> {
    return this.http.post<Product>(`${this.baseUrl}/products`, body);
  }

  update(id: string, body: UpdateProductRequest): Observable<Product> {
    return this.http.put<Product>(`${this.baseUrl}/products/${id}`, body);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/products/${id}`);
  }

  /* ---------- Categories ---------- */
  listCategories(): Observable<Category[]> {
    return this.http.get<Category[]>(`${this.baseUrl}/categories`);
  }

  getCategory(id: string): Observable<Category> {
    return this.http.get<Category>(`${this.baseUrl}/categories/${id}`);
  }

  /* ---------- Stores ---------- */
  listStores(): Observable<Store[]> {
    // Backend returns plain List<StoreResponse>.
    return this.http.get<Store[]>(`${this.baseUrl}/stores`);
  }

  getStore(id: string): Observable<Store> {
    return this.http.get<Store>(`${this.baseUrl}/stores/${id}`);
  }

  /* ---------- Reviews ---------- */
  listReviews(productId: string): Observable<Review[]> {
    return this.http.get<Review[]>(`${this.baseUrl}/reviews/by-product/${productId}`);
  }

  createReview(body: CreateReviewRequest): Observable<Review> {
    return this.http.post<Review>(`${this.baseUrl}/reviews`, body);
  }
}
