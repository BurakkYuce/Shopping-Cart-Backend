import { Routes } from '@angular/router';

import { SellerShellComponent } from './seller-shell/seller-shell.component';

export const SELLER_ROUTES: Routes = [
  {
    path: '',
    component: SellerShellComponent,
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./overview/overview.component').then((m) => m.SellerOverviewComponent),
      },
      {
        path: 'products',
        loadComponent: () =>
          import('./products/seller-products.component').then((m) => m.SellerProductsComponent),
      },
      {
        path: 'orders',
        loadComponent: () =>
          import('./orders/seller-orders.component').then((m) => m.SellerOrdersComponent),
      },
      {
        path: 'analytics',
        loadComponent: () =>
          import('./analytics/seller-analytics.component').then((m) => m.SellerAnalyticsComponent),
      },
      {
        path: 'settings',
        loadComponent: () =>
          import('./settings/seller-settings.component').then((m) => m.SellerSettingsComponent),
      },
    ],
  },
];
