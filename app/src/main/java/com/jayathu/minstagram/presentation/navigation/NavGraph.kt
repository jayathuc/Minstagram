package com.jayathu.minstagram.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.jayathu.minstagram.domain.model.SessionIntention
import com.jayathu.minstagram.presentation.browser.BrowserScreen
import com.jayathu.minstagram.presentation.intent.IntentScreen

@Composable
fun MinstagramNavHost(
    navController: NavHostController = rememberNavController()
) {
    NavHost(navController = navController, startDestination = "intent") {
        composable("intent") {
            IntentScreen(
                onIntentSelected = { intention ->
                    navController.navigate("browser/${intention.name}")
                }
            )
        }
        composable(
            route = "browser/{intention}",
            arguments = listOf(
                navArgument("intention") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val intentionName = backStackEntry.arguments?.getString("intention")
                ?: SessionIntention.JUST_BROWSING.name
            val intention = SessionIntention.valueOf(intentionName)
            BrowserScreen(
                intention = intention,
                onSessionEnd = {
                    navController.navigate("intent") {
                        popUpTo("intent") { inclusive = true }
                    }
                }
            )
        }
    }
}
