import { Routes } from '@angular/router';
import { Login } from './pages/login/login';
import { ProductDetail } from './pages/product-detail/product-detail';
import { ProductList } from './pages/product-list/product-list';



export const routes: Routes = [
  {
    path: "",
    redirectTo: "login",
    pathMatch: "full"
  },
  {
    path: "login",
    component: Login
  },
  {
    path: "products",
    component: ProductList
  },
  {
    path: "products/:id",
    component: ProductDetail
  },
  {
    path: "**",
    redirectTo: "login"
  }
];
