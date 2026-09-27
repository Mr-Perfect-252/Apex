package com.apexhub.sample

import android.app.Application
import android.util.Log
import com.apexhub.sdk.ApexHubConfig
import com.apexhub.sdk.ApexHubUpdater
import com.opensdk.analytics.OpenAnalytics
import com.opensdk.analytics.model.AnalyticsConfig

class SampleApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // ── 1) ApexHub OTA SDK ────────────────────────────────────────────
        // Schedules a periodic background update check (WorkManager). Safe to call
        // on every launch — WorkManager deduplicates by work name.
        val updater = ApexHubUpdater(
            context = this,
            config = ApexHubConfig(
                publicKey = PUBLIC_KEY,   // required
                channel = "stable",       // optional: stable | beta | nightly
                checkIntervalHours = 6,   // optional: >= 1
            )
        )
        updater.schedulePeriodicCheck(appDisplayName = "Apex")

        // ── 2) open-analytics-android ─────────────────────────────────────
        OpenAnalytics.init(
            this,
            AnalyticsConfig(
                endpoint = TRACK_URL,     // required
                apiKey = PUBLIC_KEY,      // your pk_live_ key (sent as Authorization)
                appId = "apex-sample",    // optional label
                debug = true,             // verbose logcat
            )
        )

        Log.i(TAG, "ApexHub SDK + OpenAnalytics initialised")
    }

    companion object {
        private const val TAG = "ApexSample"

        // ApexHub app public key (Console → your app → Settings). Safe to ship in the app.
        const val PUBLIC_KEY = "pk_live_B2Lj4nS1OPWtJxkEmqHGfW0nYZxmQxC5"

        // ApexHub backend analytics ingestion endpoint (open-analytics-android wire format).
        const val TRACK_URL = "https://apex-hub-production.vercel.app/api/v1/track"
    }
}
