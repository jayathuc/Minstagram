package com.jayathu.minstagram

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.jayathu.minstagram.presentation.navigation.MinstagramNavHost
import com.jayathu.minstagram.receiver.EndSessionReceiver
import com.jayathu.minstagram.service.SessionService
import com.jayathu.minstagram.ui.theme.MinstagramTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_SHOW_SUMMARY = "extra_show_summary"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MinstagramTheme {
                MinstagramNavHost(
                    startOnSummary = intent?.getBooleanExtra(EXTRA_SHOW_SUMMARY, false) == true,
                    summaryIntention = intent?.getStringExtra(SessionService.EXTRA_INTENTION),
                    summaryDuration = intent?.getIntExtra(EndSessionReceiver.EXTRA_DURATION_SECONDS, 0) ?: 0
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra(EXTRA_SHOW_SUMMARY, false)) {
            setContent {
                MinstagramTheme {
                    MinstagramNavHost(
                        startOnSummary = true,
                        summaryIntention = intent.getStringExtra(SessionService.EXTRA_INTENTION),
                        summaryDuration = intent.getIntExtra(EndSessionReceiver.EXTRA_DURATION_SECONDS, 0)
                    )
                }
            }
        }
    }
}
