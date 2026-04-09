export type OrderStatus =
  | 'pending'
  | 'shipped'
  | 'delivered'
  | 'cancelled'
  | 'return_requested'
  | 'returned';

export type ShipmentStatus =
  | 'pending'
  | 'processing'
  | 'shipped'
  | 'in_transit'
  | 'out_for_delivery'
  | 'delivered'
  | 'failed';

export type PaymentMethod = 'credit_card' | 'bank_transfer' | 'cash_on_delivery' | 'card';

export interface OrderItem {
  id: string;
  productId: string;
  quantity: number;
  price: number;
  productName?: string;
  imageUrl?: string;
}

export interface Order {
  id: string;
  userId: string;
  storeId?: string;
  status: OrderStatus | string;
  grandTotal: number;
  createdAt: string;
  paymentMethod: string;
  items: OrderItem[];
  shipmentStatus?: ShipmentStatus | string;
}

export interface OrderItemRequest {
  productId: string;
  quantity: number;
}

export interface CreateOrderRequest {
  storeId: string;
  paymentMethod: string;
  items: OrderItemRequest[];
}

export interface ShipmentHistoryEntry {
  timestamp: string;
  status: string;
  description?: string;
}

export interface Shipment {
  id: string;
  orderId: string;
  warehouse?: string;
  mode?: string;
  status: string;
  customerCareCalls?: number;
  customerRating?: number;
  weightGms?: number;
  history?: ShipmentHistoryEntry[];
}
