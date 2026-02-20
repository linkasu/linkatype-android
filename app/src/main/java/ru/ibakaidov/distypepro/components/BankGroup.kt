package ru.ibakaidov.distypepro.components

import android.content.Intent
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import ru.ibakaidov.distypepro.R
import ru.ibakaidov.distypepro.bank.SortMode
import ru.ibakaidov.distypepro.bank.sortEntries
import ru.ibakaidov.distypepro.data.CategoryManager
import ru.ibakaidov.distypepro.data.StatementManager
import ru.ibakaidov.distypepro.dialogs.ConfirmDialog
import ru.ibakaidov.distypepro.dialogs.ContextDialog
import ru.ibakaidov.distypepro.dialogs.ContextDialogAction
import ru.ibakaidov.distypepro.dialogs.InputDialog
import ru.ibakaidov.distypepro.screens.GlobalImportActivity
import ru.ibakaidov.distypepro.utils.Callback
import ru.ibakaidov.distypepro.utils.HashMapRecyclerAdapter
import ru.ibakaidov.distypepro.utils.Tts

class BankGroup @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : Component(context, attrs) {

    override fun layoutId(): Int = R.layout.bank_group

    private val categoryManager = CategoryManager(context)
    private var statementManager: StatementManager? = null
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyStateText: TextView
    private lateinit var adapter: HashMapRecyclerAdapter
    private var tts: Tts? = null
    private var toolbarStateListener: ((ToolbarState) -> Unit)? = null

    private var showingStatements = false
    private var currentStatements: Map<String, String> = emptyMap()
    private var currentCategoryId: String? = null
    private var currentCategoryTitle: String = ""
    private var isDownloading = false

    private val preferences = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
    private var sortMode: SortMode =
        SortMode.entries.getOrElse(preferences.getInt(PREF_SORT_MODE, SortMode.ALPHABET_ASC.ordinal)) {
            SortMode.ALPHABET_ASC
        }

    override fun initUi() {
        recyclerView = findViewById(R.id.gridview)
        emptyStateText = findViewById(R.id.bank_empty_state)

        adapter = HashMapRecyclerAdapter(
            onItemClick = { key, value -> onItemSelected(key, value) },
            onItemLongClick = { key, value -> onItemLongClick(key, value) }
        )

        val spanCount = resources.getInteger(R.integer.bank_grid_columns)
        recyclerView.layoutManager = GridLayoutManager(context, spanCount)
        recyclerView.adapter = adapter

        notifyToolbarStateChanged()
        showCategories()
    }

    fun setTts(tts: Tts) {
        this.tts = tts
    }

    fun setToolbarStateListener(listener: (ToolbarState) -> Unit) {
        toolbarStateListener = listener
        notifyToolbarStateChanged()
    }

    fun toolbarState(): ToolbarState {
        return ToolbarState(
            title = if (showingStatements) {
                context.getString(R.string.bank_toolbar_title_statements, currentCategoryTitle)
            } else {
                context.getString(R.string.bank_toolbar_title_categories)
            },
            showingStatements = showingStatements,
            isDownloading = isDownloading,
        )
    }

    fun back(): Boolean {
        if (!showingStatements) {
            return false
        }
        setState(false)
        return true
    }

    fun refresh() {
        if (showingStatements) {
            showStatements()
        } else {
            showCategories()
        }
    }

    fun onSortClicked() {
        showSortDialog()
    }

    fun onAddClicked() {
        showAddDialog()
    }

    fun onDownloadCacheClicked() {
        downloadCurrentCategoryToCache()
    }

    fun onImportGlobalClicked() {
        context.startActivity(Intent(context, GlobalImportActivity::class.java))
    }

    private fun showAddDialog() {
        InputDialog.showDialog(context, R.string.create, listener = object : Callback<String> {
            override fun onDone(result: String) {
                if (showingStatements) {
                    statementManager?.create(result, object : Callback<Unit> {
                        override fun onDone(result: Unit) {
                            refresh()
                        }
                    })
                } else {
                    categoryManager.create(result, object : Callback<Unit> {
                        override fun onDone(result: Unit) {
                            refresh()
                        }
                    })
                }
            }
        })
    }

    private fun onItemSelected(key: String, value: String) {
        if (showingStatements) {
            tts?.speak(value)
        } else {
            currentCategoryId = key
            currentCategoryTitle = value
            statementManager = StatementManager(context, key)
            setState(true)
        }
    }

    private fun onItemLongClick(key: String, value: String) {
        ContextDialog.show(context, value, object : Callback<ContextDialogAction> {
            override fun onDone(result: ContextDialogAction) {
                when (result) {
                    ContextDialogAction.EDIT -> showEditDialog(key, value)
                    ContextDialogAction.REMOVE -> confirmRemoval(key)
                }
            }
        })
    }

