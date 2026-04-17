import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { CouponValidation, Coupon, CreateCouponRequest } from '../models/coupon.models';

@Injectable({ providedIn: 'root' })
export class CouponService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/coupons`;
  private readonly adminUrl = `${environment.apiUrl}/admin/coupons`;

  validate(code: string, subtotal: number): Observable<CouponValidation> {
    return this.http.post<CouponValidation>(`${this.baseUrl}/validate`, { code, subtotal });
  }

  /* ---------- Admin CRUD ---------- */
  listCoupons(): Observable<Coupon[]> {
    return this.http.get<Coupon[]>(this.adminUrl);
  }

  createCoupon(body: CreateCouponRequest): Observable<Coupon> {
    return this.http.post<Coupon>(this.adminUrl, body);
  }

  updateCoupon(id: string, body: Partial<CreateCouponRequest>): Observable<Coupon> {
    return this.http.patch<Coupon>(`${this.adminUrl}/${id}`, body);
  }

  deactivateCoupon(id: string): Observable<void> {
    return this.http.delete<void>(`${this.adminUrl}/${id}`);
  }
}
