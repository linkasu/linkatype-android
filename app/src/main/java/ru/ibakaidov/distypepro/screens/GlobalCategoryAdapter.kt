package ru.ibakaidov.distypepro.screens

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.ibakaidov.distypepro.R
import ru.ibakaidov.distypepro.databinding.ItemGlobalCategoryBinding

internal data class GlobalCategoryItem(
    val id: String,
    val label: String,
    val count: Int,
    val isImporting: Boolean = false,
)

internal class GlobalCategoryAdapter(
    private val onImport: (GlobalCategoryItem) -> Unit,
) : ListAdapter<GlobalCategoryItem, GlobalCategoryAdapter.ViewHolder>(CategoryDiffCallback()) {

    fun submit(newItems: List<GlobalCategoryItem>) {
        submitList(newItems.toList())
    }

    fun setImporting(id: String?) {
        val updated = currentList.map { item ->
            item.copy(isImporting = item.id == id)
        }
        submitList(updated)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemGlobalCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding, onImport)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    internal class ViewHolder(
        private val binding: ItemGlobalCategoryBinding,
        private val onImport: (GlobalCategoryItem) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: GlobalCategoryItem) {
            val context = binding.root.context
            binding.globalCategoryLabel.text = item.label
            binding.globalCategoryCount.text = context.resources.getQuantityString(
                R.plurals.global_import_count,
                item.count,
                item.count,
            )
            binding.globalCategoryImport.isEnabled = !item.isImporting
            binding.globalCategoryImport.text = context.getString(
                if (item.isImporting) R.string.global_import_importing else R.string.global_import_import,
            )
            binding.globalCategoryImport.setOnClickListener {
                if (!item.isImporting) {
                    onImport(item)
                }
            }
        }
    }

    private class CategoryDiffCallback : DiffUtil.ItemCallback<GlobalCategoryItem>() {
        override fun areItemsTheSame(oldItem: GlobalCategoryItem, newItem: GlobalCategoryItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: GlobalCategoryItem, newItem: GlobalCategoryItem): Boolean {
            return oldItem == newItem
        }
    }
}
