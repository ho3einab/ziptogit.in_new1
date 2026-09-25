# Mini Capsule — v0.1.0-alpha (Nothing Phone (2a))

A native "Dynamic Island"-style capsule for Nothing OS, built to hug the
Phone (2a)'s centered punch-hole camera. This is a full Android Studio
project, not a pre-built APK — see **Why no APK** below.

## What's in this build (v1)

- 🎵 Music playback control + now-playing label (reads the active
  `MediaSession`, works with Spotify/YT Music/anything that publishes one)
- ⏱️ Countdown timer with a live progress ring on the pill
- 🔋 Battery status — only surfaces when charging starts or the level drops
  below a low-battery threshold, not shown all the time
- 📞 Incoming call banner
- 🔔 Selected notifications — pick which apps are allowed to show up, via
  "Choose notification apps" on the home screen
- Tap the pill to expand into a full control panel; swipe down to dismiss a
  transient item (notification/battery) early
- Minimal black-pill look matching Nothing OS, single red accent

## Project layout

```
NothingCapsule/
├── app/src/main/java/com/nothingcapsule/app/
│   ├── MainActivity.kt            entry screen: on/off toggle, links out
│   ├── PermissionActivity.kt      walks through the 4 permissions needed
│   ├── AppPickerActivity.kt       notification whitelist picker
│   ├── CapsuleApplication.kt      holds the shared CapsuleStateManager
│   ├── CapsulePrefs.kt            tiny SharedPreferences wrapper
│   ├── manager/                   one class per data source (music, timer,
│   │                              battery), plus CapsuleStateManager which
│   │                              merges them by priority
│   ├── service/
│   │   ├── CapsuleOverlayService.kt      the WindowManager overlay itself
│   │   └── CapsuleNotificationListener.kt
│   ├── receiver/                  CallReceiver, BootReceiver
│   ├── model/CapsuleState.kt      the CapsuleContent sealed class + priority order
│   └── view/CapsuleView.kt        custom view: collapsed pill / expanded panel
└── app/src/main/res/              layouts, vector icons, colors, strings
```

## Opening the project

1. Android Studio (Koala/2024.1 or newer) → **Open** → select the
   `NothingCapsule/` folder.
2. Let Gradle sync. The wrapper jar isn't checked in (see below) —
   Android Studio will offer to generate it, or run `gradle wrapper` once
   from a machine with Gradle installed.
3. Run on a Nothing Phone (2a), or any device/emulator running Android 10+
   (minSdk 29; targets/compiles against 35).

## Why no APK

This conversation runs in a sandbox without the Android SDK, NDK, or Gradle
distribution available — there's no compiler here to actually produce and
sign an installable `.apk`. What you're getting is the complete, real source
tree; opening it in Android Studio and pressing Run is the remaining step,
and that step needs a real Android toolchain.

## Permissions this app asks for, and why

| Permission | Why |
|---|---|
| `SYSTEM_ALERT_WINDOW` | Draw the capsule on top of every other app |
| `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_SPECIAL_USE` | Keep the overlay alive; Android 14+ requires every foreground service to declare a type, and "persistent system-wide UI" is the `specialUse` case |
| `POST_NOTIFICATIONS` | Android 13+ requires this just to show the mandatory low-priority "capsule is running" notification that foreground services must have |
| `READ_PHONE_STATE` | Detect an incoming call to show the call banner |
| Notification-listener access (granted via system settings, not a manifest permission) | Needed for two things at once: reading whitelisted-app notifications, **and** unlocking `MediaSessionManager.getActiveSessions()` — Android only gives that to apps that are also a bound `NotificationListenerService` |
| `RECEIVE_BOOT_COMPLETED` | Restart the overlay after a reboot, only if you had it turned on |

## Known constraints to test for on a real device

These are the two things flagged before building this out, plus a couple
more that surfaced while writing the overlay logic — none of them are
guaranteed to behave a specific way without a real Nothing Phone (2a) in
hand:

- **Touch-through on Android 12+.** `FLAG_NOT_TOUCH_MODAL` is set so taps
  outside the pill fall through to the app underneath, but some OEM skins
  are stricter about this above Android 12. Nothing OS is close to stock
  AOSP so it should be fine, but this needs an on-device check.
- **Android 15 foreground-service rules.** The `specialUse` type plus the
  `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` manifest property is the current
  correct approach, but Play Store review for `specialUse` foreground
  services has gotten stricter — if this is ever submitted to the Play
  Store rather than sideloaded, expect back-and-forth over the declared
  use-case string.
- **Exact cutout position.** The pill reads the real
  `WindowInsets.displayCutout` at runtime rather than a hardcoded offset
  (Nothing hasn't published fixed coordinates, and it'd break across
  screen resolutions/densities anyway) — but the vertical centering math in
  `CapsuleOverlayService.positionAroundCutout()` has only been written
  against the API, not measured against a physical (2a) yet.
- **Media session access can lag on first launch.** Notification-listener
  access has to be granted, the service has to actually rebind, and only
  then does `MusicManager` get a callback to start watching sessions —
  there can be a few seconds' delay right after granting the permission.

## Roadmap ideas for v1.1+

- Smooth expand/collapse animation (spring physics) instead of a visibility
  toggle
- Haptic pulse when a timer finishes
- Long-press pill for quick actions (flashlight, DND toggle)
- Settings screen for the low-battery threshold (currently hardcoded default
  of 20%, changeable only via `CapsulePrefs.lowBatteryThreshold` in code)
- Home-screen widget alternative for users who don't want to grant
  `SYSTEM_ALERT_WINDOW`
