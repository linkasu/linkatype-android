package ru.ibakaidov.distypepro.utils

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.ibakaidov.distypepro.databinding.ItemBankEntryBinding

class HashMapRecyclerAdapter(
    private val onItemClick: (key: String, value: String) -> Unit,
    private val onItemLongClick: (key: String, value: String) -> Unit,
) : ListAdapter<HashMapRecyclerAdapter.Entry, HashMapRecyclerAdapter.ViewHolder>(EntryDiffCallback()) {

    data class Entry(
        val key: String,
        val value: String,
    )

    fun submitMap(data: Map<String, String>) {
        val entries = data.map { (key, value) -> Entry(key, value) }
        submitList(entries)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemBankEntryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding, onItemClick, onItemLongClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemBankEntryBinding,
        private val onItemClick: (key: String, value: String) -> Unit,
        private val onItemLongClick: (key: String, value: String) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(entry: Entry) {
            binding.title.text = entry.value
            binding.root.setOnClickListener {
                onItemClick(entry.key, entry.value)
            }
            binding.root.setOnLongClickListener {
                onItemLongClick(entry.key, entry.value)
                true
            }
        }
    }

    private class EntryDiffCallback : DiffUtil.ItemCallback<Entry>() {
        override fun areItemsTheSame(oldItem: Entry, newItem: Entry): Boolean {
            return oldItem.key == newItem.key
        }

        override fun areContentsTheSame(oldItem: Entry, newItem: Entry): Boolean {
            return oldItem == newItem
        }
    }
}
