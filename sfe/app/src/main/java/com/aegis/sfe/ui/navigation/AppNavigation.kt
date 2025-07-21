package com.aegis.sfe.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aegis.sfe.ui.screen.dashboard.DashboardScreen
import com.aegis.sfe.ui.screen.provisioning.DeviceProvisioningScreen
import com.aegis.sfe.ui.screen.transfer.TransferScreen
import com.aegis.sfe.ui.screen.history.TransactionHistoryScreen

sealed class Screen(val route: String) {
    object DeviceProvisioning : Screen("device_provisioning")
    object Dashboard : Screen("dashboard")
    object Transfer : Screen("transfer")
    object TransactionHistory : Screen("transaction_history")
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.DeviceProvisioning.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.DeviceProvisioning.route) {
            DeviceProvisioningScreen(
                onProvisioningComplete = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.DeviceProvisioning.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToTransfer = {
                    navController.navigate(Screen.Transfer.route)
                },
                onNavigateToHistory = {
                    navController.navigate(Screen.TransactionHistory.route)
                }
            )
        }
        
        composable(Screen.Transfer.route) {
            TransferScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onTransferComplete = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.TransactionHistory.route) {
            TransactionHistoryScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}