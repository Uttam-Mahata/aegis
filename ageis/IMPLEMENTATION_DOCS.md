# Aegis Security API Implementation Documentation

## Overview

The Aegis Security API is a comprehensive Spring Boot application that provides secure device registration and signature validation services for mobile applications. This document details the implementation of each component.

## Architecture

The API follows a layered architecture:
- **Controller Layer**: REST endpoints for device registration and signature validation
- **Service Layer**: Business logic for cryptography, device management, and validation
- **Repository Layer**: Data access using Spring Data JPA
- **Entity Layer**: JPA entities for persistence
- **Configuration Layer**: Security, CORS, and Redis configuration

## Core Components

### 1. Entity Layer

#### Device Entity (`Device.java`)
- **Purpose**: Represents a registered device in the system
- **Key Fields**:
  - `deviceId`: Unique identifier for the device (generated)
  - `clientId`: Identifies the client application (e.g., "UCOBANK_PROD_ANDROID")
  - `secretKey`: Cryptographically secure key for HMAC signing
  - `isActive`: Boolean flag for device status
  - `lastSeen`: Timestamp of last activity
- **Security Features**:
  - Indexed on deviceId and clientId for performance
  - Sensitive data (secretKey) is properly handled
  - Audit timestamps with @CreationTimestamp and @UpdateTimestamp

#### RegistrationKey Entity (`RegistrationKey.java`)
- **Purpose**: Manages registration keys for client onboarding
- **Key Fields**:
  - `clientId`: Associated client identifier
  - `registrationKey`: Cryptographically secure registration key
  - `description`: Human-readable description
  - `isActive`: Key status flag
  - `expiresAt`: Optional expiration timestamp
- **Security Features**:
  - Unique constraints on clientId and registrationKey
  - Built-in expiration checking with `isExpired()` method

### 2. Service Layer

#### CryptographyService (`CryptographyService.java`)
- **Purpose**: Handles all cryptographic operations
- **Key Methods**:
  - `generateSecretKey()`: Creates 256-bit cryptographically secure keys
  - `generateRegistrationKey()`: Creates registration keys using BigInteger
  - `computeHmacSha256()`: Implements HMAC-SHA256 signing
  - `verifyHmacSha256()`: Constant-time signature verification
  - `hashString()`: SHA-256 hashing utility
- **Mathematical Implementation**:
  ```java
  // Key Generation: Uses BigInteger with 256-bit entropy
  String secretKey = new BigInteger(256, SECURE_RANDOM).toString(32);
  
  // HMAC-SHA256 Calculation
  Mac mac = Mac.getInstance("HmacSHA256");
  SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(), "HmacSHA256");
  mac.init(secretKeySpec);
  byte[] hashBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
  return Base64.getEncoder().encodeToString(hashBytes);
  ```
- **Security Features**:
  - Uses SecureRandom for entropy
  - Constant-time comparison to prevent timing attacks
  - Base64 validation for signature format

#### DeviceRegistrationService (`DeviceRegistrationService.java`)
- **Purpose**: Manages device registration workflow
- **Registration Process**:
  1. Validates integrity token (simulated for hackathon)
  2. Verifies registration key exists and is active
  3. Checks client ID matches
  4. Verifies key hasn't expired
  5. Generates unique device ID and secret key
  6. Persists device record
- **Security Validations**:
  - Registration key validation
  - Client ID verification
  - Expiration checking
  - Transaction management for data consistency

#### SignatureValidationService (`SignatureValidationService.java`)
- **Purpose**: Validates HMAC signatures from client devices
- **Validation Process**:
  1. Looks up device by deviceId
  2. Verifies device is active
  3. Validates signature format (Base64)
  4. Computes expected signature using stored secret key
  5. Performs constant-time comparison
  6. Updates device last seen timestamp
- **Mathematical Verification**:
  ```java
  // Expected signature calculation
  String expectedSignature = HMAC_SHA256(device.secretKey, stringToSign);
  
  // Constant-time comparison
  boolean isValid = MessageDigest.isEqual(
      expectedSignature.getBytes(UTF_8),
      providedSignature.getBytes(UTF_8)
  );
  ```

#### IntegrityValidationService (`IntegrityValidationService.java`)
- **Purpose**: Validates Google Play Integrity tokens (simulated)
- **Production Implementation**: Would validate JWS tokens against Google's public keys
- **Hackathon Mode**: Bypasses validation for demonstration purposes
- **Security Notes**: Includes detailed logging for production implementation steps

### 3. Controller Layer

#### DeviceController (`DeviceController.java`)
- **Endpoints**:
  - `POST /v1/register`: Device registration
  - `POST /v1/validate`: Signature validation  
  - `GET /v1/health`: Health check
- **Security Features**:
  - Input validation with @Valid annotations
  - Comprehensive error handling
  - Structured logging for security events
  - CORS support for cross-origin requests

