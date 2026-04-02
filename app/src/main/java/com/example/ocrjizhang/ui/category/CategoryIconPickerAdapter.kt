package com.example.ocrjizhang.ui.category

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.ocrjizhang.R
import com.example.ocrjizhang.databinding.ItemCategoryIconOptionBinding

class CategoryIconPickerAdapter(
    private val onSelect: (CategoryIconOption) -> Unit,
) : RecyclerView.Adapter<CategoryIconPickerAdapter.IconOptionViewHolder>() {

    private var items: List<CategoryIconOption> = emptyList()
    private var selectedKey: String = ""

    fun submitOptions(options: List<CategoryIconOption>, selectedKey: String) {
        items = options
        this.selectedKey = selectedKey
        notifyDataSetChanged()
    }

    fun updateSelectedKey(selectedKey: String) {
        if (this.selectedKey == selectedKey) return
        this.selectedKey = selectedKey
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IconOptionViewHolder {
        val binding = ItemCategoryIconOptionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return IconOptionViewHolder(binding, onSelect)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: IconOptionViewHolder, position: Int) {
        holder.bind(items[position], items[position].key == selectedKey)
    }

    class IconOptionViewHolder(
        private val binding: ItemCategoryIconOptionBinding,
        private val onSelect: (CategoryIconOption) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CategoryIconOption, isSelected: Boolean) {
            val context = binding.root.context
            binding.iconView.setImageResource(item.iconRes)
            binding.labelView.text = item.label

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
            val iconBackground = if (isSelected) {
                R.color.primary_brand
            } else {
                R.color.surface_base
            }
            val iconTint = if (isSelected) {
                android.R.color.white
            } else {
                R.color.primary_brand_dark
            }
            val labelColor = if (isSelected) {
                R.color.primary_brand_dark
            } else {
                R.color.ink_secondary
            }

            binding.root.setCardBackgroundColor(ContextCompat.getColor(context, cardColor))
            binding.root.strokeColor = ContextCompat.getColor(context, strokeColor)
            binding.iconCard.setCardBackgroundColor(ContextCompat.getColor(context, iconBackground))
            binding.iconView.setColorFilter(ContextCompat.getColor(context, iconTint))
            binding.labelView.setTextColor(ContextCompat.getColor(context, labelColor))
            binding.root.setOnClickListener { onSelect(item) }
        }
    }
}
