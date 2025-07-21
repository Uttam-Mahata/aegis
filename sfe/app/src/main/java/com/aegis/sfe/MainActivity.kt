package com.aegis.sfe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aegis.sfe.ui.navigation.AppNavigation
import com.aegis.sfe.ui.navigation.Screen
import com.aegis.sfe.ui.theme.SfeTheme
import com.aegis.sfe.ui.viewmodel.DeviceProvisioningViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        Thread.setDefaultUncaughtExceptionHandler { _, exception ->
            android.util.Log.e("MainActivity", "Uncaught exception", exception)
        }
        
        setContent {
            SfeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    UCOBankApp()
                }
            }
        }
    }
}

@Composable
fun UCOBankApp() {
    val provisioningViewModel: DeviceProvisioningViewModel = viewModel()
    val provisioningState by provisioningViewModel.provisioningState.collectAsState()
    
    // Determine the start destination based on provisioning and login status
    val startDestination = when {
        !provisioningState.isProvisioned -> Screen.DeviceProvisioning.route
        UCOBankApplication.currentUser == null -> Screen.Login.route
        else -> Screen.Dashboard.route
    }
    
    AppNavigation(startDestination = startDestination)
}