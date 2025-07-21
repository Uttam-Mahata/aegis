# UCO Bank Backend API Documentation

## Overview

The UCO Bank Backend is a demonstration banking API that showcases the integration with Aegis Security Environment for request validation and signature verification. All API requests (except health endpoints) must be signed using the Aegis SFE Client SDK.

## Base URL

```
http://localhost:8081/api/v1
```

## Authentication

All API requests must include the following security headers:

- `X-Signature`: HMAC-SHA256 signature of the request
- `X-Device-Id`: Unique device identifier from Aegis provisioning
- `X-Timestamp`: Request timestamp in milliseconds
- `X-Nonce`: Unique request nonce to prevent replay attacks

## API Endpoints

### Health Check

#### GET /health
Check the health status of the service.

**Response:**
```json
{
  "status": "UP",
  "service": "UCO Bank Backend",
  "timestamp": "2025-01-21T10:30:00",
  "version": "1.0.0"
}
```

### Account Management

#### GET /accounts/{accountNumber}
Retrieve account details by account number.

**Path Parameters:**
- `accountNumber` - 12-digit account number

**Response:**
```json
{
  "accountNumber": "123456789012",
  "accountHolderName": "Anurag Sharma",
  "balance": 50000.00,
  "currency": "INR",
  "accountType": "SAVINGS",
  "status": "ACTIVE",
  "createdAt": "2025-01-21T10:00:00"
}
```

#### GET /accounts/user/{userId}
Get all accounts for a specific user.

**Path Parameters:**
- `userId` - User identifier

**Response:**
```json
[
  {
    "accountNumber": "123456789012",
    "accountHolderName": "Anurag Sharma",
    "balance": 50000.00,
    "currency": "INR",
    "accountType": "SAVINGS",
    "status": "ACTIVE",
    "createdAt": "2025-01-21T10:00:00"
  }
]
```

#### GET /accounts/{accountNumber}/validate
Validate if an account exists and is active.

**Response:**
```json
true
```

### Transaction Management

#### POST /transactions/transfer
Process a money transfer between accounts.

**Request Body:**
```json
{
  "fromAccount": "123456789012",
  "toAccount": "987654321098",
  "amount": 1000.00,
  "currency": "INR",
  "description": "Payment to friend",
  "remarks": "Lunch money"
}
```

**Response:**
```json
{
  "transactionReference": "TXN1737460123456ABCD1234",
  "status": "COMPLETED",
  "amount": 1000.00,
  "currency": "INR",
  "fromAccount": "123456789012",
  "toAccount": "987654321098",
  "timestamp": "2025-01-21T10:35:00",
  "message": "Transfer completed successfully"
}
```

#### GET /transactions/account/{accountNumber}
Get transaction history for an account.

**Query Parameters:**
- `page` - Page number (default: 0)
- `size` - Page size (default: 20)
- `sort` - Sort field and direction (default: createdAt,desc)

**Response:**
```json
{
  "content": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "transactionReference": "TXN1737460123456ABCD1234",
      "fromAccountId": "550e8400-e29b-41d4-a716-446655440001",
      "toAccountId": "550e8400-e29b-41d4-a716-446655440002",
      "amount": 1000.00,
      "currency": "INR",
      "transactionType": "TRANSFER",
      "status": "COMPLETED",
      "description": "Payment to friend",
      "createdAt": "2025-01-21T10:35:00",
      "completedAt": "2025-01-21T10:35:01"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20
  },
  "totalElements": 1,
  "totalPages": 1
}
```

#### GET /transactions/reference/{transactionReference}
Get transaction details by reference number.

**Response:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "transactionReference": "TXN1737460123456ABCD1234",
  "fromAccountId": "550e8400-e29b-41d4-a716-446655440001",
  "toAccountId": "550e8400-e29b-41d4-a716-446655440002",
  "amount": 1000.00,
  "currency": "INR",
  "transactionType": "TRANSFER",
  "status": "COMPLETED",
  "description": "Payment to friend",
  "createdAt": "2025-01-21T10:35:00",
  "completedAt": "2025-01-21T10:35:01",
  "deviceId": "DEV123456",
  "signature": "base64signature",
  "nonce": "unique-nonce-123"
}
```

## Error Responses

### 400 Bad Request
```json
{
  "error": "VALIDATION_ERROR",
  "message": "Validation failed",
  "path": "/api/v1/transactions/transfer",
  "status": 400,
  "errors": {
    "amount": "Amount must be positive"
  }
}
```

### 401 Unauthorized
```json
{
  "error": "UNAUTHORIZED",
  "message": "Invalid request signature",
  "path": "/api/v1/accounts/123456789012",
  "status": 401,
  "timestamp": "2025-01-21T10:40:00"
}
```

### 404 Not Found
```json
{
  "error": "ACCOUNT_NOT_FOUND",
  "message": "Account not found: 123456789012",
  "path": "/api/v1/accounts/123456789012",
  "status": 404,
  "timestamp": "2025-01-21T10:40:00"
}
```

### 500 Internal Server Error
```json
{
  "error": "INTERNAL_SERVER_ERROR",
  "message": "An unexpected error occurred. Please try again later.",
  "path": "/api/v1/transactions/transfer",
  "status": 500,
  "timestamp": "2025-01-21T10:40:00"
}
```

## Sample Accounts (Pre-initialized)

| Account Number | Account Holder | Balance | Type    | User ID |
|---------------|----------------|---------|---------|---------|
| 123456789012  | Anurag Sharma  | 50,000  | SAVINGS | USER001 |
| 123456789013  | Anurag Sharma  | 100,000 | CURRENT | USER001 |
| 987654321098  | Priya Patel    | 75,000  | SAVINGS | USER002 |
| 987654321099  | Rajesh Kumar   | 25,000  | SAVINGS | USER003 |

## Integration with Aegis Security

1. **Device Provisioning**: Before making API calls, the client device must be provisioned with Aegis Security API
2. **Request Signing**: All requests must be signed using HMAC-SHA256 with the device's secret key
3. **Signature Validation**: The backend validates all signatures through Aegis Security API before processing requests

## Running the Backend

1. Ensure PostgreSQL and Redis are running
2. Update `application.properties` with correct database credentials
3. Run: `./gradlew bootRun`
4. The service will start on port 8081

## Testing the Integration

1. Start the Aegis Security API (port 8080)
2. Start the UCO Bank Backend (port 8081)
3. Use the Android app with integrated Aegis SFE Client SDK
4. Or use a tool like Postman with proper request signing