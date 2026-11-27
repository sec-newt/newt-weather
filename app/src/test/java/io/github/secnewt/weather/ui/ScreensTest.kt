package io.github.secnewt.weather.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import io.github.secnewt.test.newtScreenshot
import io.github.secnewt.ui.NewtLook
import io.github.secnewt.ui.NewtLookTheme
import io.github.secnewt.weather.data.Coordinates
import io.github.secnewt.weather.data.Fixtures
import io.github.secnewt.weather.data.Location
import io.github.secnewt.weather.data.PlaceFinder
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.performClick
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
                warningAlertsOn = false,
                notificationsBlocked = false,
                onRefresh = {},
                onWarningAlertsChange = {},
                onNotificationSettings = {},
                addingPlace = false,
                places = { Places() },
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
    fun placeFirstTimeDark() = composeRule.newtScreenshot("place-dark", dark = true) {
        PlaceScreen(PlaceSearch(), onSearch = {}, onPick = {}, onTyped = {})
    }

    @Test
    fun placeChoicesLight() {
        composeRule.newtScreenshot("place-choices-light", dark = false) {
            PlaceScreen(PlaceSearch(choices = springfields), onSearch = {}, onPick = {}, onTyped = {})
        }
        composeRule.onNodeWithText("Which one?").assertIsDisplayed()
        composeRule.onNodeWithText("Springfield, IL").assertIsDisplayed()
    }

    @Test
    fun placeUnknownLargeText() = composeRule.newtScreenshot("place-error-200pct", dark = true, fontScale = 2f) {
        PlaceScreen(PlaceSearch(error = PlaceFinder.unknownCity("Gotham")), onSearch = {}, onPick = {}, onTyped = {})
    }

    @Test
    fun placeSearchSendsWhatWasTyped() {
        var searched: String? = null
        composeRule.setContent {
            io.github.secnewt.ui.NewtTheme(darkTheme = true) {
                PlaceScreen(PlaceSearch(), onSearch = { searched = it }, onPick = {}, onTyped = {})
            }
        }
        composeRule.onNodeWithText("City or ZIP code").performTextInput("Paris, TX")
        composeRule.onNodeWithText("Show the forecast").performClick()
        org.junit.Assert.assertEquals("Paris, TX", searched)
    }

    private val paris = Location("Paris, TX", null, Coordinates("33.67", "-95.55"))
    private val springfields = listOf("IL" to "39.79", "MA" to "42.12", "MO" to "37.19")
        .map { (state, lat) -> Location("Springfield, $state", null, Coordinates(lat, "-90")) }

    @androidx.compose.runtime.Composable
    private fun Places(
        locations: List<Location> = listOf(Fixtures.OKC),
        active: Location = Fixtures.OKC,
        adding: Boolean = false,
        search: PlaceSearch = PlaceSearch(),
        onShow: (Location) -> Unit = {},
        onRemove: (Location) -> Unit = {},
    ) = PlacesCard(
        locations = locations, active = active, adding = adding, search = search,
        onShow = onShow, onRemove = onRemove, onAdd = {}, onSearch = {}, onPick = {}, onTyped = {}, onCancel = {},
    )

    @Test
    fun placesOneDark() {
        composeRule.newtScreenshot("places-one-dark", dark = true) { Places() }
        composeRule.onNodeWithText("Oklahoma City, OK").assertIsDisplayed()
        composeRule.onNodeWithText("SHOWING").assertIsDisplayed()
        // The only place can't be removed: there'd be nothing to show.
        composeRule.onNodeWithContentDescription("Remove", substring = true).assertDoesNotExist()
        composeRule.onNodeWithText("Add a place").assertIsDisplayed()
    }

    @Test
    fun placesTwoLight() = composeRule.newtScreenshot("places-two-light", dark = false) {
        Places(locations = listOf(Fixtures.OKC, paris))
    }

    @Test
    fun placesTwoLargeText() = composeRule.newtScreenshot("places-two-200pct", dark = true, fontScale = 2f) {
        Places(locations = listOf(Fixtures.OKC, paris))
    }

    @Test
    fun placesAddingDark() = composeRule.newtScreenshot("places-adding-dark", dark = true) {
        Places(adding = true, search = PlaceSearch(error = PlaceFinder.UNKNOWN_ZIP))
    }

    @Test
    fun placesTapToShowAndRemove() {
        var shown: Location? = null
        var removed: Location? = null
        composeRule.setContent {
            io.github.secnewt.ui.NewtTheme(darkTheme = true) {
                Places(locations = listOf(Fixtures.OKC, paris), onShow = { shown = it }, onRemove = { removed = it })
            }
        }
        composeRule.onNodeWithText("Paris, TX").performClick()
        org.junit.Assert.assertEquals(paris, shown)
        // Each Remove names its place, so no two read out the same.
        composeRule.onNodeWithContentDescription("Remove Oklahoma City, OK · ZIP 73102").performClick()
        org.junit.Assert.assertEquals(Fixtures.OKC, removed)
        composeRule.onNodeWithContentDescription("Remove Paris, TX").assertExists()
    }

    @Test
    fun warningsOffDark() {
        composeRule.newtScreenshot("warnings-off-dark", dark = true) { WarningAlertsCard(on = false, blocked = false, onChange = {}, onOpenSettings = {}) }
        composeRule.onNodeWithText("Warning alerts").assertIsOff()
    }

    @Test
    fun warningsOnLight() {
        composeRule.newtScreenshot("warnings-on-light", dark = false) { WarningAlertsCard(on = true, blocked = false, onChange = {}, onOpenSettings = {}) }
        composeRule.onNodeWithText("Warning alerts").assertIsOn()
    }

    @Test
    fun warningsOffLargeText() =
        composeRule.newtScreenshot("warnings-off-200pct", dark = true, fontScale = 2f) { WarningAlertsCard(on = false, blocked = false, onChange = {}, onOpenSettings = {}) }

    // The terminal look, as when Newt Home is set to `look terminal`.
    @Test
    fun terminalWeatherDark() = composeRule.newtScreenshot("terminal-weather-dark", dark = true) {
        NewtLookTheme(NewtLook.TERMINAL) { WeatherNow() }
    }

    @Test
    fun terminalWeatherLight() = composeRule.newtScreenshot("terminal-weather-light", dark = false) {
        NewtLookTheme(NewtLook.TERMINAL) { WeatherNow() }
    }

    @Test
    fun terminalWeatherLargeText() = composeRule.newtScreenshot("terminal-weather-200pct", dark = true, fontScale = 2f) {
        NewtLookTheme(NewtLook.TERMINAL) { WeatherNow() }
    }

    @Test
    fun terminalPlaceDark() = composeRule.newtScreenshot("terminal-place-dark", dark = true) {
        NewtLookTheme(NewtLook.TERMINAL) { PlaceScreen(PlaceSearch(), onSearch = {}, onPick = {}, onTyped = {}) }
    }

    @androidx.compose.runtime.Composable
    private fun WeatherNow() = WeatherScreen(
        forecast = Fixtures.forecast(),
        now = Fixtures.NOON,
        zone = zone,
        refreshing = false,
        status = null,
        warningAlertsOn = false,
        notificationsBlocked = false,
        onRefresh = {},
        onWarningAlertsChange = {},
        onNotificationSettings = {},
        addingPlace = false,
        places = { Places() },
    )

    @Test
    fun loadingFailedDark() = composeRule.newtScreenshot("loading-failed-dark", dark = true) {
        LoadingScreen(
            placeName = "Paris, TX", status = "Couldn't reach the National Weather Service.",
            refreshing = false, onRetry = {}, places = { Places(locations = listOf(Fixtures.OKC, paris), active = paris) },
        )
    }

    @Test
    fun warningAlertsSwitchFlips() {
        var changed: Boolean? = null
        composeRule.setContent { io.github.secnewt.ui.NewtTheme(darkTheme = true) { WarningAlertsCard(on = false, blocked = false, onChange = { changed = it }, onOpenSettings = {}) } }
        composeRule.onNodeWithText("Warning alerts").performClick()
        org.junit.Assert.assertEquals(true, changed)
    }

    @Test
    fun warningAlertsOnButBlockedSaysSoAndOffersAFix() {
        var opened = 0
        composeRule.newtScreenshot("warnings-blocked-dark", dark = true) {
            WarningAlertsCard(on = true, blocked = true, onChange = {}, onOpenSettings = { opened++ })
        }
        composeRule.onNodeWithText("Android has notifications off for Newt Weather").assertExists()
        composeRule.onNodeWithText("Fix in settings").performClick()
        org.junit.Assert.assertEquals(1, opened)
    }

    @Test
    fun alertsThenWarningAlertsThenTheForecast() {
        composeRule.setContent { io.github.secnewt.ui.NewtTheme(darkTheme = true) { WeatherNow() } }
        val alert = composeRule.onNodeWithText("Flood Warning").fetchSemanticsNode().boundsInRoot
        val setting = composeRule.onNodeWithText("Warning alerts").fetchSemanticsNode().boundsInRoot
        org.junit.Assert.assertTrue(setting.top > alert.bottom)
        composeRule.onNodeWithText("Next 7 days").assertExists()
    }
}
