package com.aegis.sfe

import android.app.Application
import com.gradientgeeks.aegis.sfe_client.AegisSfeClient

class UCOBankApplication : Application() {
    
    companion object {
        // Configuration constants
        const val AEGIS_API_BASE_URL = "http://192.168.232.12:8080/api"  // Physical device
        const val BANK_API_BASE_URL = "http://192.168.232.12:8081/api/v1"  // Physical device
        const val CLIENT_ID = "UCOBANK_PROD_ANDROID"
        const val REGISTRATION_KEY = "ucobank_registration_key_2025"
        
        // Global SDK instance
        lateinit var aegisClient: AegisSfeClient
            private set
            
        // Auth state
        var authToken: String? = null
        var currentUser: com.aegis.sfe.data.model.UserInfo? = null
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Aegis SFE Client SDK
        try {
            aegisClient = AegisSfeClient.initialize(
                context = this,
                baseUrl = AEGIS_API_BASE_URL
            )
            
            // Log SDK initialization
            android.util.Log.d("UCOBankApp", "Aegis SDK initialized successfully")
            
        } catch (e: Exception) {
            android.util.Log.e("UCOBankApp", "Failed to initialize Aegis SDK", e)
            throw RuntimeException("SDK initialization failed", e)
        }
    }
}