package com.jayathu.minstagram.presentation.browser

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// Excludes elapsedSeconds — kept in a separate StateFlow so the timer tick
// does not recompose the WebView container, only the SessionBanner.
data class BrowserUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

@HiltViewModel
class BrowserViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(BrowserUiState())
    val uiState: StateFlow<BrowserUiState> = _uiState.asStateFlow()

    private val _elapsedSeconds = MutableStateFlow(0)
    val elapsedSeconds: StateFlow<Int> = _elapsedSeconds.asStateFlow()

    private val startTimeMs = System.currentTimeMillis()

    init {
        viewModelScope.launch {
            while (true) {
                delay(1000L)
                _elapsedSeconds.value = ((System.currentTimeMillis() - startTimeMs) / 1000).toInt()
            }
        }
    }

    fun setLoading(loading: Boolean) {
        _uiState.update { it.copy(isLoading = loading) }
    }

    fun setError(message: String) {
        _uiState.update { it.copy(isLoading = false, errorMessage = message) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
