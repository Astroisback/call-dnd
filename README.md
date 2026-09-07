# Auto Reject

A Quick Settings toggle that **actually rejects** incoming phone calls instead of just
silencing them.

Built because OxygenOS/Truecaller only offer "silence unknown callers" — the call still
connects and rings out. This hangs up.

## Modes

Tap the tile to cycle:

| Mode | Behaviour |
|---|---|
| Off | Nothing is screened |
| Reject unknown | Saved contacts ring; everyone else is hung up |
| Reject all | Every incoming call is hung up |

Rejected calls **still appear in your call log** so you know who tried.

## Install

Grab the APK from [Releases](../../releases/tag/latest), then:

1. Open the app, tap **Grant screening role** (Android asks you to confirm).
2. Tap **Allow contacts access** (only needed for "Reject unknown").
3. Add the **Auto Reject** tile to your Quick Settings panel.

## Limits (read this)

**Only works for normal phone calls.** WhatsApp, Signal, Telegram, Discord and similar
register as `SelfManaged` connections in Android's Telecom stack, which explicitly
bypasses `CallScreeningService`. No third-party app can reject those — Android has no
API for it. Use each app's own privacy settings instead.

**Only one app can hold the screening role.** Granting it here takes it away from
Truecaller (or whatever holds it), so you lose that app's spam lookup.

Emergency callbacks are never rejected.

## Build

```bash
gradle :app:assembleRelease
```

Requires JDK 17 and Android SDK 36. CI builds on every push to `main` and publishes
the APK to the `latest` release.
