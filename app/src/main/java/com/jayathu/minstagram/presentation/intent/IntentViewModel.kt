package com.jayathu.minstagram.presentation.intent

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayathu.minstagram.domain.model.SessionIntention
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
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    var selectedIntention by mutableStateOf<SessionIntention?>(null)
        private set

    var selectedTimeLimitMinutes by mutableStateOf(5)
        private set

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

    fun selectIntention(intention: SessionIntention) {
        selectedIntention = intention
    }

    fun selectTimeLimit(minutes: Int) {
        selectedTimeLimitMinutes = minutes
    }
}