    private fun showEditDialog(key: String, value: String) {
        InputDialog.showDialog(context, R.string.edit, currentValue = value, listener = object : Callback<String> {
            override fun onDone(result: String) {
                if (showingStatements) {
                    statementManager?.edit(key, result, object : Callback<Unit> {
                        override fun onDone(result: Unit) = Unit
                    })
                } else {
                    categoryManager.edit(key, result, object : Callback<Unit> {
                        override fun onDone(result: Unit) = Unit
                    })
                }
            }
        })
    }

    private fun confirmRemoval(key: String) {
        ConfirmDialog.showConfirmDialog(context, R.string.remove, object : Callback<Unit> {
            override fun onDone(result: Unit) {
                if (showingStatements) {
                    statementManager?.remove(key, object : Callback<Unit> {
                        override fun onDone(result: Unit) = Unit
                    })
                } else {
                    categoryManager.remove(key, object : Callback<Unit> {
                        override fun onDone(result: Unit) = Unit
                    })
                }
            }
        })
    }

    private fun setState(statements: Boolean) {
        showingStatements = statements
        if (!statements) {
            currentStatements = emptyMap()
            currentCategoryId = null
            currentCategoryTitle = ""
        }
        notifyToolbarStateChanged()
        if (statements) {
            showStatements()
        } else {
            showCategories()
        }
    }

    private fun notifyToolbarStateChanged() {
        toolbarStateListener?.invoke(toolbarState())
    }

    private fun showCategories() {
        categoryManager.getList(object : Callback<Map<String, String>> {
            override fun onDone(result: Map<String, String>) {
                if (showingStatements) return
                val sorted = sortEntries(result, sortMode)
                renderEntries(sorted)
            }
        })
    }

    private fun showStatements() {
        if (!showingStatements) return
        statementManager?.getList(object : Callback<Map<String, String>> {
            override fun onDone(result: Map<String, String>) {
                currentStatements = sortEntries(result, sortMode)
                renderEntries(currentStatements)
            }
        })
    }

    private fun renderEntries(entries: Map<String, String>) {
        adapter.submitMap(entries)
        recyclerView.scheduleLayoutAnimation()
        val isEmpty = entries.isEmpty()
        recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
        emptyStateText.visibility = if (isEmpty) View.VISIBLE else View.GONE
        emptyStateText.text = if (showingStatements) {
            context.getString(R.string.bank_empty_statements)
        } else {
            context.getString(R.string.bank_empty_categories)
        }
    }

    private fun showSortDialog() {
        val options = arrayOf(
            context.getString(R.string.bank_sort_alpha_asc),
            context.getString(R.string.bank_sort_alpha_desc)
        )
        AlertDialog.Builder(context)
            .setTitle(R.string.bank_action_sort)
            .setSingleChoiceItems(options, sortMode.ordinal) { dialog, which ->
                sortMode = SortMode.entries[which]
                preferences.edit().putInt(PREF_SORT_MODE, sortMode.ordinal).apply()
                dialog.dismiss()
                if (showingStatements) {
                    showStatements()
                } else {
                    showCategories()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun downloadCurrentCategoryToCache() {
        val ttsInstance = tts ?: run {
            Toast.makeText(context, R.string.bank_download_cache_error_tts, Toast.LENGTH_SHORT).show()
            return
        }
        val phrases = currentStatements.values.toList()
        if (phrases.isEmpty()) {
            Toast.makeText(context, R.string.bank_download_cache_empty, Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_download_cache, this, false)
        val progressBar = dialogView.findViewById<ProgressBar>(R.id.cacheProgressBar)
        val progressText = dialogView.findViewById<TextView>(R.id.cacheProgressText)
        progressBar.max = phrases.size
        progressText.text = context.getString(R.string.bank_download_cache_progress, 0, phrases.size)

        val dialog = AlertDialog.Builder(context)
            .setTitle(R.string.bank_download_cache_title)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialog.show()
        isDownloading = true
        notifyToolbarStateChanged()
        Firebase.analytics.logEvent("download_category_cache", null)

        ttsInstance.downloadPhrasesToCache(phrases, "current") { current, total ->
            progressBar.progress = current
            progressText.text = context.getString(R.string.bank_download_cache_progress, current, total)
            if (current >= total) {
                dialog.dismiss()
                isDownloading = false
                notifyToolbarStateChanged()
                Toast.makeText(context, R.string.bank_download_cache_done, Toast.LENGTH_LONG).show()
            }
        }
    }

    data class ToolbarState(
        val title: String,
        val showingStatements: Boolean,
        val isDownloading: Boolean,
    )

    companion object {
        private const val PREF_SORT_MODE = "bank_sort_mode"
    }
}
