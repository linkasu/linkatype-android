package ru.ibakaidov.distypepro.screens

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import ru.ibakaidov.distypepro.R
import ru.ibakaidov.distypepro.databinding.ItemGlobalCategoryBinding

internal data class GlobalCategoryItem(
    val id: String,
    val label: String,
    val count: Int,
)

internal class GlobalCategoryAdapter(
    private val onImport: (GlobalCategoryItem) -> Unit,
) : RecyclerView.Adapter<GlobalCategoryAdapter.ViewHolder>() {

    private val items = mutableListOf<GlobalCategoryItem>()
    private var importingId: String? = null

    fun submit(newItems: List<GlobalCategoryItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun setImporting(id: String?) {
        importingId = id
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemGlobalCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding, onImport)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], importingId)
    }

    override fun getItemCount(): Int = items.size

    internal class ViewHolder(
        private val binding: ItemGlobalCategoryBinding,
        private val onImport: (GlobalCategoryItem) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: GlobalCategoryItem, importingId: String?) {
            val context = binding.root.context
            binding.globalCategoryLabel.text = item.label
            binding.globalCategoryCount.text = context.resources.getQuantityString(
                R.plurals.global_import_count,
                item.count,
                item.count,
            )
            val isImporting = importingId == item.id
            binding.globalCategoryImport.isEnabled = !isImporting
            binding.globalCategoryImport.text = context.getString(
                if (isImporting) R.string.global_import_importing else R.string.global_import_import,
            )
            binding.globalCategoryImport.setOnClickListener {
                if (!isImporting) {
                    onImport(item)
                }
            }
        }
    }
}
