package ru.ibakaidov.distypepro.utils

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import ru.ibakaidov.distypepro.R

class HashMapAdapter(
    private val context: Context,
    private val data: Map<String, String?>
) : BaseAdapter() {

    private val keys: List<String> = data.keys.toList()

    override fun getCount(): Int = data.size

    fun getEntry(position: Int): Pair<String, String?> = getKey(position) to data[getKey(position)]

    override fun getItem(position: Int): String = data[keys[position]].orEmpty()

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(
            R.layout.item_bank_entry,
            parent,
            false
        )

        val titleView: TextView = view.findViewById(R.id.title)
        titleView.text = getItem(position)
        return view
    }

    fun getKey(position: Int): String = keys[position]
}
