# Saldos (Android)

Native Android client for **CNT** (Ecuador) prepaid balances. It reads the
remaining quota from CNT and exposes it through a balance screen and home
screen widgets.

> Unofficial client. Not affiliated with CNT. CNT may change its API at any
> time and break the app.

## Features

- Direct CNT login from the device (Sanctum cookie flow); no relay server.
- Credentials encrypted at rest with Android Keystore.
- Balance screen with a usage ring, per-item quotas and a line selector.
- Home screen widgets: **2x1** (ring + main quota) and **1x1** (ring).
- Periodic refresh every 15 minutes.
- Configurable refresh policy:
  - **Mobile data only** (default): skips automatic refresh while on WiFi.
  - **Always**: refreshes on WiFi too.
  - **Manual**: only when the user taps refresh.
- Offline cache: the last snapshot stays visible when the network fails.
- Settings screen with account, refresh policy, widget line, privacy and version.

## Requirements

- JDK 17
- Android SDK 37 (`platforms;android-37.0`, `build-tools;37.0.0`)
- A device running Android 8.0+ (minSdk 26)

The Gradle wrapper is included.

## Build

```bash
# Point Gradle at your SDK (not committed)
echo "sdk.dir=$ANDROID_HOME" > local.properties

./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Run the unit tests:

```bash
./gradlew :app:testDebugUnitTest
```

## Release

Release builds are signed with a local keystore. The signing material is not
committed:

- `keystore/saldos-release.jks`
- `keystore.properties`
- `keystore/CREDENTIALS.txt`

`keystore.properties` format:

```properties
storeFile=keystore/saldos-release.jks
storePassword=...
keyAlias=saldos
keyPassword=...
```

Build the signed APK:

```bash
./gradlew :app:assembleRelease
# app/build/outputs/apk/release/app-release.apk
```

The release build enables R8 and resource shrinking. Because it uses a
different signing key than the debug build, uninstall the debug app before
installing the release APK.

## Architecture

```
app/src/main/java/com/thegreatyamori/saldos/
├─ data/
│  ├─ cnt/        CNT HTTP client, CookieJar, crypto, mapper
│  ├─ creds/      Android Keystore credential storage
│  ├─ cache/      Snapshot JSON cache
│  └─ settings/   Refresh policy and preferred line
├─ domain/        Snapshot model and BalancesRepository
├─ work/          WorkManager refresh worker and network policy
├─ widget/        Glance widgets (2x1 and 1x1), ring bitmap renderer
├─ ui/            Compose login, home, settings and ViewModel
└─ util/          Time formatting
```

Uses Kotlin, Jetpack Compose, Glance, WorkManager, OkHttp and
kotlinx-serialization.

## CNT integration notes

The client reproduces the CNT web client flow:

1. `GET /sanctum/csrf-cookie`
2. `POST /api/login` with the AES-encrypted password
3. `GET /api/user`
4. `POST /api/balances_consumed/` with the AES-encrypted identifier

The payload encryption matches CryptoJS with a passphrase: OpenSSL
`EVP_BytesToKey` (MD5) + AES-256-CBC, a random 8-byte salt and the
`Salted__` base64 envelope. The `iv` option used by the original front-end is
ignored by CryptoJS.

CNT reports the **remaining** quota in the `consumed` field, so the ring shows
`(total - remaining) / total` as used.

Cookies are persisted with OkHttp's `CookieJar` to keep the session across
process restarts.

## Refresh behavior

- `BalanceRefreshWorker` runs every 15 minutes with a `CONNECTED` constraint.
- The worker checks the stored policy before touching the network.
- `NetworkPolicy` reads the active transport; on WiFi the default policy skips.
- Manual refresh ignores the automatic policy and always attempts a fetch.
- WorkManager's 15 minutes is a minimum; Doze and battery optimization can
  delay it. The UI always shows the last update time.

## Privacy and security

- Credentials are encrypted with an AES key generated in Android Keystore.
- Cloud backup and device transfer are disabled for app data.
- The password must be recoverable to log in to CNT, so it is encrypted rather
  than hashed.
- There is no backend: requests go from the device to CNT.
- Widgets only read the local snapshot cache.
