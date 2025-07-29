package com.gradientgeeks.aegis.sfe_client.security

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.telephony.TelephonyManager
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import com.gradientgeeks.aegis.sfe_client.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.*

/**
 * Device fingerprinting service that collects stable hardware characteristics
 * to create a persistent device identity that survives factory resets and app reinstalls.
 * 
 * This service focuses on hardware attributes that don't change and cannot be easily
 * modified by users, providing fraud detection capabilities while respecting privacy.
 */
class DeviceFingerprintingService(private val context: Context) {
    
    companion object {
        private const val TAG = "DeviceFingerprintingService"
        private const val FINGERPRINT_VERSION = "1.0"
    }
    
    /**
     * Generates a comprehensive device fingerprint based on stable hardware characteristics.
     * 
     * @return DeviceFingerprint containing all collected characteristics
     */
    suspend fun generateDeviceFingerprint(): DeviceFingerprint = withContext(Dispatchers.IO) {
        Log.d(TAG, "Generating device fingerprint")
        
        val hardware = collectHardwareFingerprint()
        val display = collectDisplayFingerprint()
        val sensors = collectSensorFingerprint()
        val network = collectNetworkFingerprint()
        
        val fingerprint = DeviceFingerprint(
            version = FINGERPRINT_VERSION,
            hardwareFingerprint = hardware,
            displayFingerprint = display,
            sensorFingerprint = sensors,
            networkFingerprint = network,
            timestamp = System.currentTimeMillis()
        )
        
        Log.d(TAG, "Device fingerprint generated: ${fingerprint.getCompositeHash()}")
        fingerprint
    }
    
    /**
     * Collects hardware-specific characteristics that are stable across resets.
     */
    private fun collectHardwareFingerprint(): HardwareFingerprint {
        val fingerprint = HardwareFingerprint(
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            device = Build.DEVICE,
            product = Build.PRODUCT,
            board = Build.BOARD,
            brand = Build.BRAND,
            hardware = Build.HARDWARE,
            cpuArchitecture = Build.SUPPORTED_ABIS.joinToString(","),
            androidVersion = Build.VERSION.RELEASE,
            apiLevel = Build.VERSION.SDK_INT,
            buildFingerprint = Build.FINGERPRINT
        )
        
        Log.d(TAG, "Hardware fingerprint collected - Manufacturer: ${fingerprint.manufacturer}, " +
            "Model: ${fingerprint.model}, Device: ${fingerprint.device}, " +
            "Board: ${fingerprint.board}, Hash: ${fingerprint.getHash()}")
        
        return fingerprint
    }
    
    /**
     * Collects display characteristics.
     */
    private fun collectDisplayFingerprint(): DisplayFingerprint {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        
        return DisplayFingerprint(
            widthPixels = displayMetrics.widthPixels,
            heightPixels = displayMetrics.heightPixels,
            densityDpi = displayMetrics.densityDpi,
            xdpi = displayMetrics.xdpi,
            ydpi = displayMetrics.ydpi,
            scaledDensity = displayMetrics.scaledDensity
        )
    }
    
    /**
     * Collects available sensor information.
     */
    private fun collectSensorFingerprint(): SensorFingerprint {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val allSensors = sensorManager.getSensorList(Sensor.TYPE_ALL)
        
        val sensorTypes = allSensors.map { it.type }.sorted()
        val sensorNames = allSensors.map { "${it.name}:${it.vendor}" }.sorted()
        
        return SensorFingerprint(
            availableSensorTypes = sensorTypes,
            sensorDetails = sensorNames,
            sensorCount = allSensors.size
        )
    }
    
    /**
     * Collects network-related characteristics.
     */
    @SuppressLint("MissingPermission")
    private fun collectNetworkFingerprint(): NetworkFingerprint {
        return try {
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            
            NetworkFingerprint(
                networkOperatorName = telephonyManager.networkOperatorName ?: "unknown",
                networkCountryIso = telephonyManager.networkCountryIso ?: "unknown",
                simCountryIso = telephonyManager.simCountryIso ?: "unknown",
                phoneType = telephonyManager.phoneType,
                networkType = telephonyManager.networkType
            )
        } catch (e: SecurityException) {
            Log.w(TAG, "Unable to collect network fingerprint due to missing permissions", e)
            NetworkFingerprint(
                networkOperatorName = "permission_denied",
                networkCountryIso = "permission_denied", 
                simCountryIso = "permission_denied",
                phoneType = -1,
                networkType = -1
            )
        }
    }
}

