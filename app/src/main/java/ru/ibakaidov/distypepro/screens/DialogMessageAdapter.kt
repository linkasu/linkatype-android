package ru.ibakaidov.distypepro.screens

import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.ibakaidov.distypepro.R
import ru.ibakaidov.distypepro.databinding.ItemDialogMessageBinding
import ru.ibakaidov.distypepro.shared.model.DialogMessage
import ru.ibakaidov.distypepro.shared.model.DialogRole
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class DialogMessageAdapter : ListAdapter<DialogMessage, DialogMessageAdapter.MessageViewHolder>(MessageDiffCallback()) {

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun submit(messages: List<DialogMessage>, onCommit: (() -> Unit)? = null) {
        val list = messages.toList()
        if (onCommit == null) {
            submitList(list)
        } else {
            submitList(list, onCommit)
        }
    }

    fun add(message: DialogMessage) {
        val newList = currentList.toMutableList().apply { add(message) }
        submitList(newList)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val binding = ItemDialogMessageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MessageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(getItem(position), timeFormatter)
    }

    class MessageViewHolder(
        private val binding: ItemDialogMessageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(message: DialogMessage, timeFormatter: DateTimeFormatter) {
            val context = itemView.context
            val isUser = message.role == DialogRole.DISABLED_PERSON
            val roleLabel = if (isUser) {
                context.getString(R.string.dialog_you)
            } else {
                context.getString(R.string.dialog_speaker)
            }
            val time = Instant.ofEpochMilli(message.created)
                .atZone(ZoneId.systemDefault())
                .format(timeFormatter)
            binding.messageMeta.text = "$roleLabel • $time"

            val content = message.content.ifBlank {
                context.getString(R.string.dialog_audio_message)
            }
            binding.messageText.text = content

            binding.messageContainer.gravity = if (isUser) Gravity.END else Gravity.START
            val bubbleColor = if (isUser) {
                ContextCompat.getColor(context, R.color.dialogBubbleUser)
            } else {
                ContextCompat.getColor(context, R.color.dialogBubbleSpeaker)
            }
            binding.messageBubble.setCardBackgroundColor(bubbleColor)
        }
    }

    private class MessageDiffCallback : DiffUtil.ItemCallback<DialogMessage>() {
        override fun areItemsTheSame(oldItem: DialogMessage, newItem: DialogMessage): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: DialogMessage, newItem: DialogMessage): Boolean {
            return oldItem == newItem
        }
    }
}
