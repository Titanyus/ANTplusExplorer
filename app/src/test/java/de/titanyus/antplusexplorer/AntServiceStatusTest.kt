package de.titanyus.antplusexplorer

import org.junit.Assert.*
import org.junit.Test

class AntServiceStatusTest {
    private val radio = AntServiceStatus("com.dsi.ant.service.socket", "ANT Radio Service", true, true, "4.18.0", 41800)

    @Test fun installedEnabledDependencyIsNotReportedAsUninstalled() {
        val message = dependencyExplanation(radio.label, radio.packageName, radio)
        assertTrue(message.contains("installiert und aktiviert"))
        assertTrue(message.contains("4.18.0"))
        assertTrue(message.contains(radio.packageName))
        assertFalse(message.contains("Installation im selben Profil"))
    }

    @Test fun disabledDependencyRequestsActivationInsteadOfInstallation() {
        val message = dependencyExplanation(radio.label, radio.packageName, radio.copy(enabled = false))
        assertTrue(message.contains("deaktiviert"))
        assertTrue(message.contains("aktivieren"))
        assertFalse(message.contains("Installation im selben Profil"))
    }

    @Test fun missingRadioIsIdentifiedWithoutBlamingPlugins() {
        val message = dependencyExplanation(radio.label, radio.packageName, radio.copy(installed = false))
        assertTrue(message.contains("ANT Radio Service"))
        assertTrue(message.contains("in diesem Android-Profil nicht gefunden"))
        assertFalse(message.contains("ANT+ Plugins Service"))
    }

    @Test fun unknownDependencyDoesNotInventMissingPackages() {
        val message = dependencyExplanation("ANT-Abhängigkeit", "", null)
        assertTrue(message.contains("Genauere Ursache unbekannt"))
        assertFalse(message.contains("ANT Radio Service"))
        assertFalse(message.contains("ANT+ Plugins Service"))
    }
}
