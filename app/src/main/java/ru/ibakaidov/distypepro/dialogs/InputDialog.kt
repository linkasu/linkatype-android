package ru.ibakaidov.distypepro.dialogs

import android.content.Context
import android.text.InputType
import android.view.LayoutInflater
import android.widget.EditText
import androidx.annotation.StringRes
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import ru.ibakaidov.distypepro.R
import ru.ibakaidov.distypepro.utils.Callback

object InputDialog {
    fun showDialog(
        context: Context,
        @StringRes title: Int,
        inputType: Int = InputType.TYPE_CLASS_TEXT,
        currentValue: String? = null,
        listener: Callback<String>
    ) {
        val view = LayoutInflater.from(context).inflate(R.layout.input_prompt, null)
        val input = view.findViewById<EditText>(R.id.input_prompt).apply {
            setInputType(inputType)
            currentValue?.let {
                setText(it)
                setSelection(it.length)
            }
        }

        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setView(view)
            .setPositiveButton(R.string.ok) { alert, _ ->
                val value = input.text?.toString()?.trim().orEmpty()
                if (value.isNotEmpty()) {
                    listener.onDone(value)
                } else {
                    listener.onError()
                }
                alert.dismiss()
            }
            .setNegativeButton(R.string.cancel) { alert, _ ->
                alert.dismiss()
                listener.onError()
            }
            .setOnCancelListener { listener.onError() }
            .create()

        dialog.setOnShowListener { input.requestFocus() }
        dialog.show()
    }
}