/**
 * Complete device fingerprint containing all collected characteristics.
 */
data class DeviceFingerprint(
    val version: String,
    val hardwareFingerprint: HardwareFingerprint,
    val displayFingerprint: DisplayFingerprint,
    val sensorFingerprint: SensorFingerprint,
    val networkFingerprint: NetworkFingerprint,
    val timestamp: Long
) {
    /**
     * Generates a SHA-256 hash of the composite fingerprint for efficient comparison.
     */
    fun getCompositeHash(): String {
        val composite = buildString {
            append(hardwareFingerprint.getHash())
            append(displayFingerprint.getHash())
            append(sensorFingerprint.getHash())
            append(networkFingerprint.getHash())
        }
        
        return MessageDigest.getInstance("SHA-256")
            .digest(composite.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
    
    /**
     * Calculates similarity score with another fingerprint (0.0 to 1.0).
     */
    fun calculateSimilarity(other: DeviceFingerprint): Double {
        val hardwareSimilarity = hardwareFingerprint.calculateSimilarity(other.hardwareFingerprint)
        val displaySimilarity = displayFingerprint.calculateSimilarity(other.displayFingerprint)
        val sensorSimilarity = sensorFingerprint.calculateSimilarity(other.sensorFingerprint)
        val networkSimilarity = networkFingerprint.calculateSimilarity(other.networkFingerprint)
        
        // Weighted average: hardware and display are most important
        return (hardwareSimilarity * 0.4 + 
                displaySimilarity * 0.3 + 
                sensorSimilarity * 0.2 + 
                networkSimilarity * 0.1)
    }
}

/**
 * Hardware characteristics that are stable across factory resets.
 */
data class HardwareFingerprint(
    val manufacturer: String,
    val model: String,
    val device: String,
    val product: String,
    val board: String,
    val brand: String,
    val hardware: String,
    val cpuArchitecture: String,
    val androidVersion: String,
    val apiLevel: Int,
    val buildFingerprint: String
) {
    fun getHash(): String {
        // Normalize values to ensure consistency across app installs
        val normalizedComposite = listOf(
            manufacturer.trim().lowercase(),
            model.trim().lowercase(),
            device.trim().lowercase(),
            product.trim().lowercase(),
            board.trim().lowercase(),
            brand.trim().lowercase(),
            hardware.trim().lowercase(),
            cpuArchitecture.trim().lowercase(),
            apiLevel.toString()
        ).joinToString(":")
        
        return MessageDigest.getInstance("SHA-256")
            .digest(normalizedComposite.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
    
    fun calculateSimilarity(other: HardwareFingerprint): Double {
        var matches = 0
        var total = 0
        
        // Core hardware characteristics (most important)
        if (manufacturer == other.manufacturer) matches += 3
        total += 3
        
        if (model == other.model) matches += 3
        total += 3
        
        if (device == other.device) matches += 2
        total += 2
        
        if (board == other.board) matches += 2
        total += 2
        
        if (cpuArchitecture == other.cpuArchitecture) matches += 2
        total += 2
        
        // Less critical characteristics
        if (brand == other.brand) matches += 1
        total += 1
        
        if (hardware == other.hardware) matches += 1
        total += 1
        
        return matches.toDouble() / total.toDouble()
    }
}

/**
 * Display characteristics.
 */
data class DisplayFingerprint(
    val widthPixels: Int,
    val heightPixels: Int,
    val densityDpi: Int,
    val xdpi: Float,
    val ydpi: Float,
    val scaledDensity: Float
) {
    fun getHash(): String {
        val composite = "$widthPixels:$heightPixels:$densityDpi"
        return MessageDigest.getInstance("SHA-256")
            .digest(composite.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
    
    fun calculateSimilarity(other: DisplayFingerprint): Double {
        return if (widthPixels == other.widthPixels && 
                  heightPixels == other.heightPixels && 
                  densityDpi == other.densityDpi) 1.0 else 0.0
    }
}

/**
 * Available sensor information.
 */
data class SensorFingerprint(
    val availableSensorTypes: List<Int>,
    val sensorDetails: List<String>,
    val sensorCount: Int
) {
    fun getHash(): String {
        val composite = "${availableSensorTypes.sorted().joinToString(",")}:$sensorCount"
        return MessageDigest.getInstance("SHA-256")
            .digest(composite.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
    
    fun calculateSimilarity(other: SensorFingerprint): Double {
        val commonSensors = availableSensorTypes.intersect(other.availableSensorTypes.toSet()).size
        val totalUniqueSensors = (availableSensorTypes + other.availableSensorTypes).toSet().size
        
        return if (totalUniqueSensors > 0) {
            commonSensors.toDouble() / totalUniqueSensors.toDouble()
        } else 1.0
    }
}

/**
 * Network-related characteristics.
 */
data class NetworkFingerprint(
    val networkOperatorName: String,
    val networkCountryIso: String,
    val simCountryIso: String,
    val phoneType: Int,
    val networkType: Int
) {
    fun getHash(): String {
        val composite = "$networkOperatorName:$networkCountryIso:$simCountryIso:$phoneType"
        return MessageDigest.getInstance("SHA-256")
            .digest(composite.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
    
    fun calculateSimilarity(other: NetworkFingerprint): Double {
        var matches = 0
        var total = 0
        
        if (networkCountryIso == other.networkCountryIso) matches += 2
        total += 2
        
        if (simCountryIso == other.simCountryIso) matches += 2
        total += 2
        
        if (networkOperatorName == other.networkOperatorName) matches += 1
        total += 1
        
        if (phoneType == other.phoneType) matches += 1
        total += 1
        
        return if (total > 0) matches.toDouble() / total.toDouble() else 1.0
    }
}

/**
 * Extension function to convert DeviceFingerprint to DeviceFingerprintData for API transmission.
 */
fun DeviceFingerprint.toApiData(): DeviceFingerprintData {
    return DeviceFingerprintData(
        version = this.version,
        compositeHash = this.getCompositeHash(),
        hardware = this.hardwareFingerprint.toApiData(),
        display = this.displayFingerprint.toApiData(),
        sensors = this.sensorFingerprint.toApiData(),
        network = this.networkFingerprint.toApiData(),
        timestamp = this.timestamp
    )
}

/**
 * Extension function to convert HardwareFingerprint to HardwareFingerprintData.
 */
fun HardwareFingerprint.toApiData(): HardwareFingerprintData {
    return HardwareFingerprintData(
        manufacturer = this.manufacturer,
        model = this.model,
        device = this.device,
        board = this.board,
        brand = this.brand,
        cpuArchitecture = this.cpuArchitecture,
        apiLevel = this.apiLevel,
        hash = this.getHash()
    )
}

/**
 * Extension function to convert DisplayFingerprint to DisplayFingerprintData.
 */
fun DisplayFingerprint.toApiData(): DisplayFingerprintData {
    return DisplayFingerprintData(
        widthPixels = this.widthPixels,
        heightPixels = this.heightPixels,
        densityDpi = this.densityDpi,
        hash = this.getHash()
    )
}

/**
 * Extension function to convert SensorFingerprint to SensorFingerprintData.
 */
fun SensorFingerprint.toApiData(): SensorFingerprintData {
    return SensorFingerprintData(
        sensorTypes = this.availableSensorTypes,
        sensorCount = this.sensorCount,
        hash = this.getHash()
    )
}

/**
 * Extension function to convert NetworkFingerprint to NetworkFingerprintData.
 */
fun NetworkFingerprint.toApiData(): NetworkFingerprintData {
    return NetworkFingerprintData(
        networkCountryIso = this.networkCountryIso,
        simCountryIso = this.simCountryIso,
        phoneType = this.phoneType,
        hash = this.getHash()
    )
}