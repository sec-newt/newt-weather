# Newt Weather UI refresh Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans.

**Goal:** Mockup 9: alerts as one-line stripe rows, the new in-app **Warning alerts** ON/OFF setting, and the 7-day forecast on the first screen. Uses newt-ui 2.0.1.

**Spec:** the design's Weather row and its "Weather, warning alerts" feature change:
- ON asks for notification permission if it's needed, with the explanation shown at that moment.
- OFF stops warning notifications while the permission stays granted.
- The switch always flips and announces its state.
- Default: ON if the permission is already granted, otherwise OFF.

## Review Focus

1. **Turned ON but Android's permission is denied:** the switch stays ON and the row says notifications are blocked, with a way to fix it. Nothing pretends warnings will arrive. (Task 2 test.)
2. **Turned OFF while permission is granted:** no warning notification is posted. (`WarningAlerts.shouldNotify` test.)
3. **Never touched:** it follows the permission (default). (Unit test.)
4. **No alerts:** no alerts heading or empty rows. (Existing evening test.)
5. **200% text:** the day rows wrap and don't clip. (200% screenshot.)

### Task 1: newt-ui 2.0.1 (bump; all 45 tests pass)

### Task 2: Warning alerts setting
- `object WarningAlerts { fun switchOn(stored: Boolean?, granted: Boolean): Boolean; fun shouldNotify(stored: Boolean?, granted: Boolean): Boolean }`
- `WeatherStore.warningAlerts: Boolean?`; `WarningNotifier` checks `shouldNotify`.
- `WarningAlertsCard(on, blocked, onChange, onOpenSettings)`:
  - A bell, "Warning alerts", the hint "Tornado · storm · flood · freeze", and a `NewtSwitch`.
  - When it's ON and blocked: "Android has notifications off for Newt Weather" plus a "Fix in settings" row.
  - Turning it ON while it's not granted shows "Android will ask to allow notifications…" and then asks.

### Task 3: Alerts as stripe rows; the 7-day forecast as one card of compact rows; the Warning alerts card sits between them.
