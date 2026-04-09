import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { Order, CreateOrderRequest, OrderStatus, Shipment } from '../models/order.models';
import { PagedResponse, PageParams } from '../models/common.models';

@Injectable({ providedIn: 'root' })
export class OrderService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.apiUrl;

  list(params: PageParams & { status?: OrderStatus } = {}): Observable<PagedResponse<Order>> {
    let httpParams = new HttpParams();
    Object.entries(params).forEach(([k, v]) => {
      if (v !== undefined && v !== null && v !== '') {
        httpParams = httpParams.set(k, String(v));
      }
    });
    return this.http.get<PagedResponse<Order>>(`${this.baseUrl}/orders`, { params: httpParams });
  }

  get(id: string): Observable<Order> {
    return this.http.get<Order>(`${this.baseUrl}/orders/${id}`);
  }

  create(body: CreateOrderRequest): Observable<Order> {
    return this.http.post<Order>(`${this.baseUrl}/orders`, body);
  }

  cancel(id: string): Observable<Order> {
    return this.http.post<Order>(`${this.baseUrl}/orders/${id}/cancel`, {});
  }

  requestReturn(id: string): Observable<Order> {
    return this.http.post<Order>(`${this.baseUrl}/orders/${id}/return`, {});
  }

  updateStatus(id: string, status: OrderStatus | string): Observable<Order> {
    return this.http.patch<Order>(`${this.baseUrl}/orders/${id}/status`, { status });
  }

  /* ---------- Seller side: same endpoint, scoped by server-side role check ---------- */
  listSellerOrders(params: PageParams & { status?: OrderStatus } = {}): Observable<PagedResponse<Order>> {
    return this.list(params);
  }

  markShipped(orderId: string): Observable<Order> {
    // Backend exposes a single status PATCH; shipping is modeled as a status transition.
    return this.updateStatus(orderId, 'shipped');
  }

  /* ---------- Shipments ---------- */
  getShipmentByOrder(orderId: string): Observable<Shipment> {
    return this.http.get<Shipment>(`${this.baseUrl}/shipments/by-order/${orderId}`);
  }
}
