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
  revenueByWeek?: Record<string, number>;
  revenueByMonth?: Record<string, number>;
  revenueByCategory?: Record<string, number>;
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

/** Mirrors backend platform-overview response. */
export interface PlatformOverview {
  totalGmv: number;
  totalOrders: number;
  totalUsers: number;
  totalStores: number;
  topStores: { storeId: string; storeName: string; revenue: number }[];
}

/** Mirrors backend StoreKpiResponse. */
export interface StoreKpi {
  storeId: string;
  storeName: string;
  totalRevenue: number;
  orderCount: number;
  averageOrderValue: number;
  customerCount: number;
  topProducts: TopProduct[];
  ratingDistribution: Record<number, number>;
}

/** Mirrors backend CustomerSegmentResponse. */
export interface CustomerSegments {
  segments: Segment[];
  totalCustomers: number;
}

export interface Segment {
  name: string;
  count: number;
  percentage: number;
  avgSpend: number;
}

export interface SalesParams {
  from?: string;
  to?: string;
  groupBy?: 'day' | 'week' | 'month' | 'category';
  storeId?: string;
}

@Injectable({ providedIn: 'root' })
export class AnalyticsService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/analytics`;

  getSales(options?: SalesParams): Observable<AnalyticsSales> {
    let params = new HttpParams();
    if (options?.from) params = params.set('from', options.from);
    if (options?.to) params = params.set('to', options.to);
    if (options?.groupBy) params = params.set('groupBy', options.groupBy);
    if (options?.storeId) params = params.set('storeId', options.storeId);
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

  getPlatformOverview(): Observable<PlatformOverview> {
    return this.http.get<PlatformOverview>(`${this.baseUrl}/platform-overview`);
  }

  getStoreKpis(storeId: string): Observable<StoreKpi> {
    return this.http.get<StoreKpi>(`${this.baseUrl}/store-kpis/${storeId}`);
  }

  getCustomerSegments(storeId?: string): Observable<CustomerSegments> {
    let params = new HttpParams();
    if (storeId) params = params.set('storeId', storeId);
    return this.http.get<CustomerSegments>(`${this.baseUrl}/customer-segments`, { params });
  }

  getStoreComparison(): Observable<StoreKpi[]> {
    return this.http.get<StoreKpi[]>(`${this.baseUrl}/store-comparison`);
  }
}
