package com.jayathu.minstagram

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.jayathu.minstagram.presentation.navigation.MinstagramNavHost
import com.jayathu.minstagram.ui.theme.MinstagramTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MinstagramTheme {
                MinstagramNavHost()
            }
        }
    }
}
