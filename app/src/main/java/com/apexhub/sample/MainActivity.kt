package com.apexhub.sample

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.apexhub.sdk.ApexHubConfig
import com.apexhub.sdk.ApexHubUpdater
import com.opensdk.analytics.OpenAnalytics
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var updater: ApexHubUpdater

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        updater = ApexHubUpdater(
            context = this,
            config = ApexHubConfig(publicKey = SampleApp.PUBLIC_KEY)
        )

        // Screen view via open-analytics-android (auto-tracked too; explicit here for clarity).
        OpenAnalytics.trackScreen("home")

        // ApexHub OTA: check → dialog → download → verify → Android system installer.
        findViewById<Button>(R.id.btn_update).setOnClickListener {
            lifecycleScope.launch {
                updater.checkAndPrompt(activity = this@MainActivity)
            }
        }

        // Both SDKs can record an event — shown side by side for comparison.
        findViewById<Button>(R.id.btn_track).setOnClickListener {
            // open-analytics-android → POST /api/v1/track
            OpenAnalytics.track("apex_tapped", properties = mapOf("source" to "home"))

            // ApexHub SDK → POST /api/analytics/event
            lifecycleScope.launch {
                updater.trackEvent(
                    appId = "apex-sample",
                    eventType = "custom",
                    eventName = "apex_tapped",
                    metadata = mapOf("source" to "home"),
                )
            }

            Toast.makeText(this, "Event tracked", Toast.LENGTH_SHORT).show()
        }
    }
}
