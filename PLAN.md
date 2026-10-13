# Newt Weather — plan

A small weather app using NOAA's free National Weather Service API, built so
[Newt Home](https://github.com/sec-newt/newt-home) can show the weather on the home
screen while staying offline itself. Follows [NEWT_STYLE.md](NEWT_STYLE.md).

## Decided (2026-10-13)

- **Data:** NOAA / National Weather Service (`api.weather.gov`). Free, no key, US only.
  Every request names the app and its repo in the User-Agent, as NOAA asks.
- **Location:** type a ZIP once. It becomes coordinates through a table built into the
  app (Census Bureau ZIP Code Tabulation Areas, public domain), so **nothing is looked
  up online and no location permission is needed**. Change it anytime in the app.
- **Alerts:** shown in the app and on the home screen; a **notification only for
  Warnings** (danger now: tornado, severe thunderstorm, flood, freeze…), not Watches,
  Advisories or Statements. Each warning notifies once.
- **Refresh:** in the background about every 45 minutes when online (NOAA updates
  about hourly). The last forecast is kept, so screens are never blank offline.
- **Permissions:** internet (the only Newt app with it) and notifications. Nothing else
  the app asks for itself; anything added by libraries is listed and explained in
  `config/allowed-permissions.txt`.

## On the home screen (Newt Home, stage 3)

- Next to the date: **current temperature / today's high or tonight's low**, whichever
  NOAA's current forecast period is ("Today" → high, "Tonight" → low), plus an icon.
- High in a warm color, low in a cool one (7:1 contrast), with ↑ / ↓ so color is never
  the only signal.
- Icons: sunny / clear night, partly cloudy, cloudy, rain, thunderstorm, snow / ice,
  fog, wind.
- **Tap the weather:** an hourly strip, scrolled left to right, starting at this hour.
- Active warnings show there too.

## In the app

- Current conditions, active alerts, and the 7-day forecast (day and night periods)
  with more detail: chance of rain, wind, humidity, NOAA's written forecast.
- ZIP setting.

## Stages (each its own pull request)

1. **The app** — NOAA client, ZIP table, background refresh, stored forecast, app
   screens, warning notifications.
2. **Sharing** — a summary (current, high/low, condition, hourly, warnings) readable
   only by apps signed with the Newt key, like Newt Phone's missed-call count.
3. **Newt Home** — reads it and draws it in both looks (terminal and clean).

## How NOAA's data is used

| Need | Request |
|---|---|
| Forecast office + grid for the ZIP's coordinates | `/points/{lat},{lon}` (cached; rarely changes) |
| 7-day (day/night periods) | `properties.forecast` from points |
| Hourly (~156 hours) | `properties.forecastHourly` |
| Current temperature | nearest station's `/observations/latest` (°C → °F); if it's missing or over 2 hours old, this hour's forecast |
| Alerts | `/alerts/active?point={lat},{lon}` |

Condition icons come from the code in NOAA's icon link (`.../land/day/tsra_hi,20` →
`tsra_hi`); if NOAA ever changes that format, the short forecast text is used instead.
