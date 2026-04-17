export interface CouponValidation {
  valid: boolean;
  code: string;
  type: 'PERCENTAGE' | 'FIXED_AMOUNT';
  discountAmount: number;
  description: string;
  message: string;
}

export interface Coupon {
  id: string;
  code: string;
  type: 'PERCENTAGE' | 'FIXED_AMOUNT';
  value: number;
  description: string;
  minOrderAmount: number;
  maxDiscount: number;
  maxUses: number;
  currentUses: number;
  active: boolean;
  validFrom: string;
  validTo: string;
  createdAt: string;
}

export interface CreateCouponRequest {
  code: string;
  type: 'PERCENTAGE' | 'FIXED_AMOUNT';
  value: number;
  description?: string;
  minOrderAmount?: number;
  maxDiscount?: number;
  maxUses?: number;
  validFrom?: string;
  validTo?: string;
}
