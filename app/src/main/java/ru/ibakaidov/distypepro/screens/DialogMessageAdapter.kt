package ru.ibakaidov.distypepro.screens

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import ru.ibakaidov.distypepro.R
import ru.ibakaidov.distypepro.shared.model.DialogMessage

class DialogMessageAdapter : RecyclerView.Adapter<DialogMessageAdapter.MessageViewHolder>() {

    private val items = mutableListOf<DialogMessage>()

    fun submit(messages: List<DialogMessage>) {
        items.clear()
        items.addAll(messages)
        notifyDataSetChanged()
    }

    fun add(message: DialogMessage) {
        items.add(message)
        notifyItemInserted(items.lastIndex)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_dialog_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(R.id.message_text)

        fun bind(message: DialogMessage) {
            textView.text = "${message.role}: ${message.content}"
        }
    }
}
