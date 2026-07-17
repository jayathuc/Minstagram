package com.jayathu.minstagram.presentation.summary

import androidx.lifecycle.ViewModel
import com.jayathu.minstagram.data.local.SessionDao
import com.jayathu.minstagram.util.sevenDaysAgoMs
import com.jayathu.minstagram.util.startOfTodayMs
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SummaryViewModel @Inject constructor(dao: SessionDao) : ViewModel() {
    val latest = dao.latest()
    val todayCount = dao.countSince(startOfTodayMs())
    val weekSeconds = dao.totalSecondsSince(sevenDaysAgoMs())
}
