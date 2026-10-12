package de.titanyus.antplusexplorer

import android.content.Context
import android.content.pm.PackageManager
import com.dsi.ant.plugins.antplus.pccbase.AntPluginPcc

internal object AntServices {
    const val RADIO = "com.dsi.ant.service.socket"
    const val PLUGINS = "com.dsi.ant.plugins.antplus"

    fun read(context: Context, packageName: String, label: String): AntServiceStatus {
        return try {
            val manager = context.packageManager
            val info = manager.getPackageInfo(packageName,
                PackageManager.PackageInfoFlags.of(PackageManager.MATCH_DISABLED_COMPONENTS.toLong()))
            val app = manager.getApplicationInfo(packageName,
                PackageManager.ApplicationInfoFlags.of(PackageManager.MATCH_DISABLED_COMPONENTS.toLong()))
            AntServiceStatus(packageName, label, installed = true, enabled = app.enabled,
                version = info.versionName, versionCode = info.longVersionCode)
        } catch (_: PackageManager.NameNotFoundException) {
            AntServiceStatus(packageName, label, installed = false)
        }
    }

    fun summary(context: Context): String {
        val code = AntPluginPcc.getInstalledPluginsVersionNumber(context)
        val sdk = when (code) {
            -1 -> "ANT-SDK: Plugins-Paket nicht gefunden"
            -2 -> "ANT-SDK: Plugins-Paket deaktiviert"
            else -> "ANT-SDK: Plugins erkannt, Versionscode $code"
        }
        return "${read(context, RADIO, "ANT Radio Service").summary()}\n" +
            "${read(context, PLUGINS, "ANT+ Plugins Service").summary()}\n$sdk"
    }

    fun missingDependency(context: Context): String {
        val packageName = AntPluginPcc.getMissingDependencyPackageName().orEmpty()
        val label = AntPluginPcc.getMissingDependencyName()?.takeIf { it.isNotBlank() } ?: "ANT-Abhängigkeit"
        val service = packageName.takeIf { it.isNotBlank() }?.let { read(context, it, label) }
        return dependencyExplanation(label, packageName, service)
    }
}
