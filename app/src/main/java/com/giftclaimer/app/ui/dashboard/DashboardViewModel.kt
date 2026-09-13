package com.giftclaimer.app.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import com.giftclaimer.app.GiftClaimerApp
import com.giftclaimer.app.data.models.ClaimLog
import java.util.Calendar

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val db = (application as GiftClaimerApp).database

    private val _isServiceRunning = MutableLiveData(false)
    val isServiceRunning: LiveData<Boolean> = _isServiceRunning

    val activeSiteCount: LiveData<Int> = db.siteDao().getActiveSiteCount().asLiveData()
    val activeProfileCount: LiveData<Int> = db.profileDao().getActiveProfileCount().asLiveData()

    val recentLogs: LiveData<List<ClaimLog>> = db.claimLogDao().getRecentLogs().asLiveData()

    val todaySuccessCount: LiveData<Int> = db.claimLogDao()
        .getTodaySuccessCount(getStartOfDay()).asLiveData()

    val todayTotalCount: LiveData<Int> = db.claimLogDao()
        .getTodayTotalCount(getStartOfDay()).asLiveData()

    fun setServiceRunning(running: Boolean) {
        _isServiceRunning.value = running
    }

    private fun getStartOfDay(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
