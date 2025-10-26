package ru.ibakaidov.distypepro.components

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.AdapterView
import android.widget.GridView
import android.widget.ImageButton
import ru.ibakaidov.distypepro.R
import ru.ibakaidov.distypepro.data.CategoryManager
import ru.ibakaidov.distypepro.data.StatementManager
import ru.ibakaidov.distypepro.dialogs.ConfirmDialog
import ru.ibakaidov.distypepro.dialogs.ContextDialog
import ru.ibakaidov.distypepro.dialogs.ContextDialogAction
import ru.ibakaidov.distypepro.dialogs.InputDialog
import ru.ibakaidov.distypepro.utils.Callback
import ru.ibakaidov.distypepro.utils.HashMapAdapter
import ru.ibakaidov.distypepro.utils.Tts

class BankGroup @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : Component(context, attrs) {

    override fun layoutId(): Int = R.layout.bank_group

    private val categoryManager = CategoryManager()
    private var statementManager: StatementManager? = null
    private lateinit var gridView: GridView
    private lateinit var backButton: ImageButton
    private lateinit var addButton: ImageButton
    private var tts: Tts? = null
    private var showingStatements = false

    override fun initUi() {
        gridView = findViewById(R.id.gridview)
        addButton = findViewById(R.id.add_button)
        backButton = findViewById(R.id.back_button)

        backButton.setOnClickListener { setState(false) }
        addButton.setOnClickListener { showAddDialog() }

        gridView.onItemClickListener = AdapterView.OnItemClickListener { parent, _, position, _ ->
            val adapter = parent?.adapter as? HashMapAdapter ?: return@OnItemClickListener
            val key = adapter.getKey(position)
            val value = adapter.getItem(position)
            onItemSelected(key, value)
        }

        gridView.onItemLongClickListener = AdapterView.OnItemLongClickListener { parent, _, position, _ ->
            val adapter = parent?.adapter as? HashMapAdapter ?: return@OnItemLongClickListener false
            onItemLongClick(adapter.getKey(position), adapter.getItem(position))
            false
        }

        showCategories()
    }

    fun setTts(tts: Tts) {
        this.tts = tts
    }

    fun back() = setState(false)

    private fun showAddDialog() {
        InputDialog.showDialog(context, R.string.create, listener = object : Callback<String> {
            override fun onDone(result: String) {
                if (showingStatements) {
                    statementManager?.create(result, object : Callback<Unit> {
                        override fun onDone(result: Unit) = Unit
                    })
                } else {
                    categoryManager.create(result, object : Callback<Unit> {
                        override fun onDone(result: Unit) = Unit
                    })
                }
            }
        })
    }

    private fun onItemSelected(key: String, value: String) {
        if (showingStatements) {
            tts?.speak(value)
        } else {
            statementManager = StatementManager(key)
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
        backButton.visibility = if (statements) View.VISIBLE else View.GONE
        if (statements) {
            showStatements()
        } else {
            showCategories()
        }
    }

    private fun showCategories() {
        categoryManager.getList(object : Callback<Map<String, String>> {
            override fun onDone(result: Map<String, String>) {
                if (showingStatements) return
                gridView.adapter = HashMapAdapter(context, result)
            }
        })
    }

    private fun showStatements() {
        if (!showingStatements) return
        statementManager?.getList(object : Callback<Map<String, String>> {
            override fun onDone(result: Map<String, String>) {
                gridView.adapter = HashMapAdapter(context, result)
            }
        })
    }
}
