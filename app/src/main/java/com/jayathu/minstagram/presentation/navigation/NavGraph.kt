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
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.jayathu.minstagram.domain.model.SessionIntention
import com.jayathu.minstagram.presentation.intent.IntentScreen
import com.jayathu.minstagram.presentation.summary.SessionSummaryScreen
import com.jayathu.minstagram.service.SessionService

@Composable
fun MinstagramNavHost(
    navController: NavHostController = rememberNavController(),
    startOnSummary: Boolean = false,
    summaryIntention: String? = null,
    summaryDuration: Int = 0
) {
    val startDest = if (startOnSummary && summaryIntention != null) {
        "summary/${summaryIntention}/${summaryDuration}"
    } else {
        "intent"
    }

    NavHost(navController = navController, startDestination = startDest) {
        composable("intent") {
            val context = LocalContext.current

            // Holds the intention chosen by the user so the permission callback can use it
            val pendingIntention = remember { mutableListOf<SessionIntention>() }

            fun launchSession(intention: SessionIntention) {
                val serviceIntent = Intent(context, SessionService::class.java).apply {
                    putExtra(SessionService.EXTRA_INTENTION, intention.name)
                }
                context.startForegroundService(serviceIntent)

                val igIntent = context.packageManager
                    .getLaunchIntentForPackage("com.instagram.android")
                if (igIntent != null) {
                    context.startActivity(igIntent)
                } else {
                    Toast.makeText(context, "Instagram is not installed", Toast.LENGTH_SHORT).show()
                }
            }

            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { granted ->
                if (granted && pendingIntention.isNotEmpty()) {
                    launchSession(pendingIntention.removeFirst())
                } else if (!granted) {
                    Toast.makeText(
                        context,
                        "Notification permission is required for the session timer",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            // Overlay permission requires sending the user to Settings; this launcher
            // resumes the session launch when they return.
            val overlayLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) {
                if (Settings.canDrawOverlays(context) && pendingIntention.isNotEmpty()) {
                    launchSession(pendingIntention.removeFirst())
                }
            }

            IntentScreen(
                onIntentSelected = { intention ->
                    // 1. Check overlay permission first
                    if (!Settings.canDrawOverlays(context)) {
                        pendingIntention.clear()
                        pendingIntention.add(intention)
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
                    // 2. Check notification permission (Android 13+)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED
                    ) {
                        pendingIntention.clear()
                        pendingIntention.add(intention)
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        return@IntentScreen
                    }
                    // 3. All permissions granted — launch
                    launchSession(intention)
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
}
