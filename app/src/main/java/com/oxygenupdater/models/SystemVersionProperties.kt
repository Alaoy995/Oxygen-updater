package com.oxygenupdater.models

import android.os.Build
import com.oxygenupdater.BuildConfig
import com.oxygenupdater.internal.settings.PrefManager
import com.oxygenupdater.utils.Logger.logError
import com.oxygenupdater.utils.Logger.logVerbose
import java.io.BufferedReader
import java.io.IOException
import java.io.StringReader
import java.util.*

/**
 * Contains some properties of the OS / ROM installed on the device.
 *
 * Used to read / extract  OnePlus-specific properties from the ROM.
 */
object SystemVersionProperties {

    private const val TAG = "SystemVersionProperties"
    private const val SECURITY_PATCH_LOOKUP_KEY = "ro.build.version.security_patch"
    private const val RO_ROM_VERSION_LOOKUP_KEY = "ro.rom.version"

    /**
     * Hack #1 (OS_VERSION_NUMBER_LOOKUP_KEY): remove incorrect "H2OS" prefix from version value (OOS 2.0.0 - 3.x).
     * If this is the case, value is discarded and we try with the next item in "items" array.
     *
     * Note for GitHub contributors: this should never have to change, as it only applies to very old devices
     */
    private const val RO_ROM_VERSION_H2OS = "H2OS"

    /**
     * Hack #2 (OS_VERSION_NUMBER_LOOKUP_KEY): remove redundant "Oxygen OS " prefix from version value (OnePlus 7-series and newer).
     * As the app only shows the number or ads custom formatting, remove this prefix
     *
     * Note for GitHub contributors: change this value if `ro.oxygen.version` contains another prefix than "Oxygen OS ".
     */
    private val RO_ROM_VERSION_OXYGENOS_PREFIX = "Oxygen ?OS ?".toRegex()

    /**
     * Hack #3 (DEVICE_NAME_LOOKUP_KEY): Support for regional device variants (OnePlus 7-series and newer).
     * OnePlus 7 Pro and newer come in regional variants which cannot be detected by `ro.display.series`.
     * However, its alternative (`ro.product.name`) does not play nice with values present on older devices.
     * Bypass: if the key is `ro.display.series` and the value is not any of the older devices, then read
     * `ro.product.name` instead to detect the correct device.
     */
    private const val RO_DISPLAY_SERIES_LOOKUP_KEY = "ro.display.series"
    private const val RO_BUILD_SOFT_VERSION_LOOKUP_KEY = "ro.build.soft.version"

    /**
     * This isn't a hack, but was introduced in the first Open Beta for 7T-series
     * These keys will be checked for on all devices, for better future-proofing, and saved to SharedPreferences
     * The saved SharedPreferences value will be used while sending a POST request to the `/submit-update-file` endpoint,
     * so that it's easy for contributors on Discord to figure out which build is for which region
     * (backend will take this into account while firing the webhook).
     */
    private val RO_BUILD_EU_LOOKUP_KEYS = arrayOf("ro.build.eu", "ro.vendor.build.eu")

    /**
     * This is a hack for Nord 2 (maybe future devices too), since it doesn't have any of the above EU keys.
     * This key will be checked *after* the above keys are checked, as a backup (if above keys aren't found).
     *
     * Note: to keep things simple, we're checking if the value corresponding to this key starts with `EU`,
     * even though we've seen that (at least on Nord 2), it's `EUEX` for EU devices, and `IN` for India.
     */
    private const val RO_VENDOR_OPLUS_REGIONMARK_LOOKUP_KEY = "ro.vendor.oplus.regionmark"

    /**
     * Easy way to quickly check if the current build is stable, beta, or alpha.
     * Supported only in 7-series and above.
     */
    private const val RO_BUILD_OS_TYPE_LOOKUP_KEY = "ro.build.os_type"

    /**
     * From v4.5.1 onwards, `ro.product.name` is read by default, except for OP1 - OP6T.
     * This change was done to simplify supporting new devices, assuming all new devices
     * follow the trend set by OP7-series onwards (i.e. `ro.product.name` containing region
     * information).
     *
     * Note for GitHub contributors: add to the list if newer devices need to be excluded.
     */
    private val RO_PRODUCT_NAME_LOOKUP_DEVICES_IGNORE = setOf(
        "OnePlus",
        "OnePlus 2",
        "OnePlus X",
        "OnePlus 3",
        "OnePlus 3T",
        "OnePlus 5",
        "OnePlus 5T",
        "OnePlus 6",
        "OnePlus 6T"
    )

