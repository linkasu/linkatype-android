package ru.ibakaidov.distypepro.screens

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import ru.ibakaidov.distypepro.R
import ru.ibakaidov.distypepro.databinding.ActivityGlobalImportBinding
import ru.ibakaidov.distypepro.shared.SharedSdkProvider

class GlobalImportActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGlobalImportBinding
    private val sdk by lazy { SharedSdkProvider.get(this) }
    private val adapter = GlobalCategoryAdapter { item -> importCategory(item) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGlobalImportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.globalImportToolbar)
        binding.globalImportToolbar.setNavigationIcon(R.drawable.ic_baseline_arrow_back_24)
        binding.globalImportToolbar.setNavigationOnClickListener { finish() }

        binding.globalImportList.layoutManager = LinearLayoutManager(this)
        binding.globalImportList.adapter = adapter

        loadCategories()
    }

    private fun loadCategories() {
        binding.globalImportProgress.visibility = View.VISIBLE
        binding.globalImportEmpty.visibility = View.GONE
        lifecycleScope.launch {
            val list = runCatching {
                sdk.globalRepository.listCategories(includeStatements = true)
            }.getOrElse { emptyList() }

            val items = list.map { category ->
                GlobalCategoryItem(
                    id = category.id,
                    label = category.label,
                    count = category.statements?.size ?: 0,
                )
            }
            adapter.submit(items)
            binding.globalImportEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
            binding.globalImportProgress.visibility = View.GONE
        }
    }

    private fun importCategory(item: GlobalCategoryItem) {
        adapter.setImporting(item.id)
        lifecycleScope.launch {
            val status = runCatching {
                sdk.globalRepository.importCategory(item.id, force = false).status
            }.getOrNull()

            val message = when (status) {
                "exists" -> getString(R.string.global_import_exists)
                "ok" -> getString(R.string.global_import_done)
                else -> getString(R.string.global_import_failed)
            }
            Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
            adapter.setImporting(null)
        }
    }
}
