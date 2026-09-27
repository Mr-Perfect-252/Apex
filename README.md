# Apex — ApexHub SDK sample app

A single-screen Android app (it just says **Apex**) wired to **both** official SDKs:

| SDK | Artifact (Maven Central) | Used for |
|-----|--------------------------|----------|
| **ApexHub OTA SDK** | `io.github.mr-perfect-252:sdk:1.0.1` | In-app updates, background update checks, built-in analytics |
| **Open Analytics** | `io.github.mr-perfect-252:open-analytics-android:1.0.0` | Sessions, screen views, offline batching, crash reports |

CI builds a signed debug APK and prints both SHAs. See [`.github/workflows/build-apk.yml`](.github/workflows/build-apk.yml).

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

### Open Analytics — `AnalyticsConfig` (only `endpoint` is required)

```
endpoint              required   e.g. https://apex-hub-production.vercel.app/api/v1/track
crashReportEndpoint   optional   defaults to sibling ".../api/v1/crash-report"
appId                 optional   label for your own reference
apiKey                optional   your pk_live_ key (sent as Authorization: Bearer)
headers               optional   extra headers on every request
enabled               optional   true (default)
debug                 optional   false (default)
promptForCrashReport  optional   true (default) — ask the user before sending a crash
```

Runtime calls: `OpenAnalytics.init(context, config)`, `.track(...)`, `.trackScreen(...)`,
`.trackError(...)`, `.trackConversion(...)`, `.identify(...)`, `.flush()`, `.processPendingCrashReports()`.

---

## 2. Where it's wired

- `app/src/main/java/com/apexhub/sample/SampleApp.kt` — inits both SDKs from `Application.onCreate()`
  (OTA `schedulePeriodicCheck` + `OpenAnalytics.init`) and holds the key/endpoint constants.
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

With the placeholder key, update checks simply 404 silently — the sample still builds and runs.

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
manually; download the `apex-sample-debug` artifact.

Local:

```bash
./gradlew assembleDebug      # app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease    # signed release APK
```

Requires JDK 17 and the Android SDK (platform 34, build-tools 34.0.0).
