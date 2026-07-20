package com.jayathu.minstagram.presentation.history

import androidx.lifecycle.ViewModel
import com.jayathu.minstagram.data.local.SessionDao
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(dao: SessionDao) : ViewModel() {
    // one source of truth; the screen buckets and groups it locally
    val sessions = dao.all()
}
