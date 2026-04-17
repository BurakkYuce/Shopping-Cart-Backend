export interface Product {
  id: string;
  storeId: string;
  categoryId: string;
  sku: string;
  name: string;
  unitPrice: number;
  description?: string;
  imageUrl?: string;
  stockQuantity: number;
  brand?: string;
  rating?: number;
  reviewCount?: number;
  retailPrice?: number;
}

export interface CreateProductRequest {
  storeId: string;
  categoryId?: string;
  sku?: string;
  name: string;
  unitPrice: number;
  description?: string;
  imageUrl?: string;
  stockQuantity?: number;
}

export interface UpdateProductRequest extends Partial<CreateProductRequest> {}

export interface ProductFilters {
  q?: string;
  categoryId?: string;
  storeId?: string;
  brand?: string;
  minPrice?: number;
  maxPrice?: number;
  minRating?: number;
}

export interface Category {
  id: string;
  name: string;
  parentId?: string;
  description?: string;
  imageUrl?: string;
  children?: Category[];
}

export interface Store {
  id: string;
  ownerId: string;
  name: string;
  status: string;
  // Optional UI-only fields
  description?: string;
  logoUrl?: string;
}

export interface Review {
  id: string;
  productId: string;
  userId: string;
  starRating: number;
  helpfulVotes?: number;
  totalVotes?: number;
  reviewHeadline?: string;
  reviewText?: string;
  sentiment?: string;
  verifiedPurchase?: string;
  reviewDate?: string;
  sellerResponse?: string;
  sellerResponseDate?: string;
}

export interface CreateReviewRequest {
  productId: string;
  starRating: number;
  reviewHeadline?: string;
  reviewText?: string;
}
