package com.giftclaimer.app.ui.profiles

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.giftclaimer.app.R
import com.giftclaimer.app.data.models.Profile
import com.giftclaimer.app.databinding.ItemProfileBinding

class ProfileAdapter(
    private val onToggle: (Profile) -> Unit,
    private val onLogin: (Profile) -> Unit,
    private val onDelete: (Profile) -> Unit
) : ListAdapter<Profile, ProfileAdapter.ProfileViewHolder>(ProfileDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProfileViewHolder {
        val binding = ItemProfileBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProfileViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProfileViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ProfileViewHolder(private val binding: ItemProfileBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(profile: Profile) {
            binding.tvProfileName.text = profile.name
            binding.tvProfileLabel.text = profile.label.ifEmpty { "No label" }
            binding.switchProfile.isChecked = profile.isActive

            binding.tvLoginStatus.text = if (profile.isLoggedIn) "Logged In" else "Not Logged In"
            binding.tvLoginStatus.setTextColor(
                ContextCompat.getColor(
                    binding.root.context,
                    if (profile.isLoggedIn) R.color.success_green else R.color.error_red
                )
            )

            binding.switchProfile.setOnCheckedChangeListener { _, _ -> onToggle(profile) }
            binding.btnLogin.setOnClickListener { onLogin(profile) }
            binding.btnDeleteProfile.setOnClickListener { onDelete(profile) }
        }
    }

    class ProfileDiffCallback : DiffUtil.ItemCallback<Profile>() {
        override fun areItemsTheSame(oldItem: Profile, newItem: Profile) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Profile, newItem: Profile) = oldItem == newItem
    }
}
