package com.gradientgeeks.aegis.sfe.dto;

import com.gradientgeeks.aegis.sfe.entity.Policy;

public class SignatureValidationResponse {
    
    private boolean isValid;
    private String message;
    private String deviceId;
    private Boolean requiresMfa;
    private Policy.EnforcementLevel policyEnforcement;
    
    public SignatureValidationResponse() {}
    
    public SignatureValidationResponse(boolean isValid) {
        this.isValid = isValid;
        this.message = isValid ? "Signature is valid" : "Signature is invalid";
    }
    
    public SignatureValidationResponse(boolean isValid, String message) {
        this.isValid = isValid;
        this.message = message;
    }
    
    public SignatureValidationResponse(boolean isValid, String message, String deviceId) {
        this.isValid = isValid;
        this.message = message;
        this.deviceId = deviceId;
    }
    
    public boolean isValid() {
        return isValid;
    }
    
    public void setValid(boolean valid) {
        isValid = valid;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getDeviceId() {
        return deviceId;
    }
    
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
    
    public Boolean getRequiresMfa() {
        return requiresMfa;
    }
    
    public void setRequiresMfa(Boolean requiresMfa) {
        this.requiresMfa = requiresMfa;
    }
    
    public Policy.EnforcementLevel getPolicyEnforcement() {
        return policyEnforcement;
    }
    
    public void setPolicyEnforcement(Policy.EnforcementLevel policyEnforcement) {
        this.policyEnforcement = policyEnforcement;
    }
    
    @Override
    public String toString() {
        return "SignatureValidationResponse{" +
                "isValid=" + isValid +
                ", message='" + message + '\'' +
                ", deviceId='" + deviceId + '\'' +
                ", requiresMfa=" + requiresMfa +
                ", policyEnforcement=" + policyEnforcement +
                '}';
    }
}