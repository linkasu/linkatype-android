package ru.ibakaidov.distypepro.dialogs

import android.app.AlertDialog
import android.content.Context
import androidx.annotation.StringRes
import ru.ibakaidov.distypepro.R
import ru.ibakaidov.distypepro.utils.Callback

object ConfirmDialog {
    fun showConfirmDialog(context: Context, @StringRes title: Int, callback: Callback<Unit>) {
        AlertDialog.Builder(context)
            .setTitle(title)
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
                callback.onError()
            }
            .setPositiveButton(R.string.ok) { dialog, _ ->
                dialog.dismiss()
                callback.onDone(Unit)
            }
            .show()
    }
}
