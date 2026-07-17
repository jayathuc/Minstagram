package com.jayathu.minstagram.presentation.navigation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.jayathu.minstagram.data.Prefs
import com.jayathu.minstagram.domain.model.SessionIntention
import com.jayathu.minstagram.presentation.intent.IntentScreen
import com.jayathu.minstagram.presentation.intent.SessionConfig
import com.jayathu.minstagram.presentation.summary.SessionSummaryScreen
import com.jayathu.minstagram.service.SessionService
import com.jayathu.minstagram.service.UsageMonitorService
import com.jayathu.minstagram.util.hasUsageAccess
import com.jayathu.minstagram.util.launchInstagram

private const val SNOOZE_MINUTES = 30

@Composable
fun MinstagramNavHost(
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    NavHost(navController = navController, startDestination = "intent") {
        composable("intent") {
            // holds the session config so permission callbacks can resume the launch
            val pendingConfig = remember { mutableListOf<SessionConfig>() }

            // true when the monitor caught a direct Instagram open
            var intercepted by remember { mutableStateOf(false) }
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        val prefs = Prefs.get(context)
                        if (prefs.getBoolean(Prefs.INTERCEPTED_PROMPT, false)) {
                            prefs.edit().remove(Prefs.INTERCEPTED_PROMPT).apply()
                            intercepted = true
                        }
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            fun startUsageMonitor() {
                if (hasUsageAccess(context)) {
                    context.startForegroundService(
                        Intent(context, UsageMonitorService::class.java)
                    )
                }
            }

            fun launchSession(config: SessionConfig) {
                val serviceIntent = Intent(context, SessionService::class.java).apply {
                    putExtra(SessionService.EXTRA_INTENTION, config.intention.name)
                    putExtra(SessionService.EXTRA_TIME_LIMIT_SECONDS, config.timeLimitMinutes * 60)
                }
                context.startForegroundService(serviceIntent)
                startUsageMonitor()
                launchInstagram(context)
            }

            fun snooze() {
                Prefs.get(context).edit()
                    .putLong(
                        Prefs.SNOOZE_UNTIL_MS,
                        System.currentTimeMillis() + SNOOZE_MINUTES * 60_000L
                    )
                    .apply()
                launchInstagram(context)
            }

            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { granted ->
                if (granted && pendingConfig.isNotEmpty()) {
                    launchSession(pendingConfig.removeFirst())
                } else if (!granted) {
                    Toast.makeText(
                        context,
                        "Notification permission is required for the session timer",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            val usageStatsLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) {
                if (pendingConfig.isNotEmpty()) {
                    // launch either way; without the permission we just can't monitor
                    launchSession(pendingConfig.removeFirst())
                }
            }

            val overlayLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) {
                if (Settings.canDrawOverlays(context) && pendingConfig.isNotEmpty()) {
                    launchSession(pendingConfig.removeFirst())
                }
            }

            IntentScreen(
                intercepted = intercepted,
                onSnooze = { snooze() },
                onSessionStart = { config ->
                    // 1. overlay permission for the session banner
                    if (!Settings.canDrawOverlays(context)) {
                        pendingConfig.clear()
                        pendingConfig.add(config)
                        Toast.makeText(
                            context,
                            "Please allow \"Display over other apps\" for the session banner",
                            Toast.LENGTH_LONG
                        ).show()
                        val overlayIntent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                        overlayLauncher.launch(overlayIntent)
                        return@IntentScreen
                    }
                    // 2. notification permission (Android 13+)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED
                    ) {
                        pendingConfig.clear()
                        pendingConfig.add(config)
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        return@IntentScreen
                    }
                    // 3. usage access, needed to pause the timer and catch direct opens
                    if (!hasUsageAccess(context)) {
                        pendingConfig.clear()
                        pendingConfig.add(config)
                        Toast.makeText(
                            context,
                            "Please allow usage access so Minstagram can track your session",
                            Toast.LENGTH_LONG
                        ).show()
                        usageStatsLauncher.launch(
                            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                        )
                        return@IntentScreen
                    }
                    // 4. all set
                    launchSession(config)
                }
            )
        }
        composable(
            route = "summary/{intention}/{duration}",
            arguments = listOf(
                navArgument("intention") { type = NavType.StringType },
                navArgument("duration") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val intentionName = backStackEntry.arguments?.getString("intention")
                ?: SessionIntention.JUST_BROWSING.name
            val intention = SessionIntention.valueOf(intentionName)
            val duration = backStackEntry.arguments?.getInt("duration") ?: 0
            SessionSummaryScreen(
                intention = intention,
                durationSeconds = duration,
                onDone = {
                    navController.navigate("intent") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }

    // a session that ended while we were in the background leaves its summary
    // in prefs; check whenever the app comes to the foreground. This runs after
    // NavHost so the graph is ready when we navigate.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val prefs = Prefs.get(context)
                val intention = prefs.getString(Prefs.PENDING_SUMMARY_INTENTION, null)
                if (intention != null) {
                    val seconds = prefs.getInt(Prefs.PENDING_SUMMARY_SECONDS, 0)
                    prefs.edit()
                        .remove(Prefs.PENDING_SUMMARY_INTENTION)
                        .remove(Prefs.PENDING_SUMMARY_SECONDS)
                        .apply()
                    navController.navigate("summary/$intention/$seconds") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}
