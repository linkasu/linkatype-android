package ru.ibakaidov.distypepro.dialogs

import android.content.Context
import androidx.annotation.StringRes
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import ru.ibakaidov.distypepro.R
import ru.ibakaidov.distypepro.utils.Callback

object ConfirmDialog {
    fun showConfirmDialog(context: Context, @StringRes title: Int, callback: Callback<Unit>) {
        MaterialAlertDialogBuilder(context)
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
