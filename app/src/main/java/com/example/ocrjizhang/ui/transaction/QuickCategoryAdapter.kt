package com.example.ocrjizhang.ui.transaction

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.ocrjizhang.R
import com.example.ocrjizhang.databinding.ItemQuickCategoryBinding
import com.example.ocrjizhang.ui.category.CategoryIconRegistry

class QuickCategoryAdapter(
    private val onSelect: (CategoryOption) -> Unit,
) : ListAdapter<CategoryOption, QuickCategoryAdapter.QuickCategoryViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuickCategoryViewHolder {
        val binding = ItemQuickCategoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return QuickCategoryViewHolder(binding, onSelect)
    }

    override fun onBindViewHolder(holder: QuickCategoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class QuickCategoryViewHolder(
        private val binding: ItemQuickCategoryBinding,
        private val onSelect: (CategoryOption) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CategoryOption) {
            binding.nameView.text = item.name
            binding.iconView.setImageResource(CategoryIconRegistry.iconResFor(item.iconKey))

            val context = binding.root.context
            val isSelected = item.isSelected
            val cardColor = if (isSelected) {
                R.color.surface_tint_soft
            } else {
                R.color.surface_container
            }
            val strokeColor = if (isSelected) {
                R.color.primary_brand
            } else {
                R.color.outline_soft
            }
            val symbolBackground = if (isSelected) {
                R.color.primary_brand
            } else {
                R.color.surface_base
            }
            val iconTintColor = if (isSelected) {
                android.R.color.white
            } else {
                R.color.primary_brand_dark
            }
            val nameTextColor = if (isSelected) {
                R.color.primary_brand_dark
            } else {
                R.color.ink_primary
            }

            binding.root.setCardBackgroundColor(ContextCompat.getColor(context, cardColor))
            binding.root.strokeColor = ContextCompat.getColor(context, strokeColor)
            binding.symbolCard.setCardBackgroundColor(ContextCompat.getColor(context, symbolBackground))
            binding.iconView.setColorFilter(ContextCompat.getColor(context, iconTintColor))
            binding.nameView.setTextColor(ContextCompat.getColor(context, nameTextColor))

            binding.root.setOnClickListener {
                onSelect(item)
            }
        }
    }

    private companion object DiffCallback : DiffUtil.ItemCallback<CategoryOption>() {
        override fun areItemsTheSame(oldItem: CategoryOption, newItem: CategoryOption): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: CategoryOption, newItem: CategoryOption): Boolean =
            oldItem == newItem
    }
}
