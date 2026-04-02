package com.example.ocrjizhang.ui.asset

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.ocrjizhang.R
import com.example.ocrjizhang.databinding.ItemAssetAccountBinding

class AssetAdapter(
    private val onEdit: (AssetAccountItem) -> Unit,
    private val onDelete: (AssetAccountItem) -> Unit,
) : ListAdapter<AssetAccountItem, AssetAdapter.AssetViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AssetViewHolder {
        val binding = ItemAssetAccountBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return AssetViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AssetViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class AssetViewHolder(
        private val binding: ItemAssetAccountBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: AssetAccountItem) {
            binding.symbolView.text = item.symbol
            binding.nameView.text = item.name
            binding.detailView.text = item.detail
            binding.balanceView.text = item.balanceLabel
            binding.badgeView.text = binding.root.context.getString(
                if (item.isDefault) {
                    R.string.asset_badge_default
                } else {
                    R.string.asset_badge_custom
                },
            )
            binding.editButton.setOnClickListener { onEdit(item) }
            binding.deleteButton.setOnClickListener { onDelete(item) }
            binding.root.setOnClickListener { onEdit(item) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<AssetAccountItem>() {
        override fun areItemsTheSame(oldItem: AssetAccountItem, newItem: AssetAccountItem): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: AssetAccountItem, newItem: AssetAccountItem): Boolean =
            oldItem == newItem
    }
}
