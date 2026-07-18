package com.jayathu.minstagram.presentation.intent

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayathu.minstagram.data.local.SessionDao
import com.jayathu.minstagram.util.hasUsageAccess
import com.jayathu.minstagram.util.instagramUsageMs
import com.jayathu.minstagram.util.sevenDaysAgoMs
import com.jayathu.minstagram.util.startOfTodayMs
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class IntentViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    sessionDao: SessionDao
) : ViewModel() {

    val sessionsToday = sessionDao.countSince(startOfTodayMs())

    // real Instagram screen time, negative means unknown
    var usageTodayMs by mutableStateOf(-1L)
        private set

    var usageWeekMs by mutableStateOf(-1L)
        private set

    init {
        loadUsage()
    }

    fun loadUsage() {
        if (!hasUsageAccess(appContext)) return
        viewModelScope.launch {
            val (today, week) = withContext(Dispatchers.IO) {
                instagramUsageMs(appContext, startOfTodayMs()) to
                    instagramUsageMs(appContext, sevenDaysAgoMs())
            }
            usageTodayMs = today
            usageWeekMs = week
        }
    }
}
