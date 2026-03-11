import { Injectable } from '@angular/core';
import { Product as ProductModel } from '../../models/product';

@Injectable({
  providedIn: 'root',
})
export class Product {
  private products: ProductModel[] = [
  {
    id: 1,
    name: 'iPhone 15',
    price: 52000,
    description: 'A powerful smartphone with an advanced camera system and fast performance.',
    category: 'Electronics',
    rating: 4.7
  },
  {
    id: 2,
    name: 'MacBook Air M3',
    price: 68000,
    description: 'A lightweight laptop designed for students and professionals.',
    category: 'Computers',
    rating: 4.8
  },
  {
    id: 3,
    name: 'Sony WH-1000XM5',
    price: 14500,
    description: 'Premium wireless headphones with industry-leading noise cancellation.',
    category: 'Audio',
    rating: 4.6
  },
  {
    id: 4,
    name: 'Logitech MX Master 3S',
    price: 4200,
    description: 'Ergonomic wireless mouse designed for productivity and precision.',
    category: 'Accessories',
    rating: 4.5
  },
  {
    id: 5,
    name: 'Samsung Galaxy Tab S9',
    price: 31000,
    description: 'High performance Android tablet with a bright AMOLED display.',
    category: 'Tablets',
    rating: 4.6
  },
  {
    id: 6,
    name: 'Apple Watch Series 9',
    price: 21000,
    description: 'Smartwatch with health tracking features and seamless Apple integration.',
    category: 'Wearables',
    rating: 4.7
  },
  {
    id: 7,
    name: 'Dell XPS 13',
    price: 72000,
    description: 'Premium ultrabook with a compact design and powerful performance.',
    category: 'Computers',
    rating: 4.6
  },
  {
    id: 8,
    name: 'Google Pixel 8',
    price: 46000,
    description: 'Android smartphone known for its excellent camera and AI features.',
    category: 'Electronics',
    rating: 4.5
  },
  {
    id: 9,
    name: 'Bose QuietComfort Ultra',
    price: 16500,
    description: 'Comfortable noise cancelling headphones with immersive sound.',
    category: 'Audio',
    rating: 4.6
  },
  {
    id: 10,
    name: 'Asus ROG Zephyrus G14',
    price: 89000,
    description: 'High performance gaming laptop with powerful graphics and cooling system.',
    category: 'Computers',
    rating: 4.7
  }
];

  getProducts(): ProductModel[] {
    return this.products;
  }

  getProductById(id: number): ProductModel | undefined {
    return this.products.find(product => product.id === id);
  }
}
