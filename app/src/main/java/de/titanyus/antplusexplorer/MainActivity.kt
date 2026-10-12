package de.titanyus.antplusexplorer

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// This diagnostic prototype deliberately has a German-only UI and report.
@android.annotation.SuppressLint("SetTextI18n")
class MainActivity : Activity() {
    private lateinit var client: AntTrainerClient
    private lateinit var profile: Spinner
    private lateinit var connect: Button
    private lateinit var disconnect: Button
    private lateinit var status: TextView
    private lateinit var reception: TextView
    private lateinit var device: TextView
    private lateinit var values: TextView
    private lateinit var logView: TextView
    private lateinit var services: String
    private lateinit var servicesView: TextView
    private val lines = ArrayDeque<String>()
    private val handler = Handler(Looper.getMainLooper())
    private val ticker = object : Runnable {
        override fun run() {
            render(client.snapshot)
            handler.postDelayed(this, 1_000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(16), dp(20), dp(24))
        }
        val scroll = ScrollView(this).apply { addView(content); isFillViewport = true }
        scroll.setOnApplyWindowInsetsListener { view, insets ->
            val bars = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        setContentView(scroll)
        scroll.requestApplyInsets()

        fun text(message: String, size: Float = 16f): TextView = TextView(this).apply {
            text = message
            textSize = size
            setPadding(0, dp(6), 0, dp(6))
            content.addView(this)
        }
        text("ANT+ Explorer", 28f)
        text("Matrix ICR50 · Verbindungstest", 18f)
        text("Trainer aktivieren und treten. Profil wählen, Suche starten und das passende Gerät im ANT+-Dialog auswählen.")
        services = AntServices.summary(this)
        servicesView = text(services, 14f)
        for ((packageName, label) in listOf(AntServices.RADIO to "ANT Radio", AntServices.PLUGINS to "ANT+ Plugins")) {
            Button(this).apply {
                this.text = "$label · App-Einstellungen"
                content.addView(this)
                setOnClickListener {
                    try {
                        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
                    } catch (_: ActivityNotFoundException) {
                        Toast.makeText(this@MainActivity, "App-Einstellungen nicht verfügbar", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        profile = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item,
                AntProfile.entries.map { it.label })
            setSelection(savedInstanceState?.getInt("profile") ?: 0)
            content.addView(this)
        }
        connect = Button(this).apply {
            text = "Trainer suchen / verbinden"
            content.addView(this)
            setOnClickListener { client.connect(AntProfile.entries[profile.selectedItemPosition]) }
        }
        disconnect = Button(this).apply {
            text = "Trennen / Suche abbrechen"
            content.addView(this)
            setOnClickListener { client.disconnect() }
        }
        status = text("", 18f)
        device = text("")
        reception = text("")
        values = text("", 22f)
        text("— = nicht empfangen oder vom Gerät als ungültig gemeldet. Fahrradleistung liefert hier nur Watt und Trittfrequenz.", 14f)
        text("Falls kein Gerät erscheint: ANT+-Übertragung an der Konsole prüfen, andere Trainingsapps schließen und das zweite Profil testen. Installierte Dienste allein belegen noch keine ANT+-Hardware oder Trainer-Kompatibilität.", 14f)
        Button(this).apply {
            text = "Testbericht kopieren"
            content.addView(this)
            setOnClickListener {
                val report = "ANT+ Explorer 0.1.1 · Matrix ICR50\nAndroid ${android.os.Build.VERSION.RELEASE}\n" +
                    "Hersteller/Modell: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}\n$services\n" +
                    "Profil: ${client.snapshot.profile.label}\n${status.text}\n${device.text}\n" +
                    "${reception.text}\n${values.text}\n\n${logView.text}"
                getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("ANT+-Test", report))
                Toast.makeText(this@MainActivity, "Testbericht kopiert", Toast.LENGTH_SHORT).show()
            }
        }
        text("Diagnose", 18f)
        logView = text("", 12f).apply { setTextIsSelectable(true) }
        client = AntTrainerClient(this, ::render, ::log)
        log("App gestartet · Verbindung wird beim Verlassen beendet; Drehung erfordert eine neue Suche")
        render(client.snapshot)
    }

    private fun render(state: TrainerSnapshot) {
        connect.isEnabled = !state.active
        profile.isEnabled = !state.active
        disconnect.isEnabled = state.active
        status.text = state.status
        device.text = "${state.device}\nGerätetyp: ${state.equipment}"
        val now = SystemClock.elapsedRealtime()
        reception.text = state.reception(now)
        val fresh = state.active && state.tracking && state.lastDataAt?.let { now - it <= TrainerSnapshot.STALE_AFTER_MS } == true
        reception.setTextColor(if (fresh) Color.rgb(0, 110, 55) else Color.DKGRAY)
        values.text = "Leistung: ${metric(state.powerWatts, "W")}\n" +
            "Trittfrequenz: ${metric(state.cadenceRpm, "U/min")}\n" +
            "Geschwindigkeit: ${metric(state.speedKmh, "km/h", 1)}\n" +
            "Distanz: ${metric(state.distanceMeters?.toDouble()?.div(1000), "km", 2)}\n" +
            "Puls: ${metric(state.heartRateBpm, "bpm")}\n" +
            "Trainer-Zeit: ${metric(state.elapsedSeconds, "s")}"
        if (state.active) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun log(message: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.GERMANY).format(Date())
        lines.addLast("$time  $message")
        while (lines.size > 40) lines.removeFirst()
        logView.text = lines.joinToString("\n")
    }

    override fun onStart() {
        super.onStart()
        handler.post(ticker)
    }

    override fun onResume() {
        super.onResume()
        services = AntServices.summary(this)
        servicesView.text = services
    }

    override fun onStop() {
        handler.removeCallbacks(ticker)
        // The ANT+ picker is an external Activity: keep its pending request alive.
        if (!client.snapshot.searching && client.snapshot.active) client.disconnect("App verlassen · Verbindung getrennt")
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("profile", profile.selectedItemPosition)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        handler.removeCallbacks(ticker)
        client.disconnect("Activity beendet · ANT+-Kanal freigegeben")
        super.onDestroy()
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
