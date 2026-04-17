import { Injectable, inject, computed, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, switchMap, tap } from 'rxjs';

import { environment } from '../../../environments/environment';
import { Cart, AddToCartRequest, UpdateCartItemRequest } from '../models/cart.models';
import { Order } from '../models/order.models';
import { AuthService } from './auth.service';

const EMPTY_CART: Cart = { items: [], totalItems: 0, totalPrice: 0 };

@Injectable({ providedIn: 'root' })
export class CartService {
  private readonly http = inject(HttpClient);
  private readonly authService = inject(AuthService);
  private readonly baseUrl = `${environment.apiUrl}/cart`;

  // BehaviorSubject — syncs cart badge across navbar + cart page
  private readonly _cart$ = new BehaviorSubject<Cart>(EMPTY_CART);
  readonly cart$ = this._cart$.asObservable();

  // Signal mirror for template ergonomics
  private readonly _cartSig = signal<Cart>(EMPTY_CART);
  readonly cartSig = this._cartSig.asReadonly();
  readonly itemCount = computed(() => this._cartSig().totalItems);
  readonly totalPrice = computed(() => this._cartSig().totalPrice);

  // Coupon code applied on cart page, consumed during checkout
  private readonly _appliedCouponCode = signal<string | null>(null);
  readonly appliedCouponCode = this._appliedCouponCode.asReadonly();

  setCouponCode(code: string | null): void {
    this._appliedCouponCode.set(code);
  }

  /* ---------- CRUD ---------- */
  fetch(): Observable<Cart> {
    return this.http.get<Cart>(this.baseUrl).pipe(tap((cart) => this.updateCart(cart)));
  }

  add(body: AddToCartRequest): Observable<Cart> {
    return this.http.post<Cart>(`${this.baseUrl}/items`, body).pipe(tap((cart) => this.updateCart(cart)));
  }

  updateItem(productId: string, body: UpdateCartItemRequest): Observable<Cart> {
    return this.http
      .patch<Cart>(`${this.baseUrl}/items/${productId}`, body)
      .pipe(tap((cart) => this.updateCart(cart)));
  }

  removeItem(productId: string): Observable<Cart> {
    // Backend returns 204 No Content on delete — re-fetch the cart so the UI stays in sync.
    return this.http
      .delete<void>(`${this.baseUrl}/items/${productId}`)
      .pipe(switchMap(() => this.fetch()));
  }

  clear(): Observable<Cart> {
    return this.http.delete<void>(this.baseUrl).pipe(
      tap(() => this.updateCart(EMPTY_CART)),
      switchMap(() => this._cart$),
    );
  }

  /**
   * Server-side checkout that converts the current cart into orders grouped by store.
   * Backend: POST /cart/checkout body {paymentMethod, consents} → Order[]
   */
  checkout(paymentMethod: string, couponCode?: string): Observable<Order[]> {
    const body: Record<string, unknown> = {
      paymentMethod,
      kvkkConsent: true,
      distanceSaleConsent: true,
      preInformationConsent: true,
    };
    const code = couponCode ?? this._appliedCouponCode();
    if (code) body['couponCode'] = code;
    return this.http
      .post<Order[]>(`${this.baseUrl}/checkout`, body)
      .pipe(tap(() => {
        this.updateCart(EMPTY_CART);
        this._appliedCouponCode.set(null);
      }));
  }

  /* ---------- Helpers ---------- */
  reload(): void {
    if (this.authService.isAuthenticated()) {
      this.fetch().subscribe({ error: () => this.updateCart(EMPTY_CART) });
    } else {
      this.updateCart(EMPTY_CART);
    }
  }

  reset(): void {
    this.updateCart(EMPTY_CART);
  }

  private updateCart(cart: Cart): void {
    this._cart$.next(cart);
    this._cartSig.set(cart);
  }
}
