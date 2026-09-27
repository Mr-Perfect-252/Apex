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

        // ── 2) apex-analytics ─────────────────────────────────────
        // The ingestion endpoint is fixed to the ApexHub backend inside the SDK, so
        // there is nothing to point at — pass only your app's public key.
        OpenAnalytics.init(
            this,
            AnalyticsConfig(
                apiKey = PUBLIC_KEY,      // required — activates the SDK, attributes events
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
    }
}
