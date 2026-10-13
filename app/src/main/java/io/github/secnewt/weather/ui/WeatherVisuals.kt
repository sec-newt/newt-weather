package io.github.secnewt.weather.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.secnewt.weather.R
import io.github.secnewt.weather.data.Condition
import io.github.secnewt.weather.data.HighLow
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@DrawableRes
fun iconFor(condition: Condition, isDaytime: Boolean): Int = when (condition) {
    Condition.CLEAR -> if (isDaytime) R.drawable.ic_wx_sunny else R.drawable.ic_wx_clear_night
    Condition.PARTLY_CLOUDY -> if (isDaytime) R.drawable.ic_wx_partly_cloudy_day else R.drawable.ic_wx_partly_cloudy_night
    Condition.CLOUDY -> R.drawable.ic_wx_cloud
    Condition.RAIN -> R.drawable.ic_wx_rainy
    Condition.THUNDERSTORM -> R.drawable.ic_wx_thunderstorm
    Condition.SNOW -> R.drawable.ic_wx_weather_snowy
    Condition.FOG -> R.drawable.ic_wx_foggy
    Condition.WIND -> R.drawable.ic_wx_air
}

/** The weather icon. Decorative by default: the words next to it say the same thing. */
@Composable
fun WeatherIcon(
    condition: Condition,
    isDaytime: Boolean,
    size: Dp = 32.dp,
    tint: Color = LocalContentColor.current,
    contentDescription: String? = null,
) {
    Icon(
        painter = painterResource(iconFor(condition, isDaytime)),
        contentDescription = contentDescription,
        tint = tint,
        modifier = Modifier.size(size),
    )
}

/** Warm for highs, cool for lows; all at 7:1 or better on Newt backgrounds and cards. */
object TemperatureColors {
    @Composable
    fun high(): Color = if (isDark()) Color(0xFFFF9E64) else Color(0xFF8F3A00)

    @Composable
    fun low(): Color = if (isDark()) Color(0xFF7DCFFF) else Color(0xFF00597A)

    @Composable
    private fun isDark(): Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f
}

/** "↑ High 90°" / "↓ Low 75°": the arrow and word mean color is never the only signal. */
fun highLowText(highLow: HighLow): String =
    if (highLow.isHigh) "↑ High ${highLow.temperature}°" else "↓ Low ${highLow.temperature}°"

fun highLowSpoken(highLow: HighLow): String =
    if (highLow.isHigh) "High ${highLow.temperature}" else "Low ${highLow.temperature}"

private val TIME = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
private val DAY_TIME = DateTimeFormatter.ofPattern("EEE h:mm a", Locale.US)

fun timeText(at: OffsetDateTime): String = at.format(TIME)

fun dayTimeText(at: OffsetDateTime): String = at.format(DAY_TIME)
