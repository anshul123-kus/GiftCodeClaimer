package com.giftclaimer.app.ui.sites

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.giftclaimer.app.GiftClaimerApp
import com.giftclaimer.app.data.models.Site
import kotlinx.coroutines.launch

class SitesViewModel(application: Application) : AndroidViewModel(application) {

    private val siteDao = (application as GiftClaimerApp).database.siteDao()

    val allSites: LiveData<List<Site>> = siteDao.getAllSites().asLiveData()

    fun addSite(site: Site) {
        viewModelScope.launch { siteDao.insert(site) }
    }

    fun updateSite(site: Site) {
        viewModelScope.launch { siteDao.update(site) }
    }

    fun deleteSite(site: Site) {
        viewModelScope.launch { siteDao.delete(site) }
    }

    fun toggleSite(site: Site) {
        viewModelScope.launch { siteDao.update(site.copy(isActive = !site.isActive)) }
    }
}
