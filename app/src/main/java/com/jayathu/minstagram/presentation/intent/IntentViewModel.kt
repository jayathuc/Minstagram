package com.jayathu.minstagram.presentation.intent

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.jayathu.minstagram.domain.model.SessionIntention
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class IntentViewModel @Inject constructor() : ViewModel() {

    var selectedIntention by mutableStateOf<SessionIntention?>(null)
        private set

    var selectedTimeLimitMinutes by mutableStateOf(5)
        private set

    fun selectIntention(intention: SessionIntention) {
        selectedIntention = intention
    }

    fun selectTimeLimit(minutes: Int) {
        selectedTimeLimitMinutes = minutes
    }
}
