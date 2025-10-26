package ru.ibakaidov.distypepro.dialogs

import android.app.AlertDialog
import android.content.Context
import ru.ibakaidov.distypepro.R
import ru.ibakaidov.distypepro.utils.Callback

object ContextDialog {
    fun show(context: Context, title: String, callback: Callback<ContextDialogAction>) {
        AlertDialog.Builder(context)
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
