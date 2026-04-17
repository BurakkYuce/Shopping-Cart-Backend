import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Observable, map, of, catchError, throwError } from 'rxjs';

import { environment } from '../../../environments/environment';
import { Order, CreateOrderRequest, OrderStatus, Shipment } from '../models/order.models';
import { PagedResponse, PageParams } from '../models/common.models';

@Injectable({ providedIn: 'root' })
export class OrderService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.apiUrl;

  /** Backend returns statuses in UPPERCASE ('PENDING', 'PROCESSING'…). The whole frontend
   *  (templates, status-badge, comparisons) works in lowercase, so normalise at the edge. */
  private normalize(o: Order): Order {
    return {
      ...o,
      status: (typeof o.status === 'string' ? o.status.toLowerCase() : o.status) as OrderStatus,
      shipmentStatus: typeof o.shipmentStatus === 'string' ? o.shipmentStatus.toLowerCase() : o.shipmentStatus,
    };
  }

  list(params: PageParams & { status?: OrderStatus } = {}): Observable<PagedResponse<Order>> {
    let httpParams = new HttpParams();
    Object.entries(params).forEach(([k, v]) => {
      if (v !== undefined && v !== null && v !== '') {
        httpParams = httpParams.set(k, String(v));
      }
    });
    return this.http.get<PagedResponse<Order>>(`${this.baseUrl}/orders`, { params: httpParams }).pipe(
      map((res) => ({ ...res, content: res.content.map((o) => this.normalize(o)) })),
    );
  }

  get(id: string): Observable<Order> {
    return this.http.get<Order>(`${this.baseUrl}/orders/${id}`).pipe(map((o) => this.normalize(o)));
  }

  create(body: CreateOrderRequest): Observable<Order> {
    return this.http.post<Order>(`${this.baseUrl}/orders`, body).pipe(map((o) => this.normalize(o)));
  }

  cancel(id: string): Observable<Order> {
    return this.http.post<Order>(`${this.baseUrl}/orders/${id}/cancel`, {}).pipe(map((o) => this.normalize(o)));
  }

  requestReturn(id: string, reason: string): Observable<unknown> {
    // Backend creates a ReturnRequest and flips the order to RETURNED server-side; caller should
    // refetch the order to pick up the new status.
    return this.http.post<unknown>(`${this.baseUrl}/orders/${id}/return-requests`, { reason });
  }

  updateStatus(id: string, status: OrderStatus | string): Observable<Order> {
    return this.http.patch<Order>(`${this.baseUrl}/orders/${id}/status`, { status }).pipe(map((o) => this.normalize(o)));
  }

  /* ---------- Seller side: same endpoint, scoped by server-side role check ---------- */
  listSellerOrders(params: PageParams & { status?: OrderStatus } = {}): Observable<PagedResponse<Order>> {
    // Backend enum accepts uppercase; forward filter value in the casing it expects.
    const forwarded: any = { ...params };
    if (forwarded.status) forwarded.status = String(forwarded.status).toUpperCase();
    return this.list(forwarded);
  }

  markProcessing(orderId: string): Observable<Order> {
    return this.updateStatus(orderId, 'PROCESSING');
  }

  markShipped(orderId: string): Observable<Order> {
    return this.updateStatus(orderId, 'SHIPPED');
  }

  /* ---------- Shipments ---------- */
  /** Orders in PENDING/PROCESSING have no shipment row yet; treat 404 as "not shipped yet"
   *  (returns null) so tracking page can show the empty state without a noisy console error. */
  getShipmentByOrder(orderId: string): Observable<Shipment | null> {
    return this.http.get<Shipment>(`${this.baseUrl}/shipments/by-order/${orderId}`).pipe(
      catchError((err: HttpErrorResponse) =>
        err.status === 404 ? of(null) : throwError(() => err)),
    );
  }
}
