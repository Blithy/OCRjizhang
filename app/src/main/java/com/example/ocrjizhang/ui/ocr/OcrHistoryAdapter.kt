package com.example.ocrjizhang.ui.ocr

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.ocrjizhang.databinding.ItemOcrHistoryBinding

class OcrHistoryAdapter(
    private val onUseRecord: (OcrHistoryUiModel) -> Unit,
) : ListAdapter<OcrHistoryUiModel, OcrHistoryAdapter.OcrHistoryViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OcrHistoryViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemOcrHistoryBinding.inflate(inflater, parent, false)
        return OcrHistoryViewHolder(binding, onUseRecord)
    }

    override fun onBindViewHolder(holder: OcrHistoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class OcrHistoryViewHolder(
        private val binding: ItemOcrHistoryBinding,
        private val onUseRecord: (OcrHistoryUiModel) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: OcrHistoryUiModel) {
            binding.titleText.text = item.title
            binding.subtitleText.text = item.subtitle
            binding.metaText.text = item.meta
            binding.useButton.setOnClickListener {
                onUseRecord(item)
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<OcrHistoryUiModel>() {
        override fun areItemsTheSame(oldItem: OcrHistoryUiModel, newItem: OcrHistoryUiModel): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: OcrHistoryUiModel, newItem: OcrHistoryUiModel): Boolean =
            oldItem == newItem
    }
}