    /**
     * Hack #4 (BUILD_SOFT_VERSION_LOOKUP_KEY): support for Indian variants in OnePlus 7T-series
     * OnePlus started rolling out India-specific features with the 7T-series, but there was no
     * distinction between ro.product.name in Indian and international variants.
     * Only workaround is to read `ro.build.soft.version`.
     */
    private val RO_BUILD_SOFT_VERSION_LOOKUP_DEVICES = setOf(
        "OnePlus7T",
        "OnePlus7TPro"
    )

    /**
     * Matchable name of the device. Must be present in the Devices returned by ServerConnector
     */
    val oxygenDeviceName: String

    /**
     * Human-readable OxygenOS version. Shown within the UI of the app
     */
    val oxygenOSVersion: String

    /**
     * Technical / OTA version of OxygenOS. Used to check for updates and shown in Device Info tab
     */
    val oxygenOSOTAVersion: String

    /**
     * Security patch date. Must be looked up manually on Android versions < 6.0
     */
    val securityPatchDate: String

    /**
     * Fingerprint of the build. Used to check if the device uses an official build of OxygenOS
     */
    val fingerprint: String = Build.FINGERPRINT.trim().also {
        logVerbose(TAG, "Detected build fingerprint: $it")
    }

    /**
     * This prop is present only on 7-series and above. Possible values:
     * 1. Stable: if property is present, but has no value
     * 2. Beta: if property is present and has "Beta" as its value
     * 3. Alpha: if property is present and has "Alpha" as its value
     * 4. <blank>: if property isn't present at all (i.e. unknown OS type)
     */
    val osType: String

    init {
        var oxygenDeviceName = Build.UNKNOWN
        var oxygenOSVersion = Build.UNKNOWN
        var oxygenOSOTAVersion = Build.UNKNOWN
        var securityPatchDate = Build.UNKNOWN
        var osType = Build.UNKNOWN

        try {
            val getBuildPropProcess = Runtime.getRuntime().exec("getprop")

            logVerbose(TAG, "Started fetching device properties using 'getprop' command")

            val scanner = Scanner(getBuildPropProcess.inputStream).useDelimiter("\\A")
            val properties = if (scanner.hasNext()) scanner.next() else ""

            getBuildPropProcess.destroy()

            oxygenDeviceName = readBuildPropItem(BuildConfig.DEVICE_NAME_LOOKUP_KEYS, properties, "Detected device: %s")
            oxygenOSVersion = readBuildPropItem(BuildConfig.OS_VERSION_NUMBER_LOOKUP_KEYS, properties, "Detected OxygenOS ROM with version: %s").also {
                if (it == Build.UNKNOWN) oxygenOSVersion = Build.DISPLAY
            }
            oxygenOSOTAVersion = readBuildPropItem(BuildConfig.OS_OTA_VERSION_NUMBER_LOOKUP_KEY, properties, "Detected OxygenOS ROM with OTA version: %s")
            osType = readBuildPropItem(RO_BUILD_OS_TYPE_LOOKUP_KEY, properties, "Detected OS Type: %s")

            val euBooleanStr = readBuildPropItem(RO_BUILD_EU_LOOKUP_KEYS, properties, "isEuBuild: %s")
            val isEuBuild = if (euBooleanStr == Build.UNKNOWN) {
                readBuildPropItem(RO_VENDOR_OPLUS_REGIONMARK_LOOKUP_KEY, properties, "isEuBuild: %s").startsWith("EU")
            } else {
                euBooleanStr.toBoolean()
            }

            // This prop is present only on 7-series and above
            if (osType.isBlank()) {
                osType = "Stable"
            } else if (osType == Build.UNKNOWN) {
                osType = ""
            }

            PrefManager.putBoolean(PrefManager.PROPERTY_IS_EU_BUILD, isEuBuild)

            securityPatchDate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Already available using Android API since Android 6.0
                Build.VERSION.SECURITY_PATCH
            } else {
                readBuildPropItem(SECURITY_PATCH_LOOKUP_KEY, properties, "Detected security patch level: %s")
            }

            logVerbose(TAG, "Finished fetching device properties using 'getprop' command")
        } catch (e: Exception) {
            logError(TAG, e.localizedMessage ?: "$e", e)
        }

