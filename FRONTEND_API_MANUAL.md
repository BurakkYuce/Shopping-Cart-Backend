# DataPulse Frontend API Integration Manual

Complete reference for integrating an Angular frontend with the DataPulse Spring Boot backend.

---

## Table of Contents

1. [Base URL & Environment Setup](#1-base-url--environment-setup)
2. [Authentication Flow](#2-authentication-flow)
3. [Error Handling](#3-error-handling)
4. [Role-Based Behavior](#4-role-based-behavior)
5. [API Endpoints](#5-api-endpoints)
   - [Auth](#51-auth)
   - [Products](#52-products)
   - [Cart](#53-cart)
   - [Orders](#54-orders)
   - [Wishlist](#55-wishlist)
   - [Addresses](#56-addresses)
   - [Categories](#57-categories)
   - [Stores](#58-stores)
   - [Reviews](#59-reviews)
   - [Shipments](#510-shipments)
   - [Users](#511-users)
   - [Customer Profiles](#512-customer-profiles)
   - [Analytics](#513-analytics)
   - [Chat](#514-chat)
6. [Chat Widget Integration](#6-chat-widget-integration)
7. [Angular Integration Guide](#7-angular-integration-guide)
   - [HttpClient Setup](#71-httpclient-setup)
   - [AuthInterceptor](#72-authinterceptor)
   - [AuthGuard](#73-authguard)
   - [Service Skeletons](#74-service-skeletons)

---

## 1. Base URL & Environment Setup

| Environment | Backend URL            |
|-------------|------------------------|
| Development | `http://localhost:8080` |
| Docker      | `http://localhost:8080` |

The backend serves all API endpoints under the `/api` prefix. Swagger UI is available at:

```
http://localhost:8080/swagger-ui.html
```

In your Angular `environment.ts`:

```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api'
};
```

---

## 2. Authentication Flow

The backend uses **JWT Bearer tokens**. Here is the complete flow:

### Step 1: Login or Register

Call `POST /api/auth/login` or `POST /api/auth/register`. Both return an `AuthResponse` containing `accessToken` and `refreshToken`.

### Step 2: Store Tokens

```typescript
localStorage.setItem('accessToken', response.accessToken);
localStorage.setItem('refreshToken', response.refreshToken);
```

### Step 3: Attach Token to Every Request

```
Authorization: Bearer <accessToken>
```

Use an Angular `HttpInterceptor` to do this automatically (see [Section 7.2](#72-authinterceptor)).

### Step 4: Refresh When Expired

The access token expires after **24 hours** (`86400` seconds). When you receive a `401 Unauthorized` response, call `POST /api/auth/refresh` with the refresh token to get a new access token. The refresh token is valid for **7 days**.

### Step 5: Password Reset (if forgotten)

Call `POST /api/auth/forgot-password` with the email. In production this sends an email; in development it returns a reset token directly. Then call `POST /api/auth/reset-password` with the token and new password.

### Token Payload (decoded JWT)

```json
{
  "sub": "user@example.com",
  "userId": "a1b2c3d4",
  "role": "INDIVIDUAL",
  "iat": 1712200000,
  "exp": 1712286400
}
```

---

## 3. Error Handling

All errors follow a consistent format:

```json
{
  "timestamp": "2026-04-04T17:00:00.000Z",
  "status": 404,
  "error": "Not Found",
  "message": "Product not found with id: xyz",
  "path": "/api/products/xyz"
}
```

Validation errors (400) include field-level details:

```json
{
  "timestamp": "2026-04-04T17:00:00.000Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/auth/register",
  "fieldErrors": {
    "email": "must not be blank",
    "roleType": "must not be null"
  }
}
```

### HTTP Status Code Reference

| Status | Meaning                    | When                                              |
|--------|----------------------------|----------------------------------------------------|
| 200    | OK                         | Successful GET, PUT, PATCH                         |
| 201    | Created                    | Successful POST (new resource)                     |
| 204    | No Content                 | Successful DELETE                                  |
| 400    | Bad Request                | Validation failure, insufficient stock             |
| 401    | Unauthorized               | Missing or expired JWT                             |
| 403    | Forbidden                  | Valid JWT but insufficient role                    |
| 404    | Not Found                  | Entity does not exist                              |
| 409    | Conflict                   | Duplicate email on registration                    |
| 500    | Internal Server Error      | Unexpected server error                            |

---

## 4. Role-Based Behavior

There are three roles: **ADMIN**, **CORPORATE**, **INDIVIDUAL**.

| Feature                  | ADMIN                      | CORPORATE                         | INDIVIDUAL                  |
|--------------------------|----------------------------|------------------------------------|-----------------------------|
| View products            | All products               | Only own stores' products          | All products                |
| Create/edit products     | Any store                  | Only own stores                    | Not allowed                 |
| View orders              | All orders                 | Own stores' orders                 | Own orders only             |
| Create orders            | Yes                        | Yes                                | Yes                         |
| Cancel/return orders     | Any order                  | Own stores' orders                 | Own orders only             |
| Shopping cart            | Yes                        | Yes                                | Yes                         |
| Wishlist                 | Yes                        | Yes                                | Yes                         |
| Manage addresses         | Own addresses              | Own addresses                      | Own addresses               |
| View analytics/sales     | All data                   | Own stores' data                   | Own data                    |
| View analytics/customers | Yes                        | Not allowed (403)                  | Not allowed (403)           |
| View analytics/products  | All                        | Own stores                         | All                         |
| Manage stores            | All stores                 | Own stores only                    | Not allowed                 |
| Delete reviews           | Any review                 | Not allowed                        | Own reviews only            |
| View all users           | Yes                        | Not allowed (403)                  | Not allowed (403)           |
| Chat                     | Full DB access             | Scoped to own stores               | Scoped to own data          |

---

## 5. API Endpoints

### 5.1 Auth

All auth endpoints are **public** (no token required).

---

#### POST `/api/auth/login`

Authenticate an existing user and receive JWT tokens.

**Headers:** `Content-Type: application/json`

**Request Body:**

```json
{
  "email": "user@example.com",
  "password": "securePass123"
}
```

| Field      | Type   | Required | Validation          |
|------------|--------|----------|---------------------|
| `email`    | string | Yes      | Valid email format   |
| `password` | string | Yes      | Not blank            |

**Success Response: `200 OK`**

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400,
  "userRole": "INDIVIDUAL",
  "userId": "a1b2c3d4"
}
```

**Error Responses:**

| Status | Cause                      |
|--------|----------------------------|
| 400    | Invalid email or blank fields |
| 404    | User not found              |

---

#### POST `/api/auth/register`

Register a new user account.

**Request Body:**

```json
{
  "email": "newuser@example.com",
  "password": "securePass123",
  "roleType": "INDIVIDUAL",
  "gender": "female"
}
```

| Field      | Type     | Required | Validation                          |
|------------|----------|----------|--------------------------------------|
| `email`    | string   | Yes      | Valid email, unique                   |
| `password` | string   | Yes      | Not blank                             |
| `roleType` | enum     | Yes      | `ADMIN`, `CORPORATE`, or `INDIVIDUAL` |
| `gender`   | string   | No       |                                       |

**Success Response: `201 Created`** -- Same shape as login response.

**Error Responses:**

| Status | Cause                    |
|--------|--------------------------|
| 400    | Validation failure        |
| 409    | Email already registered  |

---

#### POST `/api/auth/refresh`

Refresh an expired access token.

**Request Body:**

```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

**Success Response: `200 OK`** -- Same shape as login response (new `accessToken`, same `refreshToken`).

---

#### POST `/api/auth/forgot-password`

Request a password reset token.

**Request Body:**

```json
{
  "email": "user@example.com"
}
```

**Success Response: `200 OK`**

```json
{
  "message": "Password reset token generated. In production, this would be sent via email.",
  "resetToken": "550e8400-e29b-41d4-a716-446655440000"
}
```

---

#### POST `/api/auth/reset-password`

Reset password using the token from forgot-password.

**Request Body:**

```json
{
  "token": "550e8400-e29b-41d4-a716-446655440000",
  "newPassword": "myNewSecurePassword"
}
```

**Success Response: `200 OK`**

```json
{
  "message": "Password has been reset successfully"
}
```

**Error Responses:**

| Status | Cause                        |
|--------|------------------------------|
| 403    | Invalid or expired token      |
| 404    | User not found                |

---

### 5.2 Products

---

#### GET `/api/products`

List all products (paginated) with optional **search and filtering**. **Public** -- no token required.

**Query Parameters:**

| Param        | Type   | Default | Description                             |
|--------------|--------|---------|-----------------------------------------|
| `page`       | int    | 0       | Page number (0-based)                   |
| `size`       | int    | 50      | Items per page                          |
| `q`          | string | null    | Search by name or description (partial match, case-insensitive) |
| `categoryId` | string | null    | Filter by category ID                   |
| `minPrice`   | number | null    | Minimum price filter                    |
| `maxPrice`   | number | null    | Maximum price filter                    |

**Examples:**
- `/api/products?q=mouse` -- search for "mouse"
- `/api/products?minPrice=10&maxPrice=50` -- price range filter
- `/api/products?q=candle&categoryId=cat01&minPrice=1&maxPrice=5` -- combined

**Success Response: `200 OK`**

```json
{
  "content": [
    {
      "id": "p001",
      "storeId": "s001",
      "categoryId": "cat01",
      "sku": "SKU-001",
      "name": "Wireless Mouse",
      "unitPrice": 29.99,
      "description": "Ergonomic wireless mouse",
      "imageUrl": "https://example.com/images/mouse.jpg",
      "stockQuantity": 150,
      "brand": "Logitech",
      "rating": 4.5,
      "retailPrice": 39.99
    }
  ],
  "totalElements": 150,
  "totalPages": 3,
  "size": 50,
  "number": 0,
  "last": false
}
```

---

#### GET `/api/products/{id}`

Get a single product by ID. **Public**.

**Success Response: `200 OK`** -- Single product object (same shape as above).

**Error Responses:**

| Status | Cause            |
|--------|------------------|
| 404    | Product not found |

---

#### POST `/api/products`

Create a new product. **Requires auth: CORPORATE or ADMIN.**

**Request Body:**

```json
{
  "storeId": "s001",
  "categoryId": "cat01",
  "sku": "SKU-NEW",
  "name": "New Product",
  "unitPrice": 49.99,
  "description": "A brand new product",
  "imageUrl": "https://example.com/images/product.jpg",
  "stockQuantity": 100
}
```

| Field          | Type   | Required | Validation      |
|----------------|--------|----------|-----------------|
| `storeId`      | string | Yes      | Not blank        |
| `categoryId`   | string | No       |                  |
| `sku`          | string | No       |                  |
| `name`         | string | Yes      | Not blank        |
| `unitPrice`    | number | Yes      | Positive number  |
| `description`  | string | No       |                  |
| `imageUrl`     | string | No       | URL to image     |
| `stockQuantity`| int    | No       | Default: 9999    |

**Success Response: `201 Created`** -- Returns the created product.

---

#### PUT `/api/products/{id}`

Update an existing product. **Requires auth: CORPORATE (own store) or ADMIN.**

Same request body as POST. Only non-null fields are updated.

**Success Response: `200 OK`** -- Returns updated product.

---

#### DELETE `/api/products/{id}`

Delete a product. **Requires auth: CORPORATE (own store) or ADMIN.**

**Success Response: `204 No Content`**

---

### 5.3 Cart

All cart endpoints require authentication. Each user has one cart. Cart items are unique per product (adding a product that already exists increases its quantity).

---

#### GET `/api/cart`

Get the current user's shopping cart.

**Headers:** `Authorization: Bearer <token>`

**Success Response: `200 OK`**

```json
{
  "items": [
    {
      "id": "ci001",
      "productId": "p001",
      "productName": "Wireless Mouse",
      "unitPrice": 29.99,
      "quantity": 2,
      "lineTotal": 59.98
    }
  ],
  "totalItems": 2,
  "totalPrice": 59.98
}
```

---

#### POST `/api/cart/items`

Add a product to the cart. If already in cart, quantity is increased.

**Request Body:**

```json
{
  "productId": "p001",
  "quantity": 2
}
```

| Field       | Type   | Required | Validation |
|-------------|--------|----------|------------|
| `productId` | string | Yes      | Not blank   |
| `quantity`  | int    | Yes      | Min 1       |

**Success Response: `200 OK`** -- Returns the updated cart.

---

#### PATCH `/api/cart/items/{productId}`

Update the quantity of a cart item. Set to 0 to remove.

**Request Body:**

```json
{
  "quantity": 5
}
```

**Success Response: `200 OK`** -- Returns the updated cart.

---

#### DELETE `/api/cart/items/{productId}`

Remove a specific product from the cart.

**Success Response: `204 No Content`**

---

#### DELETE `/api/cart`

Clear the entire cart.

**Success Response: `204 No Content`**

---

#### POST `/api/cart/checkout`

Convert the cart into an order. Cart is cleared after successful checkout.

**Request Body:**

```json
{
  "storeId": "s001",
  "paymentMethod": "card"
}
```

**Success Response: `201 Created`** -- Returns the created `OrderResponse`.

**Error Responses:**

| Status | Cause                       |
|--------|-----------------------------|
| 400    | Insufficient stock           |
| 404    | Cart is empty or product not found |

---

### 5.4 Orders

All order endpoints require authentication.

---

#### GET `/api/orders`

List orders (paginated, role-filtered).

**Query Parameters:**

| Param  | Type | Default | Description           |
|--------|------|---------|-----------------------|
| `page` | int  | 0       | Page number (0-based) |
| `size` | int  | 50      | Items per page        |

**Role behavior:**
- **INDIVIDUAL** -- own orders only
- **CORPORATE** -- orders from own stores
- **ADMIN** -- all orders

**Success Response: `200 OK`**

```json
{
  "content": [
    {
      "id": "ord001",
      "userId": "u001",
      "storeId": "s001",
      "status": "pending",
      "grandTotal": 59.98,
      "createdAt": "2026-03-15T14:30:00",
      "paymentMethod": "card",
      "items": [
        {
          "id": "oi001",
          "productId": "p001",
          "quantity": 2,
          "price": 59.98
        }
      ],
      "shipmentStatus": "shipped"
    }
  ],
  "totalElements": 25,
  "totalPages": 1,
  "size": 50,
  "number": 0,
  "last": true
}
```

**Order status flow:**

```
pending -> shipped -> delivered
pending -> cancelled (via cancel endpoint)
shipped/delivered -> return_requested (via return endpoint)
```

---

#### GET `/api/orders/{id}`

Get a single order with items and shipment status.

---

#### POST `/api/orders`

Create a new order directly (without cart). Stock is deducted automatically.

**Request Body:**

```json
{
  "storeId": "s001",
  "paymentMethod": "card",
  "items": [
    { "productId": "p001", "quantity": 2 },
    { "productId": "p002", "quantity": 1 }
  ]
}
```

**Error Responses:**

| Status | Cause                      |
|--------|----------------------------|
| 400    | Validation failure, insufficient stock |
| 404    | Product not found           |

---

#### PATCH `/api/orders/{id}/status`

Update an order's status (ADMIN or CORPORATE store owner).

**Request Body:**

```json
{
  "status": "shipped"
}
```

---

#### POST `/api/orders/{id}/cancel`

Cancel a pending order. Stock is restored automatically.

**Requires:** Order must have status `pending`.

**Success Response: `200 OK`** -- Returns order with `status: "cancelled"`.

**Error Responses:**

| Status | Cause                              |
|--------|------------------------------------|
| 400    | Order is not in pending status      |
| 403    | Not your order                      |

---

#### POST `/api/orders/{id}/return`

Request a return for a shipped or delivered order.

**Requires:** Order must have status `shipped` or `delivered`.

**Success Response: `200 OK`** -- Returns order with `status: "return_requested"`.

---

### 5.5 Wishlist

All endpoints require authentication. Each user can have multiple products in their wishlist.

---

#### GET `/api/wishlist`

Get the current user's wishlist.

**Success Response: `200 OK`** -- Array of `ProductResponse` objects.

```json
[
  {
    "id": "p001",
    "name": "Wireless Mouse",
    "unitPrice": 29.99,
    "imageUrl": "https://example.com/mouse.jpg",
    "stockQuantity": 150
  }
]
```

---

#### POST `/api/wishlist/{productId}`

Add a product to the wishlist. Duplicate adds are ignored.

**Success Response: `201 Created`** -- Returns the added product.

---

#### DELETE `/api/wishlist/{productId}`

Remove a product from the wishlist.

**Success Response: `204 No Content`**

---

### 5.6 Addresses

All endpoints require authentication. Users manage their own addresses.

---

#### GET `/api/addresses`

List the current user's addresses.

**Success Response: `200 OK`**

```json
[
  {
    "id": "addr001",
    "title": "Home",
    "fullName": "Burak Yilmaz",
    "phone": "5551234567",
    "addressLine1": "Ataturk Cad. No:1",
    "addressLine2": null,
    "city": "Istanbul",
    "district": "Kadikoy",
    "zipCode": "34000",
    "country": "Turkey",
    "isDefault": true
  }
]
```

---

#### POST `/api/addresses`

Create a new address. If `isDefault: true`, the previous default is cleared.

**Request Body:**

```json
{
  "title": "Home",
  "fullName": "Burak Yilmaz",
  "phone": "5551234567",
  "addressLine1": "Ataturk Cad. No:1",
  "addressLine2": "Kat 3",
  "city": "Istanbul",
  "district": "Kadikoy",
  "zipCode": "34000",
  "country": "Turkey",
  "isDefault": true
}
```

| Field          | Type    | Required | Default    |
|----------------|---------|----------|------------|
| `title`        | string  | Yes      |            |
| `fullName`     | string  | Yes      |            |
| `phone`        | string  | No       |            |
| `addressLine1` | string  | Yes      |            |
| `addressLine2` | string  | No       |            |
| `city`         | string  | Yes      |            |
| `district`     | string  | No       |            |
| `zipCode`      | string  | No       |            |
| `country`      | string  | No       | `"Turkey"` |
| `isDefault`    | boolean | No       | `false`    |

**Success Response: `201 Created`** -- Returns the created address.

---

#### PUT `/api/addresses/{id}`

Update an address. Only non-null fields are changed.

**Success Response: `200 OK`** -- Returns updated address.

---

#### DELETE `/api/addresses/{id}`

Delete an address.

**Success Response: `204 No Content`**

---

### 5.7 Categories

GET endpoints are **public**. POST/PUT/DELETE require authentication.

---

#### GET `/api/categories`

List all categories.

**Success Response: `200 OK`**

```json
[
  { "id": "cat01", "name": "Electronics", "parentId": null },
  { "id": "cat02", "name": "Smartphones", "parentId": "cat01" }
]
```

---

#### GET `/api/categories/{id}`

Get a single category.

---

#### GET `/api/categories/{id}/children`

Get child categories of a parent.

---

#### POST `/api/categories`

Create a category. **Requires auth.**

**Request Body:**

```json
{ "id": "cat03", "name": "Laptops", "parentId": "cat01" }
```

**Success Response: `201 Created`**

---

#### PUT `/api/categories/{id}` / DELETE `/api/categories/{id}`

Update or delete a category. **Requires auth.**

---

### 5.8 Stores

All endpoints require authentication.

---

#### GET `/api/stores`

List stores. **CORPORATE** sees only own stores. **ADMIN** sees all.

**Success Response: `200 OK`**

```json
[
  { "id": "s001", "ownerId": "u001", "name": "Tech Store", "status": "active" }
]
```

---

#### GET `/api/stores/{id}` | POST `/api/stores` | PUT `/api/stores/{id}` | DELETE `/api/stores/{id}`

Standard CRUD. POST body:

```json
{ "name": "My New Store", "status": "active" }
```

---

### 5.9 Reviews

---

#### GET `/api/reviews/by-product/{productId}`

Get all reviews for a product. **Public.**

**Success Response: `200 OK`**

```json
[
  {
    "id": "r001",
    "userId": "u001",
    "productId": "p001",
    "starRating": 5,
    "helpfulVotes": 12,
    "totalVotes": 15,
    "reviewHeadline": "Great product!",
    "reviewText": "Works perfectly, highly recommend.",
    "sentiment": "positive",
    "verifiedPurchase": "Y",
    "reviewDate": "2026-03-10"
  }
]
```

---

#### POST `/api/reviews`

Create a review. **Requires auth.**

**Request Body:**

```json
{
  "productId": "p001",
  "starRating": 4,
  "reviewHeadline": "Good quality",
  "reviewText": "Solid product for the price."
}
```

| Field            | Type   | Required | Validation |
|------------------|--------|----------|------------|
| `productId`      | string | Yes      | Not blank   |
| `starRating`     | int    | Yes      | 1 to 5      |
| `reviewHeadline` | string | No       |             |
| `reviewText`     | string | No       |             |

**Success Response: `201 Created`**

---

#### DELETE `/api/reviews/{id}`

Delete a review. **INDIVIDUAL** can delete own reviews. **ADMIN** can delete any.

**Success Response: `204 No Content`**

---

### 5.10 Shipments

All endpoints require authentication. See original sections for full CRUD details.

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/shipments/{id}` | Get shipment by ID |
| GET | `/api/shipments/by-order/{orderId}` | Get shipment for an order |
| POST | `/api/shipments` | Create shipment |
| PUT | `/api/shipments/{id}` | Update shipment |
| DELETE | `/api/shipments/{id}` | Delete (ADMIN only) |

---

### 5.11 Users

All endpoints require authentication.

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/users/me` | Get current user profile |
| GET | `/api/users/{id}` | Get user by ID (ADMIN or self) |
| PUT | `/api/users/{id}` | Update user (ADMIN or self) |
| GET | `/api/users` | List all users (ADMIN only) |

**UserResponse:**

```json
{
  "id": "u001",
  "email": "user@example.com",
  "roleType": "INDIVIDUAL",
  "gender": "female"
}
```

---

### 5.12 Customer Profiles

All endpoints require authentication.

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/customer-profiles/me` | Get own profile |
| PUT | `/api/customer-profiles/me` | Update own profile (partial) |
| GET | `/api/customer-profiles/{id}` | Get by ID (ADMIN or self) |

**CustomerProfileResponse:**

```json
{
  "id": "cp001",
  "userId": "u001",
  "age": 28,
  "city": "Istanbul",
  "membershipType": "Gold",
  "totalSpend": 1250.50,
  "itemsPurchased": 15,
  "averageRating": 4.2,
  "satisfactionLevel": "High"
}
```

---

### 5.13 Analytics

All endpoints require authentication.

---

#### GET `/api/analytics/sales`

Sales analytics with optional date range.

**Query Parameters:**

| Param | Type          | Required | Format                |
|-------|---------------|----------|-----------------------|
| `from`| LocalDateTime | No       | `2026-01-01T00:00:00` |
| `to`  | LocalDateTime | No       | `2026-03-31T23:59:59` |

**Success Response: `200 OK`**

```json
{
  "totalRevenue": 15230.50,
  "orderCount": 142,
  "averageOrderValue": 107.25,
  "revenueByDay": {
    "2026-03-01": 520.00,
    "2026-03-02": 890.50
  },
  "fromDate": "2026-01-01",
  "toDate": "2026-03-31"
}
```

---

#### GET `/api/analytics/customers`

Customer demographics and satisfaction. **ADMIN only.**

```json
{
  "averageAge": 34.5,
  "spendByMembership": { "Gold": 25000.00, "Silver": 12000.00 },
  "satisfactionDistribution": { "High": 450, "Medium": 320 },
  "topCities": { "Istanbul": 250, "Ankara": 120 }
}
```

---

#### GET `/api/analytics/products`

Product performance. Optional `storeId` query param.

```json
{
  "topSellingProducts": [
    { "productId": "p001", "productName": "Wireless Mouse", "totalQuantity": 523, "totalRevenue": 15667.77 }
  ],
  "avgRatingByCategory": { "Electronics": 4.2, "Clothing": 3.8 }
}
```

---

### 5.14 Chat

---

#### POST `/api/chat/ask`

Send a natural-language question to the AI chatbot.

**Request Body:**

```json
{
  "message": "What were the top 5 selling products last month?",
  "sessionId": "optional-session-id"
}
```

**Success Response: `200 OK`**

```json
{
  "message": "The top 5 selling products last month were...",
  "sessionId": "abc123",
  "status": "completed",
  "plotlyJson": "{\"data\": [{\"type\": \"bar\", ...}], \"layout\": {...}}"
}
```

| Field        | Type        | Description                                      |
|--------------|-------------|--------------------------------------------------|
| `message`    | string      | Human-readable analysis                          |
| `sessionId`  | string      | Reuse for follow-up questions                    |
| `status`     | string      | `"completed"` or `"error"`                       |
| `plotlyJson` | string/null | Plotly chart JSON, or `null` if no visualization |

**Role behavior:** The chatbot automatically scopes SQL queries by role (ADMIN=all, CORPORATE=own stores, INDIVIDUAL=own data).

---

## 6. Chat Widget Integration

### Sending a message

```typescript
this.http.post<ChatResponse>(`${apiUrl}/chat/ask`, {
  message: userInput,
  sessionId: this.currentSessionId
}).subscribe(response => {
  this.currentSessionId = response.sessionId;
  this.displayMessage(response.message);
  if (response.plotlyJson) {
    this.renderChart(response.plotlyJson);
  }
});
```

### Rendering Plotly charts

```bash
npm install plotly.js-dist-min
```

```typescript
import Plotly from 'plotly.js-dist-min';

renderChart(plotlyJson: string) {
  const figure = JSON.parse(plotlyJson);
  Plotly.newPlot('chart-container', figure.data, figure.layout, { responsive: true });
}
```

### Intent types

- **sql_query** -- data question -> SQL execution + analysis + optional chart
- **greeting** -- "Hello" -> friendly greeting
- **clarify** -- ambiguous -> asks for clarification
- **off_topic** -- unrelated -> polite redirect

---

## 7. Angular Integration Guide

### 7.1 HttpClient Setup

```typescript
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { authInterceptor } from './core/interceptors/auth.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    provideHttpClient(withInterceptors([authInterceptor]))
  ]
};
```

### 7.2 AuthInterceptor

```typescript
import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  const token = localStorage.getItem('accessToken');

  if (token) {
    req = req.clone({
      setHeaders: { Authorization: `Bearer ${token}` }
    });
  }

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401) {
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        router.navigate(['/login']);
      }
      return throwError(() => error);
    })
  );
};
```

### 7.3 AuthGuard

```typescript
import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

export const authGuard: CanActivateFn = () => {
  const router = inject(Router);
  const token = localStorage.getItem('accessToken');
  if (token) return true;
  router.navigate(['/login']);
  return false;
};
```

### 7.4 Service Skeletons

#### AuthService

```typescript
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  userRole: string;
  userId: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private url = `${environment.apiUrl}/auth`;

  constructor(private http: HttpClient) {}

  login(email: string, password: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.url}/login`, { email, password })
      .pipe(tap(res => this.storeTokens(res)));
  }

  register(email: string, password: string, roleType: string, gender?: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.url}/register`, { email, password, roleType, gender })
      .pipe(tap(res => this.storeTokens(res)));
  }

  refresh(): Observable<AuthResponse> {
    const refreshToken = localStorage.getItem('refreshToken');
    return this.http.post<AuthResponse>(`${this.url}/refresh`, { refreshToken })
      .pipe(tap(res => this.storeTokens(res)));
  }

  forgotPassword(email: string): Observable<any> {
    return this.http.post(`${this.url}/forgot-password`, { email });
  }

  resetPassword(token: string, newPassword: string): Observable<any> {
    return this.http.post(`${this.url}/reset-password`, { token, newPassword });
  }

  logout(): void {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('userRole');
    localStorage.removeItem('userId');
  }

  isLoggedIn(): boolean {
    return !!localStorage.getItem('accessToken');
  }

  getUserRole(): string | null {
    return localStorage.getItem('userRole');
  }

  private storeTokens(res: AuthResponse): void {
    localStorage.setItem('accessToken', res.accessToken);
    localStorage.setItem('refreshToken', res.refreshToken);
    localStorage.setItem('userRole', res.userRole);
    localStorage.setItem('userId', res.userId);
  }
}
```

#### ProductService

```typescript
import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface Product {
  id: string;
  storeId: string;
  categoryId: string;
  sku: string;
  name: string;
  unitPrice: number;
  description: string;
  imageUrl: string | null;
  stockQuantity: number;
  brand: string | null;
  rating: number | null;
  retailPrice: number | null;
}

export interface PagedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  last: boolean;
}

@Injectable({ providedIn: 'root' })
export class ProductService {
  private url = `${environment.apiUrl}/products`;

  constructor(private http: HttpClient) {}

  getProducts(page = 0, size = 50): Observable<PagedResponse<Product>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<PagedResponse<Product>>(this.url, { params });
  }

  search(query?: string, categoryId?: string, minPrice?: number, maxPrice?: number, page = 0, size = 50): Observable<PagedResponse<Product>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (query) params = params.set('q', query);
    if (categoryId) params = params.set('categoryId', categoryId);
    if (minPrice !== undefined) params = params.set('minPrice', minPrice);
    if (maxPrice !== undefined) params = params.set('maxPrice', maxPrice);
    return this.http.get<PagedResponse<Product>>(this.url, { params });
  }

  getProductById(id: string): Observable<Product> {
    return this.http.get<Product>(`${this.url}/${id}`);
  }

  createProduct(product: Partial<Product>): Observable<Product> {
    return this.http.post<Product>(this.url, product);
  }

  updateProduct(id: string, product: Partial<Product>): Observable<Product> {
    return this.http.put<Product>(`${this.url}/${id}`, product);
  }

  deleteProduct(id: string): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }
}
```

#### CartService

```typescript
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface CartItemResponse {
  id: string;
  productId: string;
  productName: string;
  unitPrice: number;
  quantity: number;
  lineTotal: number;
}

export interface CartResponse {
  items: CartItemResponse[];
  totalItems: number;
  totalPrice: number;
}

@Injectable({ providedIn: 'root' })
export class CartService {
  private url = `${environment.apiUrl}/cart`;

  constructor(private http: HttpClient) {}

  getCart(): Observable<CartResponse> {
    return this.http.get<CartResponse>(this.url);
  }

  addToCart(productId: string, quantity = 1): Observable<CartResponse> {
    return this.http.post<CartResponse>(`${this.url}/items`, { productId, quantity });
  }

  updateQuantity(productId: string, quantity: number): Observable<CartResponse> {
    return this.http.patch<CartResponse>(`${this.url}/items/${productId}`, { quantity });
  }

  removeFromCart(productId: string): Observable<void> {
    return this.http.delete<void>(`${this.url}/items/${productId}`);
  }

  clearCart(): Observable<void> {
    return this.http.delete<void>(this.url);
  }

  checkout(storeId: string, paymentMethod = 'card'): Observable<any> {
    return this.http.post(`${this.url}/checkout`, { storeId, paymentMethod });
  }
}
```

#### WishlistService

```typescript
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Product } from './product.service';

@Injectable({ providedIn: 'root' })
export class WishlistService {
  private url = `${environment.apiUrl}/wishlist`;

  constructor(private http: HttpClient) {}

  getWishlist(): Observable<Product[]> {
    return this.http.get<Product[]>(this.url);
  }

  add(productId: string): Observable<Product> {
    return this.http.post<Product>(`${this.url}/${productId}`, {});
  }

  remove(productId: string): Observable<void> {
    return this.http.delete<void>(`${this.url}/${productId}`);
  }
}
```

#### AddressService

```typescript
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface Address {
  id: string;
  title: string;
  fullName: string;
  phone: string;
  addressLine1: string;
  addressLine2: string | null;
  city: string;
  district: string | null;
  zipCode: string | null;
  country: string;
  isDefault: boolean;
}

@Injectable({ providedIn: 'root' })
export class AddressService {
  private url = `${environment.apiUrl}/addresses`;

  constructor(private http: HttpClient) {}

  getAddresses(): Observable<Address[]> {
    return this.http.get<Address[]>(this.url);
  }

  create(address: Partial<Address>): Observable<Address> {
    return this.http.post<Address>(this.url, address);
  }

  update(id: string, address: Partial<Address>): Observable<Address> {
    return this.http.put<Address>(`${this.url}/${id}`, address);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }
}
```

#### ChatService

```typescript
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface ChatResponse {
  message: string;
  sessionId: string;
  status: string;
  plotlyJson: string | null;
}

@Injectable({ providedIn: 'root' })
export class ChatService {
  private url = `${environment.apiUrl}/chat`;
  private sessionId: string | null = null;

  constructor(private http: HttpClient) {}

  ask(message: string): Observable<ChatResponse> {
    return this.http.post<ChatResponse>(`${this.url}/ask`, {
      message,
      sessionId: this.sessionId
    });
  }

  updateSessionId(sessionId: string): void {
    this.sessionId = sessionId;
  }

  clearSession(): void {
    this.sessionId = null;
  }
}
```

#### OrderService

```typescript
import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PagedResponse } from './product.service';

export interface OrderItem {
  id: string;
  productId: string;
  quantity: number;
  price: number;
}

export interface Order {
  id: string;
  userId: string;
  storeId: string;
  status: string;
  grandTotal: number;
  createdAt: string;
  paymentMethod: string;
  items: OrderItem[];
  shipmentStatus: string | null;
}

@Injectable({ providedIn: 'root' })
export class OrderService {
  private url = `${environment.apiUrl}/orders`;

  constructor(private http: HttpClient) {}

  getOrders(page = 0, size = 50): Observable<PagedResponse<Order>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<PagedResponse<Order>>(this.url, { params });
  }

  getOrderById(id: string): Observable<Order> {
    return this.http.get<Order>(`${this.url}/${id}`);
  }

  cancel(id: string): Observable<Order> {
    return this.http.post<Order>(`${this.url}/${id}/cancel`, {});
  }

  requestReturn(id: string): Observable<Order> {
    return this.http.post<Order>(`${this.url}/${id}/return`, {});
  }
}
```
