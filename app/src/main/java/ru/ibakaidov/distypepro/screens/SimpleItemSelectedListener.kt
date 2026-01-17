package ru.ibakaidov.distypepro.screens

import android.view.View
import android.widget.AdapterView

class SimpleItemSelectedListener(
    private val onSelected: (position: Int) -> Unit,
) : AdapterView.OnItemSelectedListener {
    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        onSelected(position)
    }

    override fun onNothingSelected(parent: AdapterView<*>?) = Unit
}
