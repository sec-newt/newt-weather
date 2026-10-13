package io.github.secnewt.weather.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import io.github.secnewt.weather.alerts.WarningNotifier
import io.github.secnewt.weather.data.RefreshException
import io.github.secnewt.weather.data.WeatherRepository
import io.github.secnewt.weather.data.WeatherStore
import java.util.concurrent.TimeUnit

/** Refreshes the forecast in the background and notifies about new warnings. */
class RefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (WeatherStore(applicationContext).zip == null) return Result.success()
        return try {
            val forecast = WeatherRepository(applicationContext).refresh()
            WarningNotifier(applicationContext).notifyNew(forecast.alerts)
            Result.success()
        } catch (e: RefreshException) {
            // The next scheduled run tries again; no need to hammer NOAA.
            Result.success()
        }
    }

    companion object {
        private const val NAME = "refresh"

        /** About every 45 minutes when online; NOAA updates forecasts about hourly. */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<RefreshWorker>(45, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
