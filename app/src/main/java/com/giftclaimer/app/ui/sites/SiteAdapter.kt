package com.giftclaimer.app.ui.sites

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.giftclaimer.app.data.models.Site
import com.giftclaimer.app.databinding.ItemSiteBinding

class SiteAdapter(
    private val onToggle: (Site) -> Unit,
    private val onEdit: (Site) -> Unit,
    private val onDelete: (Site) -> Unit
) : ListAdapter<Site, SiteAdapter.SiteViewHolder>(SiteDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SiteViewHolder {
        val binding = ItemSiteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SiteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SiteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SiteViewHolder(private val binding: ItemSiteBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(site: Site) {
            binding.tvSiteName.text = site.name
            binding.tvSiteUrl.text = site.url
            binding.switchSite.isChecked = site.isActive

            binding.switchSite.setOnCheckedChangeListener { _, _ ->
                onToggle(site)
            }
            binding.btnEditSite.setOnClickListener { onEdit(site) }
            binding.btnDeleteSite.setOnClickListener { onDelete(site) }
        }
    }

    class SiteDiffCallback : DiffUtil.ItemCallback<Site>() {
        override fun areItemsTheSame(oldItem: Site, newItem: Site) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Site, newItem: Site) = oldItem == newItem
    }
}
