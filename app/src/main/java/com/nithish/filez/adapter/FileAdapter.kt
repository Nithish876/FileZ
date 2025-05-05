package com.nithish.filez.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nithish.filez.databinding.ItemFileBinding
import com.nithish.filez.model.FileItem
import com.nithish.filez.utils.FileUtils

class FileAdapter(
    private val onItemClick: (FileItem) -> Unit,
    private val onItemLongClick: (FileItem) -> Boolean
) : ListAdapter<FileItem, FileAdapter.FileViewHolder>(FileDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val binding = ItemFileBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FileViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        val fileItem = getItem(position)
        holder.bind(fileItem)
    }

    inner class FileViewHolder(
        private val binding: ItemFileBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = getAdapterPosition()
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }

            binding.root.setOnLongClickListener {
                val position = getAdapterPosition()
                if (position != RecyclerView.NO_POSITION) {
                    return@setOnLongClickListener onItemLongClick(getItem(position))
                }
                false
            }
        }

        fun bind(fileItem: FileItem) {
            binding.apply {
                tvFileName.text = fileItem.name
                
                if (fileItem.isDirectory) {
                    ivFileIcon.setImageResource(android.R.drawable.ic_menu_more)
                    tvFileDetails.text = "Directory"
                } else {
                    // Check if it's a zip file
                    if (FileUtils.isZipFile(fileItem.file)) {
                        ivFileIcon.setImageResource(android.R.drawable.ic_menu_save)
                    } else {
                        ivFileIcon.setImageResource(android.R.drawable.ic_menu_edit)
                    }
                    
                    val size = FileUtils.getReadableFileSize(fileItem.size)
                    val date = FileUtils.getLastModifiedDate(fileItem.file)
                    tvFileDetails.text = "$size • $date"
                }
            }
        }
    }

    class FileDiffCallback : DiffUtil.ItemCallback<FileItem>() {
        override fun areItemsTheSame(oldItem: FileItem, newItem: FileItem): Boolean {
            return oldItem.path == newItem.path
        }

        override fun areContentsTheSame(oldItem: FileItem, newItem: FileItem): Boolean {
            return oldItem == newItem
        }
    }
} 