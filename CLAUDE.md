# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is the **Aegis Security Environment** - a comprehensive security system for mobile applications consisting of three main components:

1. **`ageis/`** - Main Aegis Security API (Spring Boot)
   - Central backend that handles device registration and signature validation
   - Uses PostgreSQL and Redis for persistence and caching
   - Implements cryptographic key provisioning and HMAC validation

2. **`backend-app/`** - Demo Bank Backend (Spring Boot)
   - Simulates a bank's backend system (e.g., "UCO Bank Backend")
   - Acts as client to the Aegis Security API for request validation
   - Demonstrates end-to-end security flow

3. **`sfe/`** - Android Security Project
   - **`sfe-client/`** - Headless Aegis Client SDK (Android Library)
     - Core security library providing cryptographic functions
     - Handles device provisioning, HMAC signing, and secure storage
   - **`app/`** - Demo Android App
     - Sample banking app that integrates the sfe-client SDK
     - Built with Jetpack Compose

## Common Development Commands

### Spring Boot Projects (ageis & backend-app)
```bash
# Build and run tests
./gradlew build

# Run tests only
./gradlew test

# Run specific project
./gradlew :ageis:bootRun
./gradlew :backend-app:bootRun

# Clean build
./gradlew clean build
```

### Android Project (sfe)
```bash
# Build all modules
./gradlew build

# Build and install debug APK
./gradlew :app:installDebug

# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest

# Build SDK library
./gradlew :sfe-client:build

# Clean build
./gradlew clean build
```

## Architecture Overview

### Security Flow
1. **Device Provisioning**: On first app launch, the SDK performs secure handshake with Aegis API to establish device identity using cryptographically secure random keys
2. **Request Signing**: All sensitive API calls are signed using HMAC-SHA256 with device-specific secret keys stored in Android Keystore
3. **Validation**: Bank backends validate requests through Aegis API using signature verification
4. **Secure Storage**: SDK provides envelope encryption (AES-256 + RSA) for sensitive data at rest

### Key Technologies
- **Spring Boot 3.5.3** with Java 21 for backend services
- **Android SDK 35** with Kotlin and Jetpack Compose
- **Cryptography**: HMAC-SHA256, AES-256, RSA, Android Keystore
- **Security**: Google Play Integrity API (simulated in demo), Spring Security
- **Data**: JPA with PostgreSQL, Redis for caching

### Package Structure
- Backend services use `com.gradientgeeks.ageis.*` packages
- Android components use `com.gradientgeeks.sfe.*` packages
- SDK uses `com.gradientgeeks.ageissfe_client.*` package

## Development Notes

- All Spring Boot projects require Java 21 and use JUnit 5 for testing
- Android projects target API 35 with minimum API 28
- The project simulates Google Play Integrity API for demonstration purposes
- Security implementation follows enterprise-grade cryptographic standards
- Both Spring projects have identical dependency structures (JPA, Redis, Security, Web, Validation)