        this.oxygenDeviceName = oxygenDeviceName
        this.oxygenOSVersion = oxygenOSVersion
        this.oxygenOSOTAVersion = oxygenOSOTAVersion
        this.securityPatchDate = securityPatchDate
        this.osType = osType
    }

    @Throws(IOException::class)
    private fun readBuildPropItem(
        itemKey: String,
        buildProperties: String?,
        logText: String?,
    ) = readBuildPropItem(arrayOf(itemKey), buildProperties, logText)

    @Throws(IOException::class)
    private fun readBuildPropItem(
        itemKeys: Array<String>,
        buildProperties: String?,
        logText: String?,
    ): String {
        if (buildProperties.isNullOrEmpty()) {
            return Build.UNKNOWN
        }

        var result = Build.UNKNOWN

        // Some keys are not present on all devices, so check multiple in-order
        itemKeys.forEach { item ->
            val reader = BufferedReader(StringReader(buildProperties))
            var inputLine: String?

            while (reader.readLine().also { inputLine = it } != null) {
                if (inputLine!!.contains(item)) {
                    // Remove brackets ([ and ]) and ":" from the getprop command output line
                    result = inputLine!!.replace("[$item]: ", "")
                        .replace("[", "")
                        .replace("]", "")

                    // Hack #1 (OS_VERSION_NUMBER_LOOKUP_KEY): remove incorrect "H2OS" prefix from version value (OOS 2.0.0 - 3.x)
                    // If this is the case, discard the value and try with the next item in "items" array
                    if (item == RO_ROM_VERSION_LOOKUP_KEY && result.contains(RO_ROM_VERSION_H2OS)) {
                        result = Build.UNKNOWN
                        continue
                    }

                    // Hack #2 (OS_VERSION_NUMBER_LOOKUP_KEY): remove redundant "Oxygen OS " prefix from version value (OnePlus 7-series and newer).
                    // Necessary because the app only shows the number or adds custom formatting
                    if (item == RO_ROM_VERSION_LOOKUP_KEY && result.contains(RO_ROM_VERSION_OXYGENOS_PREFIX)) {
                        result = result.replace(RO_ROM_VERSION_OXYGENOS_PREFIX, "")
                    }

                    // Hack #3 (DEVICE_NAME_LOOKUP_KEY): support for regional device variants (OnePlus 7-series and newer).
                    // OnePlus 7-series and newer devices come in regional variants which cannot be detected by `ro.display.series`.
                    // `ro.product.name` should be read instead of `ro.display.series` to detect the correct device variant.
                    if (item == RO_DISPLAY_SERIES_LOOKUP_KEY && !RO_PRODUCT_NAME_LOOKUP_DEVICES_IGNORE.contains(result)) {
                        val product = Build.PRODUCT
                        logVerbose(TAG, "Detected $result variant: $product")
                        result = product

                        // Hack #4 (BUILD_SOFT_VERSION_LOOKUP_KEY): support for Indian variants in OnePlus 7T-series
                        // OnePlus 7T-series come with Indian builds which cannot be detected by `ro.product.name`.
                        // `ro.build.soft.version` should be read in addition to `ro.product.name` to detect the correct device variant.
                        if (RO_BUILD_SOFT_VERSION_LOOKUP_DEVICES.contains(result)) {
                            // Android Logger class is not loaded during unit tests, so omit logging if called from test.
                            val logMessage = if (logText != null) "Detected $result variant: %s" else null
                            val buildSoftVersion = readBuildPropItem(RO_BUILD_SOFT_VERSION_LOOKUP_KEY, buildProperties, logMessage)

                            // append _IN to mark device as Indian variant
                            if (buildSoftVersion[0] == 'I') {
                                result += "_IN"
                            }
                        }
                    }

                    if (logText != null) {
                        logVerbose(TAG, String.format(logText, result))
                    }

                    // Return the first successfully detected item. This because some keys have multiple values which all exist in the same properties file.
                    return result
                }
            }
        }

        return result
    }
}
