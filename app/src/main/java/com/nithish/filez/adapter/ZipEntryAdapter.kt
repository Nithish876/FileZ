package com.nithish.filez.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nithish.filez.databinding.ItemZipEntryBinding
import com.nithish.filez.model.ZipEntryItem

class ZipEntryAdapter(
    private val onItemClick: (ZipEntryItem) -> Unit
) : ListAdapter<ZipEntryItem, ZipEntryAdapter.ZipEntryViewHolder>(ZipEntryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ZipEntryViewHolder {
        val binding = ItemZipEntryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ZipEntryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ZipEntryViewHolder, position: Int) {
        val zipEntry = getItem(position)
        holder.bind(zipEntry)
    }

    inner class ZipEntryViewHolder(
        private val binding: ItemZipEntryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = getAdapterPosition()
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }
        }

        fun bind(zipEntry: ZipEntryItem) {
            binding.apply {
                // Apply indentation based on depth
                val indentPx = (16 * zipEntry.depth).toFloat()
                root.setPadding(indentPx.toInt(), root.paddingTop, root.paddingRight, root.paddingBottom)
                
                tvZipEntryName.text = zipEntry.name
                
                if (zipEntry.isDirectory) {
                    ivZipEntryIcon.setImageResource(android.R.drawable.ic_menu_more)
                } else {
                    ivZipEntryIcon.setImageResource(android.R.drawable.ic_menu_edit)
                }
            }
        }
    }

    class ZipEntryDiffCallback : DiffUtil.ItemCallback<ZipEntryItem>() {
        override fun areItemsTheSame(oldItem: ZipEntryItem, newItem: ZipEntryItem): Boolean {
            return oldItem.path == newItem.path
        }

        override fun areContentsTheSame(oldItem: ZipEntryItem, newItem: ZipEntryItem): Boolean {
            return oldItem == newItem
        }
    }
} 