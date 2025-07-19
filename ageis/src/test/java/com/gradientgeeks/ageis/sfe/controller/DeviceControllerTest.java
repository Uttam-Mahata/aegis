package com.gradientgeeks.ageis.sfe.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gradientgeeks.ageis.sfe.dto.DeviceRegistrationRequest;
import com.gradientgeeks.ageis.sfe.dto.DeviceRegistrationResponse;
import com.gradientgeeks.ageis.sfe.dto.SignatureValidationRequest;
import com.gradientgeeks.ageis.sfe.dto.SignatureValidationResponse;
import com.gradientgeeks.ageis.sfe.service.DeviceRegistrationService;
import com.gradientgeeks.ageis.sfe.service.SignatureValidationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DeviceController.class)
class DeviceControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private DeviceRegistrationService deviceRegistrationService;
    
    @MockBean
    private SignatureValidationService signatureValidationService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    void testRegisterDeviceSuccess() throws Exception {
        DeviceRegistrationRequest request = new DeviceRegistrationRequest(
            "UCOBANK_PROD_ANDROID",
            "valid-registration-key",
            null
        );
        
        DeviceRegistrationResponse response = new DeviceRegistrationResponse(
            "dev_12345",
            "secret-key-12345"
        );
        
        when(deviceRegistrationService.registerDevice(any(DeviceRegistrationRequest.class)))
            .thenReturn(response);
        
        mockMvc.perform(post("/v1/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.deviceId").value("dev_12345"))
                .andExpect(jsonPath("$.secretKey").value("secret-key-12345"));
    }
    
    @Test
    void testRegisterDeviceInvalidRequest() throws Exception {
        DeviceRegistrationRequest request = new DeviceRegistrationRequest(
            "", // Invalid empty clientId
            "valid-registration-key",
            null
        );
        
        mockMvc.perform(post("/v1/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    void testRegisterDeviceError() throws Exception {
        DeviceRegistrationRequest request = new DeviceRegistrationRequest(
            "UCOBANK_PROD_ANDROID",
            "invalid-registration-key",
            null
        );
        
        DeviceRegistrationResponse response = new DeviceRegistrationResponse(
            "error",
            "Invalid registration key"
        );
        
        when(deviceRegistrationService.registerDevice(any(DeviceRegistrationRequest.class)))
            .thenReturn(response);
        
        mockMvc.perform(post("/v1/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Invalid registration key"));
    }
    
    @Test
    void testValidateSignatureSuccess() throws Exception {
        SignatureValidationRequest request = new SignatureValidationRequest(
            "dev_12345",
            "valid-signature",
            "POST|/api/transfer|1234567890|nonce123|body-hash"
        );
        
        SignatureValidationResponse response = new SignatureValidationResponse(
            true,
            "Signature is valid",
            "dev_12345"
        );
        
        when(signatureValidationService.validateSignature(any(SignatureValidationRequest.class)))
            .thenReturn(response);
        
        mockMvc.perform(post("/v1/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpected(jsonPath("$.isValid").value(true))
                .andExpect(jsonPath("$.message").value("Signature is valid"))
                .andExpect(jsonPath("$.deviceId").value("dev_12345"));
    }
    
    @Test
    void testValidateSignatureInvalid() throws Exception {
        SignatureValidationRequest request = new SignatureValidationRequest(
            "dev_12345",
            "invalid-signature",
            "POST|/api/transfer|1234567890|nonce123|body-hash"
        );
        
        SignatureValidationResponse response = new SignatureValidationResponse(
            false,
            "Signature is invalid"
        );
        
        when(signatureValidationService.validateSignature(any(SignatureValidationRequest.class)))
            .thenReturn(response);
        
        mockMvc.perform(post("/v1/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.isValid").value(false))
                .andExpect(jsonPath("$.message").value("Signature is invalid"));
    }
    
    @Test
    void testValidateSignatureInvalidRequest() throws Exception {
        SignatureValidationRequest request = new SignatureValidationRequest(
            "", // Invalid empty deviceId
            "signature",
            "string-to-sign"
        );
        
        mockMvc.perform(post("/v1/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    void testHealthEndpoint() throws Exception {
        mockMvc.perform(get("/v1/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("Aegis Security API is running"));
    }
}