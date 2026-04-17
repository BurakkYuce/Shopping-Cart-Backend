import { Routes } from '@angular/router';

import { CustomerShellComponent } from './shared/layouts/customer-shell/customer-shell.component';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';

export const routes: Routes = [
  {
    path: '',
    component: CustomerShellComponent,
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./features/home/home.component').then((m) => m.HomeComponent),
      },
      {
        path: 'products',
        loadChildren: () =>
          import('./features/products/products.routes').then((m) => m.PRODUCTS_ROUTES),
      },
      {
        path: 'visual-search',
        loadComponent: () =>
          import('./features/products/visual-search/visual-search.component').then(
            (m) => m.VisualSearchComponent,
          ),
      },
      {
        path: 'cart',
        canActivate: [authGuard],
        loadComponent: () =>
          import('./features/cart/cart-page/cart-page.component').then((m) => m.CartPageComponent),
      },
      {
        path: 'checkout',
        canActivate: [authGuard],
        loadComponent: () =>
          import('./features/checkout/checkout.component').then((m) => m.CheckoutComponent),
      },
      {
        path: 'orders',
        canActivate: [authGuard],
        loadChildren: () =>
          import('./features/orders/orders.routes').then((m) => m.ORDERS_ROUTES),
      },
      {
        path: 'wishlist',
        canActivate: [authGuard],
        loadComponent: () =>
          import('./features/account/wishlist/wishlist.component').then((m) => m.WishlistComponent),
      },
      {
        path: 'addresses',
        canActivate: [authGuard],
        loadComponent: () =>
          import('./features/account/addresses/addresses.component').then(
            (m) => m.AddressesComponent,
          ),
      },
      {
        path: 'profile',
        canActivate: [authGuard],
        loadComponent: () =>
          import('./features/account/profile/profile.component').then((m) => m.ProfileComponent),
      },
      {
        path: 'stores/:id',
        loadComponent: () =>
          import('./features/stores/store-detail/store-detail.component').then(
            (m) => m.StoreDetailComponent,
          ),
      },
      {
        path: 'notifications',
        canActivate: [authGuard],
        loadComponent: () =>
          import('./features/account/notifications/notifications.component').then(
            (m) => m.NotificationsComponent,
          ),
      },
      {
        path: 'returns',
        canActivate: [authGuard],
        loadComponent: () =>
          import('./features/account/returns/returns.component').then((m) => m.ReturnsComponent),
      },
    ],
  },
  {
    path: 'auth',
    loadChildren: () => import('./features/auth/auth.routes').then((m) => m.AUTH_ROUTES),
  },
  {
    path: 'seller',
    canActivate: [roleGuard(['CORPORATE'])],
    loadChildren: () =>
      import('./features/seller/seller.routes').then((m) => m.SELLER_ROUTES),
  },
  {
    path: 'admin',
    canActivate: [roleGuard(['ADMIN'])],
    loadChildren: () => import('./features/admin/admin.routes').then((m) => m.ADMIN_ROUTES),
  },
  {
    path: '**',
    redirectTo: '',
  },
];