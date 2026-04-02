package com.example.ocrjizhang.ui.category

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.ocrjizhang.R
import com.example.ocrjizhang.databinding.ItemCategoryBinding

class CategoryAdapter(
    private val onEdit: (CategoryListItem) -> Unit,
    private val onDelete: (CategoryListItem) -> Unit,
) : ListAdapter<CategoryListItem, CategoryAdapter.CategoryViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return CategoryViewHolder(binding, onEdit, onDelete)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class CategoryViewHolder(
        private val binding: ItemCategoryBinding,
        private val onEdit: (CategoryListItem) -> Unit,
        private val onDelete: (CategoryListItem) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CategoryListItem) {
            binding.iconView.setImageResource(CategoryIconRegistry.iconResFor(item.iconKey))
            binding.nameView.text = item.name
            binding.detailView.text = item.detail
            binding.defaultBadge.isVisible = item.isDefault
            binding.editButton.isVisible = item.canEdit
            binding.deleteButton.isVisible = item.canDelete
            binding.iconCard.setCardBackgroundColor(
                ContextCompat.getColor(binding.root.context, R.color.surface_tint_soft),
            )
            binding.iconView.setColorFilter(
                ContextCompat.getColor(binding.root.context, R.color.primary_brand_dark),
            )

            binding.editButton.setOnClickListener { onEdit(item) }
            binding.deleteButton.setOnClickListener { onDelete(item) }
        }
    }

    private companion object DiffCallback : DiffUtil.ItemCallback<CategoryListItem>() {
        override fun areItemsTheSame(oldItem: CategoryListItem, newItem: CategoryListItem): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: CategoryListItem, newItem: CategoryListItem): Boolean =
            oldItem == newItem
    }
}
