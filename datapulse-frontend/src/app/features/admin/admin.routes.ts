import { Routes } from '@angular/router';

import { AdminShellComponent } from './admin-shell/admin-shell.component';

export const ADMIN_ROUTES: Routes = [
  {
    path: '',
    component: AdminShellComponent,
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./overview/admin-overview.component').then((m) => m.AdminOverviewComponent),
      },
      {
        path: 'users',
        loadComponent: () =>
          import('./users/admin-users.component').then((m) => m.AdminUsersComponent),
      },
      {
        path: 'products',
        loadComponent: () =>
          import('./products/admin-products.component').then((m) => m.AdminProductsComponent),
      },
      {
        path: 'orders',
        loadComponent: () =>
          import('./orders/admin-orders.component').then((m) => m.AdminOrdersComponent),
      },
      {
        path: 'stores',
        loadComponent: () =>
          import('./stores/admin-stores.component').then((m) => m.AdminStoresComponent),
      },
      {
        path: 'coupons',
        loadComponent: () =>
          import('./coupons/admin-coupons.component').then((m) => m.AdminCouponsComponent),
      },
      {
        path: 'analytics',
        loadComponent: () =>
          import('./analytics/admin-analytics.component').then((m) => m.AdminAnalyticsComponent),
      },
    ],
  },
];
