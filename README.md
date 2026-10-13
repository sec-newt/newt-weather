# Newt Weather

A private weather app for Android, part of the Newt family (built from
[newt-android-template](https://github.com/sec-newt/newt-android-template)), using the
National Weather Service's free forecast service. Large text, high contrast, and words
next to every icon, following the [house style](NEWT_STYLE.md).

> **A personal project, in early development.** US only (NOAA's coverage).

## What it does

- **Now:** the temperature from the nearest weather station, today's high or tonight's low
  (↑ warm / ↓ cool colors), and the conditions with an icon.
- **Alerts:** NOAA watches, warnings and advisories for your ZIP, with what to do. A
  notification only for **Warnings**, and only if you turn it on.
- **Next 7 days:** each day's high, low, chance of rain and icon; tap a day for NOAA's
  full written forecast and the wind.
- Refreshes about every 45 minutes in the background, and keeps the last forecast so it
  still shows offline.

## Privacy

- **Your ZIP stays on the phone.** It's turned into map coordinates using a Census Bureau
  table built into the app, so no location permission and no lookup service. Only those
  coordinates go to the National Weather Service, to get the forecast.
- The only place the app connects to is `api.weather.gov`. No ads, analytics, Google Play
  Services or accounts.
- Later, Newt Home will show the forecast on the home screen without internet of its own:
  Newt Weather will share it only with apps signed with the Newt key.

See [PLAN.md](PLAN.md) for the plan and [SECURITY.md](SECURITY.md) for security details.

## Install

Releases are built and signed by GitHub Actions. Install and update with Obtainium from
this repository's Releases page.