#### AdminController (`AdminController.java`)
- **Purpose**: Administrative interface for registration key management
- **Endpoints**:
  - `POST /admin/registration-keys`: Generate new registration key
  - `GET /admin/registration-keys`: List all keys
  - `GET /admin/registration-keys/{clientId}`: Get specific key
  - `PUT /admin/registration-keys/{clientId}/revoke`: Revoke key
  - `PUT /admin/registration-keys/{clientId}/regenerate`: Regenerate key
- **Security**: Requires authentication (configured in SecurityConfig)

### 4. Configuration Layer

#### SecurityConfig (`SecurityConfig.java`)
- **Purpose**: Configures Spring Security
- **Configuration**:
  - Stateless session management
  - CORS enabled for development
  - Public access to registration/validation endpoints
  - Authentication required for admin endpoints
  - CSRF disabled for API usage

#### RedisConfig (`RedisConfig.java`)
- **Purpose**: Configures Redis for caching and session storage
- **Features**:
  - Lettuce connection factory
  - JSON serialization for complex objects
  - String template for simple key-value operations
  - Connection pooling configuration

## Database Schema

### Tables Created
1. **devices**
   - Primary key: `id` (auto-increment)
   - Unique index on `device_id`
   - Index on `client_id`
   - Audit columns: `created_at`, `updated_at`

2. **registration_keys**
   - Primary key: `id` (auto-increment)
   - Unique index on `client_id`
   - Unique index on `registration_key`
   - Audit columns: `created_at`, `updated_at`

## API Security Flow

### Device Registration Flow
```
1. Client App → POST /v1/register
   {
     "clientId": "UCOBANK_PROD_ANDROID",
     "registrationKey": "shared-key-from-admin",
     "integrityToken": null (simulated)
   }

2. API validates registration key
3. API generates device ID and secret key
4. API returns credentials:
   {
     "deviceId": "dev_abc123",
     "secretKey": "secret-key-xyz789",
     "status": "success"
   }
```

### Signature Validation Flow
```
1. Bank Backend → POST /v1/validate
   {
     "deviceId": "dev_abc123",
     "signature": "base64-encoded-hmac",
     "stringToSign": "POST|/api/transfer|timestamp|nonce|body-hash"
   }

2. API looks up device secret key
3. API computes expected signature
4. API performs constant-time comparison
5. API returns validation result:
   {
     "isValid": true,
     "message": "Signature is valid",
     "deviceId": "dev_abc123"
   }
```

## Testing

### Unit Tests Implemented
- **CryptographyServiceTest**: Tests all cryptographic operations
- **DeviceRegistrationServiceTest**: Tests device registration workflow
- **DeviceControllerTest**: Tests REST endpoints with MockMvc

### Test Coverage
- Cryptographic function correctness
- Error handling scenarios
- Input validation
- Business logic edge cases
- HTTP response codes and content

## Configuration

### Application Properties
```properties
# Database (PostgreSQL)
spring.datasource.url=jdbc:postgresql://localhost:5432/aegis_security
spring.datasource.username=aegis
spring.datasource.password=aegis_123

# Redis
spring.data.redis.host=localhost
spring.data.redis.port=6379

# Security
server.servlet.context-path=/api
```

## Security Considerations

### Implemented Security Measures
1. **Cryptographic Security**:
   - 256-bit entropy for key generation
   - HMAC-SHA256 for message authentication
   - Constant-time comparison to prevent timing attacks
   - Secure random number generation

2. **Input Validation**:
   - Bean validation annotations
   - Size limits on all string fields
   - Format validation for Base64 signatures

3. **Error Handling**:
   - No sensitive information in error messages
   - Structured logging for security events
   - Graceful handling of edge cases

4. **Database Security**:
   - Indexed queries for performance
   - Transaction management
   - Audit trails with timestamps

### Production Considerations
1. **Play Integrity API**: Replace simulation with real Google Play Integrity validation
2. **Authentication**: Implement OAuth2 or JWT for admin endpoints
3. **Rate Limiting**: Add rate limiting to prevent abuse
4. **Monitoring**: Integrate with monitoring and alerting systems
5. **Key Rotation**: Implement key rotation mechanisms
6. **SSL/TLS**: Ensure all communications are encrypted

## Deployment

### Prerequisites
- PostgreSQL 12+
- Redis 6+
- Java 21
- Gradle 8+

### Build Commands
```bash
# Build the application
./gradlew build

# Run tests
./gradlew test

# Start the application
./gradlew bootRun
```

### Docker Support
The application is containerizable with standard Spring Boot Docker practices.

## Conclusion

The Aegis Security API provides a robust, enterprise-grade foundation for mobile application security. The implementation follows security best practices and provides a complete solution for device registration and request validation. The modular architecture allows for easy extension and maintenance while maintaining high security standards.