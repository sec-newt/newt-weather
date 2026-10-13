package io.github.secnewt.weather.share

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import io.github.secnewt.weather.data.WeatherStore
import java.time.OffsetDateTime

/**
 * Shares the forecast summary and hourly forecast with apps signed with the Newt key (see
 * [ForecastShare]). Read-only, and guarded by a signature permission in the manifest, so no
 * other app can read it and nobody is ever asked.
 */
class ForecastProvider : ContentProvider() {

    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? {
        val context = context ?: return null
        val forecast = WeatherStore(context).load()
        val at = now()
        return when (uri.lastPathSegment) {
            ForecastShare.PATH_SUMMARY -> MatrixCursor(ForecastShare.SUMMARY_COLUMNS).apply {
                if (forecast != null) addRow(ForecastShare.summaryRow(forecast, at))
            }
            ForecastShare.PATH_HOURLY -> MatrixCursor(ForecastShare.HOURLY_COLUMNS).apply {
                forecast?.let { ForecastShare.hourlyRows(it, at).forEach(::addRow) }
            }
            else -> null
        }.also { it?.setNotificationUri(context.contentResolver, ForecastShare.summaryUri(context.packageName)) }
    }

    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0

    companion object {
        /** The clock, replaceable in tests. */
        internal var now: () -> OffsetDateTime = { OffsetDateTime.now() }
    }
}
