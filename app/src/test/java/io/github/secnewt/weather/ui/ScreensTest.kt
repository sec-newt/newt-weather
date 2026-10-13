package io.github.secnewt.weather.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import io.github.secnewt.test.newtScreenshot
import io.github.secnewt.weather.data.Fixtures
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.ZoneId

/** Screenshots (light, dark, 200% text) and accessibility checks for every screen. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = RobolectricDeviceQualifiers.Pixel7)
class ScreensTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val zone = ZoneId.of("America/Chicago")

    private fun weather(dark: Boolean, name: String, fontScale: Float = 1f, alerts: Boolean = true, evening: Boolean = false) =
        composeRule.newtScreenshot(name, dark = dark, fontScale = fontScale) {
            WeatherScreen(
                forecast = Fixtures.forecast(alerts),
                now = if (evening) Fixtures.EVENING else Fixtures.NOON,
                zone = zone,
                refreshing = false,
                status = null,
                notificationsAllowed = false,
                onRefresh = {},
                onAllowNotifications = {},
                onNotificationSettings = {},
                editingZip = false,
                zipError = null,
                onEditZip = {},
                onSaveZip = {},
                onCancelZip = {},
            )
        }

    @Test
    fun weatherDark() {
        weather(dark = true, name = "weather-dark")
        // The Now card reads as one sentence to a screen reader, so that's what is checked.
        composeRule.onNodeWithContentDescription("Now 82 degrees, mostly sunny. High 90.").assertIsDisplayed()
        composeRule.onNodeWithText("Flood Warning").assertIsDisplayed()
    }

    @Test
    fun weatherLight() = weather(dark = false, name = "weather-light")

    @Test
    fun weatherLargeText() = weather(dark = true, name = "weather-200pct", fontScale = 2f)

    @Test
    fun weatherEveningShowsTheLow() {
        weather(dark = true, name = "weather-evening", alerts = false, evening = true)
        // The Now card (not today's row, which also says it) ends with tonight's low.
        composeRule.onNode(hasContentDescription("Now ", substring = true) and hasContentDescription("Low 75.", substring = true))
            .assertIsDisplayed()
    }

    @Test
    fun zipFirstTimeDark() = composeRule.newtScreenshot("zip-dark", dark = true) {
        ZipScreen(initial = "", error = null, onSave = {}, onCancel = null)
    }

    @Test
    fun zipChangeLight() = composeRule.newtScreenshot("zip-light", dark = false) {
        ZipScreen(initial = "73102", error = null, onSave = {}, onCancel = {})
    }

    @Test
    fun zipUnknownLargeText() = composeRule.newtScreenshot("zip-error-200pct", dark = true, fontScale = 2f) {
        ZipScreen(initial = "00000", error = "That ZIP isn't one we know. Check the five digits.", onSave = {}, onCancel = {})
    }

    private fun zipCard(name: String, dark: Boolean, editing: Boolean, error: String? = null, fontScale: Float = 1f) =
        composeRule.newtScreenshot(name, dark = dark, fontScale = fontScale) {
            ZipCard(
                place = "Oklahoma City, OK", zip = "73102", editing = editing, error = error,
                onEdit = {}, onSave = {}, onCancel = {},
            )
        }

    @Test
    fun zipCardClosedDark() {
        zipCard("zip-card-dark", dark = true, editing = false)
        composeRule.onNodeWithText("Forecast for Oklahoma City, OK · ZIP 73102").assertIsDisplayed()
        composeRule.onNodeWithText("Change ZIP").assertIsDisplayed()
    }

    @Test
    fun zipCardOpenLight() {
        zipCard("zip-card-open-light", dark = false, editing = true)
        composeRule.onNodeWithText("Save").assertIsDisplayed()
        composeRule.onNodeWithText("Cancel").assertIsDisplayed()
    }

    @Test
    fun zipCardErrorLargeText() =
        zipCard("zip-card-error-200pct", dark = true, editing = true, error = "That ZIP isn't one we know. Check the five digits.", fontScale = 2f)

    @Test
    fun warningsOffDark() {
        composeRule.newtScreenshot("warnings-off-dark", dark = true) { NotificationsCard(false, {}, {}) }
        composeRule.onNodeWithText("Turn on warnings").assertIsDisplayed()
    }

    @Test
    fun warningsOnLight() {
        composeRule.newtScreenshot("warnings-on-light", dark = false) { NotificationsCard(true, {}, {}) }
        composeRule.onNodeWithText("Notification settings").assertIsDisplayed()
    }

    @Test
    fun warningsOffLargeText() =
        composeRule.newtScreenshot("warnings-off-200pct", dark = true, fontScale = 2f) { NotificationsCard(false, {}, {}) }

    @Test
    fun loadingFailedDark() = composeRule.newtScreenshot("loading-failed-dark", dark = true) {
        LoadingScreen(status = "Couldn't reach the National Weather Service.", refreshing = false, onRetry = {}, onChangeZip = {})
    }
}
