package com.giftclaimer.app.ui.settings

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.giftclaimer.app.GiftClaimerApp
import com.giftclaimer.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        findPreference<Preference>("pref_clear_logs")?.setOnPreferenceClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Clear All Logs")
                .setMessage("Are you sure you want to delete all claim logs?")
                .setPositiveButton("Clear") { _, _ ->
                    CoroutineScope(Dispatchers.IO).launch {
                        (requireActivity().application as GiftClaimerApp)
                            .database.claimLogDao().deleteAll()
                    }
                    Toast.makeText(requireContext(), "Logs cleared!", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
            true
        }

        findPreference<Preference>("pref_about")?.summary = "Gift Code Auto Claimer v1.0\nMulti-profile gift code auto-claiming app"
    }
}
