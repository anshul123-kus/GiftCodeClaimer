package com.giftclaimer.app.ui.profiles

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.giftclaimer.app.GiftClaimerApp
import com.giftclaimer.app.data.models.Profile
import com.giftclaimer.app.util.Constants
import kotlinx.coroutines.launch
import java.io.File

class ProfilesViewModel(application: Application) : AndroidViewModel(application) {

    private val profileDao = (application as GiftClaimerApp).database.profileDao()

    val allProfiles: LiveData<List<Profile>> = profileDao.getAllProfiles().asLiveData()

    fun addProfile(name: String): Long {
        var newId = 0L
        viewModelScope.launch {
            newId = profileDao.insert(Profile(
                name = name,
                dataDirSuffix = "profile_${System.currentTimeMillis()}"
            ))
        }
        return newId
    }

    fun updateProfile(profile: Profile) {
        viewModelScope.launch { profileDao.update(profile) }
    }

    fun toggleProfile(profile: Profile) {
        viewModelScope.launch { profileDao.update(profile.copy(isActive = !profile.isActive)) }
    }

    fun deleteProfile(profile: Profile) {
        viewModelScope.launch {
            profileDao.delete(profile)
            // Clean up profile data directory
            val dir = File(
                getApplication<GiftClaimerApp>().filesDir,
                "${Constants.PROFILE_DIR_PREFIX}${profile.id}"
            )
            if (dir.exists()) dir.deleteRecursively()
        }
    }

    fun markLoggedIn(profileId: Long) {
        viewModelScope.launch {
            val profile = profileDao.getById(profileId) ?: return@launch
            profileDao.update(profile.copy(isLoggedIn = true))
        }
    }
}
