package com.gradientgeeks.ageis.sfe.dto;

public class DeviceRegistrationResponse {
    
    private String deviceId;
    private String secretKey;
    private String status;
    private String message;
    
    public DeviceRegistrationResponse() {}
    
    public DeviceRegistrationResponse(String deviceId, String secretKey) {
        this.deviceId = deviceId;
        this.secretKey = secretKey;
        this.status = "success";
        this.message = "Device registered successfully";
    }
    
    public DeviceRegistrationResponse(String status, String message) {
        this.status = status;
        this.message = message;
    }
    
    public String getDeviceId() {
        return deviceId;
    }
    
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
    
    public String getSecretKey() {
        return secretKey;
    }
    
    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    @Override
    public String toString() {
        return "DeviceRegistrationResponse{" +
                "deviceId='" + deviceId + '\'' +
                ", secretKey='[REDACTED]'" +
                ", status='" + status + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}