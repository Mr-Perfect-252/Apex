# Apex — ApexHub SDK sample app

A single-screen Android app (it just says **Apex**) wired to **both** official SDKs:

| SDK | Artifact (Maven Central) | Used for |
|-----|--------------------------|----------|
| **ApexHub OTA SDK** | `io.github.mr-perfect-252:sdk:1.0.1` | In-app updates, background update checks, built-in event tracking |
| **Apex Analytics** | `io.github.mr-perfect-252:apex-analytics:1.0.0` | Sessions, screen views, offline batching, crash reports |

CI builds a signed release APK and prints both SHAs. See [`.github/workflows/build-apk.yml`](.github/workflows/build-apk.yml).

---

## 1. Fields you declare in code (plain text)

### ApexHub OTA — `ApexHubConfig` (only `publicKey` is required)

```
publicKey           required   your app's public key, starts with pk_live_ (or pk_test_)
packageName         optional   defaults to the host app's own package name
channel             optional   "stable" (default) | "beta" | "nightly"
baseUrl             optional   defaults to https://apex-hub-production.vercel.app
checkIntervalHours  optional   background check cadence in hours, default 6, minimum 1
updateStrategy      optional   FLEXIBLE (default) | IMMEDIATE
allowMeteredNetwork optional   false (default); true allows background checks on cellular
```

Runtime calls:

```
ApexHubUpdater(context, config)
  .schedulePeriodicCheck(appDisplayName)   // WorkManager, dedup by work name; safe every launch
  .cancelPeriodicCheck()
  .checkAndPrompt(activity)                // check -> dialog -> download -> verify -> installer
  .checkForUpdate()                        // -> UpdateCheckResult.{UpdateAvailable,UpToDate,Error}
  .checkAndUpdate(activity, onUpdateFound, onProgress, onReadyToInstall, onError)
  .trackEvent(appId, eventType, eventName, metadata)   // -> POST /api/analytics/event
```

### Apex Analytics — `AnalyticsConfig` (only `apiKey` is required)

`apex-analytics` talks to **ApexHub only** — the ingestion endpoints are baked into the SDK, so
there is no `endpoint` to pass. Your `pk_live_…` key activates the SDK and attributes every event.

```
apiKey                required   your pk_live_ key (sent as Authorization: Bearer); must start pk_live_ / pk_test_
appId                 optional   label attached to events (default null)
headers               optional   extra headers on every request (default {})
enabled               optional   true (default)
debug                 optional   false (default)
inactivityTimeoutMs   optional   1800000 (30 min) — session expiry
flushIntervalMs       optional   5000 — background flush cadence
batchSize             optional   20 — events per flush
promptForCrashReport  optional   true (default) — ask the user before sending a crash
disableAutoScreenView / disableAutoCrashCapture / disableAutoPerformance   optional   false (default) — opt-outs

endpoint / crashReportEndpoint   NOT configurable — fixed to
                                 https://apex-hub-production.vercel.app/api/v1/{track,crash-report}
```

Runtime calls: `OpenAnalytics.init(context, config)`, `.track(...)`, `.trackScreen(...)`,
`.trackError(...)`, `.trackConversion(...)`, `.identify(...)`, `.flush()`, `.processPendingCrashReports()`.

---

## 2. Where it's wired

- `app/src/main/java/com/apexhub/sample/SampleApp.kt` — inits both SDKs from `Application.onCreate()`
  (OTA `schedulePeriodicCheck` + `OpenAnalytics.init`) and holds the `PUBLIC_KEY` constant.
- `app/src/main/java/com/apexhub/sample/MainActivity.kt` — the single screen; "Check for updates"
  runs `checkAndPrompt()`, "Track event" records the same event through **both** SDKs.
- `app/src/main/AndroidManifest.xml` — declares `POST_NOTIFICATIONS` (the SDK merges
  INTERNET / REQUEST_INSTALL_PACKAGES / RECEIVE_BOOT_COMPLETED itself).

## 3. Make OTA actually return an update

1. In the ApexHub Console, create an app whose **package name is `com.apexhub.sample`**.
2. Copy its `pk_live_…` key and paste it over `PUBLIC_KEY` in `SampleApp.kt`.
3. Upload a release with a **higher versionCode** than `1`.
4. Launch the app → "Check for updates". Updates install through the normal Android installer
   (the app must be signed with the same key and a higher versionCode).

This sample is already wired to a live app registered in the ApexHub store
(package `com.apexhub.sample`, public key `pk_live_B2Lj4nS1OPWtJxkEmqHGfW0nYZxmQxC5`),
so update checks and analytics work against the real backend out of the box.
To ship your own app, replace these constants with your app's values.

## 4. Signing keystore + SHA

A **demo** keystore is committed at `keystore/apex-sample.keystore` (PKCS12, password `apexhub`,
alias `apex-sample`) and used to sign **both** debug and release. A stable key is required so an
in-place OTA update is allowed by Android (the OS rejects an update signed by a different key), and
ApexHub pins the signing certificate fingerprint per app.

```
Keystore        keystore/apex-sample.keystore   (storeType PKCS12)
Store password  apexhub
Key alias       apex-sample
Key password    apexhub

Signing certificate SHA-256 (for ApexHub certificate pinning):
  DA:4E:F4:DC:FF:9E:8F:5B:34:73:19:82:C9:BE:DB:AB:05:F0:66:29:57:89:C8:06:C8:35:2C:B1:F3:7D:EA:93

Signing certificate SHA-1:
  31:A3:E7:BD:2F:56:54:71:26:D5:D9:B3:1F:20:8D:3A:68:65:14:88
```

The workflow also prints the **APK's own SHA-256** after each build (that's the value ApexHub stores
and the SDK verifies on download). Replace this demo keystore with your own before shipping anything
real — never commit a production keystore.

## 5. Build

CI (recommended — no local Android SDK needed): push to `main` or run the **Build APK** workflow
manually; download the `apex-sample-release` artifact.

Local:

```bash
./gradlew assembleDebug      # app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease    # signed release APK
```

Requires JDK 17 and the Android SDK (platform 34, build-tools 34.0.0).
