# Newt Weather

A private weather app for Android, part of the Newt family (built from
[newt-android-template](https://github.com/sec-newt/newt-android-template)), using the
National Weather Service's free forecast service. Large text, high contrast, and words
next to every icon, following the [house style](NEWT_STYLE.md).

> **A personal project, in early development.** US only (NOAA's coverage).

## What it does

- **Now:** the temperature from the nearest weather station, today's high or tonight's low
  (↑ warm / ↓ cool colors), and the conditions with an icon.
- **Places:** type a city ("Paris, TX") or a ZIP. Save as many places as you like in the
  **Places** card at the bottom, and tap one to show its forecast. If a name fits more than one
  town (Springfield), you pick from a list. The place showing is the one Newt Home shows and
  the one warnings come from.
- **Alerts:** NOAA watches, warnings and advisories for the place showing, with what to do. A
  notification only for **Warnings**, and only if you turn it on.
- **Next 7 days:** each day's high, low, chance of rain and icon; tap a day for NOAA's
  full written forecast and the wind.
- Refreshes about every 45 minutes in the background, and keeps the last forecast so it
  still shows offline.

## Privacy

- **Your places stay on the phone.** A city or ZIP is turned into map coordinates using
  Census Bureau tables built into the app, so no location permission and no lookup service. Only those
  coordinates go to the National Weather Service, to get the forecast.
- The only place the app connects to is `api.weather.gov`. No ads, analytics, Google Play
  Services or accounts.
- **Sharing with Newt Home:** temperatures, conditions and active warnings (not the ZIP or
  place) are readable only by apps signed with the Newt key, through a signature permission
  nobody is ever asked for. Newt Home shows the weather without internet of its own. The
  shared format is documented in `share/ForecastShare.kt`.

See [PLAN.md](PLAN.md) for the plan and [SECURITY.md](SECURITY.md) for security details.

## Install

Releases are built and signed by GitHub Actions. Install and update with Obtainium from
this repository's Releases page.
