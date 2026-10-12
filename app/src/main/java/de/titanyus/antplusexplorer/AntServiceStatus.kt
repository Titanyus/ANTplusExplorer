package de.titanyus.antplusexplorer

data class AntServiceStatus(
    val packageName: String,
    val label: String,
    val installed: Boolean,
    val enabled: Boolean = false,
    val version: String? = null,
    val versionCode: Long? = null,
) {
    fun summary(): String = if (installed) {
        "$label: ${if (enabled) "installiert, aktiviert" else "installiert, DEAKTIVIERT"} " +
            "(${version ?: "Version unbekannt"}, Code ${versionCode ?: "?"})"
    } else "$label: in diesem Android-Profil nicht gefunden"
}

internal fun dependencyExplanation(label: String, packageName: String, service: AntServiceStatus?): String {
    val dependency = "$label (${packageName.ifBlank { "Paket unbekannt" }})"
    return when {
        service == null -> "ANT-SDK meldet $dependency als fehlend/nicht nutzbar. Genauere Ursache unbekannt; Diagnosebericht kopieren."
        !service.installed -> "$dependency wurde in diesem Android-Profil nicht gefunden. Installation im selben Profil wie ANT+ Explorer prüfen."
        !service.enabled -> "$dependency ist installiert, aber deaktiviert. In den Android-App-Einstellungen aktivieren."
        else -> "$dependency ist installiert und aktiviert (Version ${service.version ?: "unbekannt"}), " +
            "wird vom ANT-SDK aber als fehlend/nicht nutzbar gemeldet. Version und Dienstzugriff müssen geprüft werden. Bitte Diagnosebericht kopieren."
    }
}
