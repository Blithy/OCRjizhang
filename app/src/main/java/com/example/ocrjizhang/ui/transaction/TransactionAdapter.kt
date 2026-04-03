package com.example.ocrjizhang.ui.transaction

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.ocrjizhang.R
import com.example.ocrjizhang.data.local.entity.RecordType
import com.example.ocrjizhang.databinding.ItemTransactionBinding

class TransactionAdapter(
    private val showActions: Boolean,
    private val enableLongPressDelete: Boolean = false,
    private val onOpen: ((TransactionListItem) -> Unit)? = null,
    private val onEdit: (TransactionListItem) -> Unit,
    private val onDelete: (TransactionListItem) -> Unit,
) : ListAdapter<TransactionListItem, TransactionAdapter.TransactionViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val binding = ItemTransactionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return TransactionViewHolder(
            binding = binding,
            showActions = showActions,
            enableLongPressDelete = enableLongPressDelete,
            onOpen = onOpen,
            onEdit = onEdit,
            onDelete = onDelete,
        )
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class TransactionViewHolder(
        private val binding: ItemTransactionBinding,
        private val showActions: Boolean,
        private val enableLongPressDelete: Boolean,
        private val onOpen: ((TransactionListItem) -> Unit)?,
        private val onEdit: (TransactionListItem) -> Unit,
        private val onDelete: (TransactionListItem) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: TransactionListItem) {
            binding.titleView.text = item.title
            binding.subtitleView.text = item.subtitle
            binding.metaView.text = item.meta
            binding.amountView.text = item.amountLabel
            binding.editButton.isVisible = showActions
            binding.deleteButton.isVisible = showActions
            binding.actionContainer.visibility = if (showActions) View.VISIBLE else View.GONE

            val colorRes = if (item.type == RecordType.INCOME) {
                R.color.positive
            } else {
                R.color.negative
            }
            binding.amountView.setTextColor(
                ContextCompat.getColor(binding.root.context, colorRes),
            )

            binding.root.isClickable = onOpen != null
            binding.root.isFocusable = onOpen != null
            binding.root.setOnClickListener {
                onOpen?.invoke(item)
            }
            binding.root.setOnLongClickListener {
                if (enableLongPressDelete) {
                    onDelete(item)
                    true
                } else {
                    false
                }
            }

            binding.editButton.setOnClickListener { onEdit(item) }
            binding.deleteButton.setOnClickListener { onDelete(item) }
        }
    }

    private companion object DiffCallback : DiffUtil.ItemCallback<TransactionListItem>() {
        override fun areItemsTheSame(
            oldItem: TransactionListItem,
            newItem: TransactionListItem,
        ): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: TransactionListItem,
            newItem: TransactionListItem,
        ): Boolean = oldItem == newItem
    }
}
