package com.giftclaimer.app.ui.dashboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.giftclaimer.app.R
import com.giftclaimer.app.data.models.ClaimLog
import com.giftclaimer.app.databinding.ItemLogBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LogAdapter : ListAdapter<ClaimLog, LogAdapter.LogViewHolder>(LogDiffCallback()) {

    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val binding = ItemLogBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LogViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class LogViewHolder(private val binding: ItemLogBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(log: ClaimLog) {
            val time = timeFormat.format(Date(log.timestamp))
            val shortCode = log.code.take(8) + "..."
            val icon = when (log.result) {
                "SUCCESS" -> "\u2713"
                else -> "\u2717"
            }

            binding.tvLogTime.text = "[$time]"
            binding.tvLogCode.text = shortCode
            binding.tvLogSite.text = log.siteName
            binding.tvLogProfile.text = log.profileName
            binding.tvLogResult.text = icon
            binding.tvLogResult.setTextColor(
                ContextCompat.getColor(
                    binding.root.context,
                    if (log.result == "SUCCESS") R.color.success_green else R.color.error_red
                )
            )

            if (log.message.isNotEmpty()) {
                binding.tvLogMessage.text = log.message
                binding.tvLogMessage.visibility = android.view.View.VISIBLE
            } else {
                binding.tvLogMessage.visibility = android.view.View.GONE
            }
        }
    }

    class LogDiffCallback : DiffUtil.ItemCallback<ClaimLog>() {
        override fun areItemsTheSame(oldItem: ClaimLog, newItem: ClaimLog) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: ClaimLog, newItem: ClaimLog) = oldItem == newItem
    }
}
