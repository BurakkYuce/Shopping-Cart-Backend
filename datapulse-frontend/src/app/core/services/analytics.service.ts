import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';

/** Mirrors backend AnalyticsSalesResponse. */
export interface AnalyticsSales {
  totalRevenue: number;
  orderCount: number;
  averageOrderValue: number;
  revenueByDay: Record<string, number>;
  fromDate?: string;
  toDate?: string;
}

/** Mirrors backend AnalyticsCustomerResponse. */
export interface AnalyticsCustomers {
  averageAge: number;
  spendByMembership: Record<string, number>;
  satisfactionDistribution: Record<string, number>;
  topCities: Record<string, number>;
}

/** Mirrors backend AnalyticsProductResponse. */
export interface AnalyticsProducts {
  topSellingProducts: TopProduct[];
  avgRatingByCategory: Record<string, number>;
}

export interface TopProduct {
  productId: string;
  productName: string;
  totalQuantity: number;
  totalRevenue: number;
}

@Injectable({ providedIn: 'root' })
export class AnalyticsService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/analytics`;

  getSales(from?: string, to?: string): Observable<AnalyticsSales> {
    let params = new HttpParams();
    if (from) params = params.set('from', from);
    if (to) params = params.set('to', to);
    return this.http.get<AnalyticsSales>(`${this.baseUrl}/sales`, { params });
  }

  getCustomers(): Observable<AnalyticsCustomers> {
    return this.http.get<AnalyticsCustomers>(`${this.baseUrl}/customers`);
  }

  getProducts(storeId?: string): Observable<AnalyticsProducts> {
    let params = new HttpParams();
    if (storeId) params = params.set('storeId', storeId);
    return this.http.get<AnalyticsProducts>(`${this.baseUrl}/products`, { params });
  }
}
