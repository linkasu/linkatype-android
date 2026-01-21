package ru.ibakaidov.distypepro.dialogs

import android.content.Context
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import ru.ibakaidov.distypepro.R
import ru.ibakaidov.distypepro.utils.Callback

object ContextDialog {
    fun show(context: Context, title: String, callback: Callback<ContextDialogAction>) {
        MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setItems(R.array.set_context_actions) { _, which ->
                val action = if (which == 0) ContextDialogAction.EDIT else ContextDialogAction.REMOVE
                callback.onDone(action)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}

enum class ContextDialogAction {
    EDIT,
    REMOVE
}
