import { Routes } from '@angular/router';

import { authGuard } from '../../core/guards/auth.guard';

export const ORDERS_ROUTES: Routes = [
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./orders-list/orders-list.component').then((m) => m.OrdersListComponent),
  },
  {
    path: ':id',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./order-detail/order-detail.component').then((m) => m.OrderDetailComponent),
  },
  {
    path: ':id/track',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./shipment-tracking/shipment-tracking.component').then((m) => m.ShipmentTrackingComponent),
  },
  {
    path: ':id/confirmation',
    canActivate: [authGuard],
    loadComponent: () =>
      import('../checkout/order-confirmation/order-confirmation.component').then(
        (m) => m.OrderConfirmationComponent,
      ),
  },
];
