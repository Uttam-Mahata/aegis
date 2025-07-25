package com.gradientgeeks.aegis.sfe.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Context object containing all data needed for policy evaluation
 */
public class PolicyContext {
    private String deviceId;
    private String clientId;
    private String userId;
    
    // Device information
    private String devicePlatform;
    private String deviceOsVersion;
    private String appVersion;
    private Boolean isRooted;
    
    // Transaction information
    private Double transactionAmount;
    private String transactionType;
    private String currency;
    private String accountNumber;
    
    // Request information
    private String httpMethod;
    private String requestUri;
    private LocalDateTime timestamp;
    private String ipAddress;
    private String userAgent;
    private String sessionId;
    
    // Location information
    private String country;
    private String city;
    private Double latitude;
    private Double longitude;
    
    // Additional context data
    private Map<String, Object> additionalData = new HashMap<>();
    
    // Builder pattern for easy construction
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private PolicyContext context = new PolicyContext();
        
        public Builder deviceId(String deviceId) {
            context.deviceId = deviceId;
            return this;
        }
        
        public Builder clientId(String clientId) {
            context.clientId = clientId;
            return this;
        }
        
        public Builder userId(String userId) {
            context.userId = userId;
            return this;
        }
        
        public Builder deviceInfo(String platform, String osVersion, String appVersion) {
            context.devicePlatform = platform;
            context.deviceOsVersion = osVersion;
            context.appVersion = appVersion;
            return this;
        }
        
        public Builder isRooted(Boolean isRooted) {
            context.isRooted = isRooted;
            return this;
        }
        
        public Builder transactionInfo(Double amount, String type, String currency) {
            context.transactionAmount = amount;
            context.transactionType = type;
            context.currency = currency;
            return this;
        }
        
        public Builder accountNumber(String accountNumber) {
            context.accountNumber = accountNumber;
            return this;
        }
        
        public Builder requestInfo(String method, String uri, LocalDateTime timestamp) {
            context.httpMethod = method;
            context.requestUri = uri;
            context.timestamp = timestamp;
            return this;
        }
        
        public Builder ipAddress(String ipAddress) {
            context.ipAddress = ipAddress;
            return this;
        }
        
        public Builder userAgent(String userAgent) {
            context.userAgent = userAgent;
            return this;
        }
        
        public Builder sessionId(String sessionId) {
            context.sessionId = sessionId;
            return this;
        }
        
        public Builder location(String country, String city, Double latitude, Double longitude) {
            context.country = country;
            context.city = city;
            context.latitude = latitude;
            context.longitude = longitude;
            return this;
        }
        
        public Builder additionalData(String key, Object value) {
            context.additionalData.put(key, value);
            return this;
        }
        
        public PolicyContext build() {
            return context;
        }
    }
    
    // Getters
    public String getDeviceId() {
        return deviceId;
    }
    
    public String getClientId() {
        return clientId;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public String getDevicePlatform() {
        return devicePlatform;
    }
    
    public String getDeviceOsVersion() {
        return deviceOsVersion;
    }
    
    public String getAppVersion() {
        return appVersion;
    }
    
    public Boolean getIsRooted() {
        return isRooted;
    }
    
    public Double getTransactionAmount() {
        return transactionAmount;
    }
    
    public String getTransactionType() {
        return transactionType;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public String getAccountNumber() {
        return accountNumber;
    }
    
    public String getHttpMethod() {
        return httpMethod;
    }
    
    public String getRequestUri() {
        return requestUri;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public String getIpAddress() {
        return ipAddress;
    }
    
    public String getUserAgent() {
        return userAgent;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public String getCountry() {
        return country;
    }
    
    public String getCity() {
        return city;
    }
    
    public Double getLatitude() {
        return latitude;
    }
    
    public Double getLongitude() {
        return longitude;
    }
    
    public Map<String, Object> getAdditionalData() {
        return additionalData;
    }
    
    @Override
    public String toString() {
        return "PolicyContext{" +
                "deviceId='" + deviceId + '\'' +
                ", clientId='" + clientId + '\'' +
                ", userId='" + userId + '\'' +
                ", transactionAmount=" + transactionAmount +
                ", transactionType='" + transactionType + '\'' +
                ", requestUri='" + requestUri + '\'' +
                ", timestamp=" + timestamp +
                ", ipAddress='" + ipAddress + '\'' +
                '}';
    }
}