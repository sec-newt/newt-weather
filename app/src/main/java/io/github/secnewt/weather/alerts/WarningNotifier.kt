package io.github.secnewt.weather.alerts

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import io.github.secnewt.weather.MainActivity
import io.github.secnewt.weather.R
import io.github.secnewt.weather.data.Forecast
import io.github.secnewt.weather.data.WeatherStore

/**
 * One notification per NOAA Warning (danger now), the first time it's seen. Watches,
 * Advisories and Statements only show in the app and on the home screen.
 */
class WarningNotifier(private val context: Context, private val store: WeatherStore = WeatherStore(context)) {

    /** Notifies about the forecast's new Warnings, named with its place ("Tornado Warning · Paris, TX"). */
    fun notifyNew(forecast: Forecast) {
        val warnings = forecast.alerts.filter { it.isWarning }
        val place = forecast.location
        val already = store.notifiedWarnings(place)
        // Forget warnings NOAA has dropped, so the list doesn't grow forever.
        store.saveNotifiedWarnings(place, already.intersect(warnings.map { it.id }.toSet()))
        val fresh = warnings.filter { it.id !in already }
        if (fresh.isEmpty() || !WarningAlerts.shouldNotify(store.warningAlerts, allowed(context))) return
        createChannel(context)
        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val manager = NotificationManagerCompat.from(context)
        fresh.forEach { alert ->
            val notification = NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_wx_warning)
                .setContentTitle("${alert.event} · ${forecast.location.label}")
                .setContentText(alert.headline)
                .setStyle(NotificationCompat.BigTextStyle().bigText(alert.instruction.ifBlank { alert.headline }))
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(open)
                .setAutoCancel(true)
                .build()
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED
            ) {
                manager.notify(alert.id.hashCode(), notification)
            }
        }
        store.saveNotifiedWarnings(place, store.notifiedWarnings(place) + fresh.map { it.id })
    }

    companion object {
        const val CHANNEL = "warnings"

        fun allowed(context: Context): Boolean =
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED

        fun createChannel(context: Context) {
            val channel = NotificationChannel(CHANNEL, "Severe weather warnings", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Tornado, severe thunderstorm, flood and other NOAA warnings for the place showing in the app."
            }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}
