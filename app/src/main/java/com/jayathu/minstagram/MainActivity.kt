package com.jayathu.minstagram

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.jayathu.minstagram.presentation.navigation.MinstagramNavHost
import com.jayathu.minstagram.service.UsageMonitorService
import com.jayathu.minstagram.ui.theme.MinstagramTheme
import com.jayathu.minstagram.util.hasUsageAccess
import dagger.hilt.android.AndroidEntryPoint

// Screen changes (summary, intercepted gate) are driven by prefs checked on
// resume, not intent extras. Android drops extras when it just brings an
// existing task forward, so extras were unreliable here.
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (hasUsageAccess(this)) {
            startForegroundService(Intent(this, UsageMonitorService::class.java))
        }
        setContent {
            MinstagramTheme {
                MinstagramNavHost()
            }
        }
    }
}
