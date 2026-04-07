import { Routes } from '@angular/router';
import { ShellComponent } from './core/layout/shell.component';
import { authGuard } from './core/guards/auth.guard';
import { adminGuard } from './core/guards/admin.guard';
import { corporateGuard } from './core/guards/corporate.guard';
import { Home } from './features/home/home';

export const routes: Routes = [
  {
    path: '',
    component: ShellComponent,
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./features/home/home').then((m) => m.Home)
      },
      {
        path: 'login',
        loadComponent: () =>
          import('./features/auth/login/login').then((m) => m.Login)
      },
      {
        path: 'register',
        loadComponent: () =>
          import('./features/auth/register/register').then((m) => m.Register)
      },
      {
        path: 'products',
        loadComponent: () =>
          import('./features/products/product-list/product-list').then((m) => m.ProductList)
      },
      {
        path: 'products/:id',
        loadComponent: () =>
          import('./features/products/product-detail/product-detail').then((m) => m.ProductDetail)
      },
      {
        path: 'cart',
        canActivate: [authGuard],
        loadComponent: () =>
          import('./features/cart/cart').then((m) => m.Cart)
      },
      {
        path: 'wishlist',
        canActivate: [authGuard],
        loadComponent: () =>
          import('./features/wishlist/wishlist').then((m) => m.Wishlist)
      },
      {
        path: 'orders',
        canActivate: [authGuard],
        loadComponent: () =>
          import('./features/orders/order-list/order-list').then((m) => m.OrderList)
      },
      {
        path: 'orders/:id',
        canActivate: [authGuard],
        loadComponent: () =>
          import('./features/orders/order-detail/order-detail').then((m) => m.OrderDetail)
      },
      {
        path: 'profile',
        canActivate: [authGuard],
        loadComponent: () =>
          import('./features/profile/profile').then((m) => m.Profile)
      },
      {
        path: 'addresses',
        canActivate: [authGuard],
        loadComponent: () =>
          import('./features/addresses/addresses').then((m) => m.Addresses)
      },
      {
        path: 'chat',
        canActivate: [authGuard],
        loadComponent: () =>
          import('./features/chat/chat').then((m) => m.Chat)
      },
      {
        path: 'dashboard/me',
        canActivate: [authGuard],
        loadComponent: () =>
          import('./features/dashboards/individual-dashboard/individual-dashboard').then(
            (m) => m.IndividualDashboard
          )
      },
      {
        path: 'dashboard/corporate',
        canActivate: [authGuard, corporateGuard],
        loadComponent: () =>
          import('./features/dashboards/corporate-dashboard/corporate-dashboard').then(
            (m) => m.CorporateDashboard
          )
      },
      {
        path: 'dashboard/admin',
        canActivate: [authGuard, adminGuard],
        loadComponent: () =>
          import('./features/dashboards/admin-dashboard/admin-dashboard').then(
            (m) => m.AdminDashboard
          )
      },
      {
        path: 'corporate',
        canActivate: [authGuard, corporateGuard],
        loadComponent: () =>
          import('./features/corporate/store-management/store-management').then(
            (m) => m.StoreManagement
          )
      },
      {
        path: 'corporate/inventory',
        canActivate: [authGuard, corporateGuard],
        loadComponent: () =>
          import('./features/corporate/inventory/inventory').then((m) => m.Inventory)
      },
      {
        path: 'corporate/orders',
        canActivate: [authGuard, corporateGuard],
        loadComponent: () =>
          import('./features/corporate/order-management/order-management').then(
            (m) => m.OrderManagement
          )
      },
      {
        path: 'admin/users',
        canActivate: [authGuard, adminGuard],
        loadComponent: () =>
          import('./features/admin/users/users').then((m) => m.Users)
      },
      {
        path: 'admin/stores',
        canActivate: [authGuard, adminGuard],
        loadComponent: () =>
          import('./features/admin/stores/stores').then((m) => m.Stores)
      },
      {
        path: 'admin/categories',
        canActivate: [authGuard, adminGuard],
        loadComponent: () =>
          import('./features/admin/categories/categories').then((m) => m.Categories)
      }
    ]
  },
  {
    path: '**',
    redirectTo: ''
  }
];
