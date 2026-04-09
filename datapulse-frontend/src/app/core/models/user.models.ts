import { UserRole } from './auth.models';

export interface User {
  id: string;
  email: string;
  roleType: UserRole;
  gender?: string;
}

export interface CustomerProfile {
  id: string;
  userId: string;
  age?: number;
  city?: string;
  membershipType?: string;
  totalSpend?: number;
  itemsPurchased?: number;
  averageRating?: number;
  satisfactionLevel?: string;
}

export interface UpdateProfileRequest {
  age?: number;
  city?: string;
  membershipType?: string;
  gender?: string;
}

export interface Address {
  id: string;
  title: string;
  fullName: string;
  phone?: string;
  addressLine1: string;
  addressLine2?: string;
  city: string;
  district?: string;
  zipCode?: string;
  country: string;
  isDefault: boolean;
}

export interface CreateAddressRequest {
  title: string;
  fullName: string;
  phone?: string;
  addressLine1: string;
  addressLine2?: string;
  city: string;
  district?: string;
  zipCode?: string;
  country?: string;
  isDefault?: boolean;
}

export interface WishlistItem {
  id: string;
  productId: string;
  productName?: string;
  unitPrice?: number;
  imageUrl?: string;
  addedAt?: string;
  inStock?: boolean;
}
