import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map, switchMap, tap } from 'rxjs';

import { environment } from '../../../environments/environment';
import { WishlistItem } from '../models/user.models';

// Backend returns ProductResponse[] for GET /wishlist
interface ProductResponse {
  id: string;
  name?: string;
  unitPrice?: number;
  imageUrl?: string;
  stockQuantity?: number;
}

@Injectable({ providedIn: 'root' })
export class WishlistService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/wishlist`;

  private readonly _items = signal<WishlistItem[]>([]);
  readonly items = this._items.asReadonly();
  readonly count = signal<number>(0);

  list(): Observable<WishlistItem[]> {
    return this.http.get<ProductResponse[]>(this.baseUrl).pipe(
      map((products) =>
        products.map((p) => ({
          id: p.id,
          productId: p.id,           // product ID is the wishlist key
          productName: p.name,
          unitPrice: p.unitPrice,
          imageUrl: p.imageUrl,
          inStock: (p.stockQuantity ?? 0) > 0,
        } as WishlistItem))
      ),
      tap((items) => {
        this._items.set(items);
        this.count.set(items.length);
      }),
    );
  }

  add(productId: string): Observable<WishlistItem[]> {
    return this.http.post<any>(`${this.baseUrl}/${productId}`, {}).pipe(
      switchMap(() => this.list()),
    );
  }

  remove(productId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${productId}`).pipe(
      tap(() => {
        const updated = this._items().filter((i) => i.productId !== productId);
        this._items.set(updated);
        this.count.set(updated.length);
      }),
    );
  }

  isInWishlist(productId: string): boolean {
    return this._items().some((i) => i.productId === productId);
  }

  reset(): void {
    this._items.set([]);
    this.count.set(0);
  }
}
