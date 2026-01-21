package ru.ibakaidov.distypepro.screens

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.ibakaidov.distypepro.R
import ru.ibakaidov.distypepro.databinding.ItemDialogChatBinding
import ru.ibakaidov.distypepro.shared.model.DialogChat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class DialogChatAdapter(
    private val onSelect: (DialogChat) -> Unit,
    private val onDelete: (DialogChat) -> Unit,
) : ListAdapter<DialogChatAdapter.ChatItem, DialogChatAdapter.ChatViewHolder>(ChatDiffCallback()) {

    private val timeFormatter = DateTimeFormatter.ofPattern("dd MMM, HH:mm")

    data class ChatItem(
        val chat: DialogChat,
        val isSelected: Boolean,
    )

    fun submit(chats: List<DialogChat>, activeChatId: String?) {
        val items = chats.map { chat ->
            ChatItem(chat = chat, isSelected = chat.id == activeChatId)
        }
        submitList(items)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val binding = ItemDialogChatBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ChatViewHolder(binding, onSelect, onDelete)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(getItem(position), timeFormatter)
    }

    class ChatViewHolder(
        private val binding: ItemDialogChatBinding,
        private val onSelect: (DialogChat) -> Unit,
        private val onDelete: (DialogChat) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ChatItem, formatter: DateTimeFormatter) {
            val context = binding.root.context
            val chat = item.chat

            binding.chatTitle.text = chat.title ?: context.getString(R.string.dialog_untitled)

            val lastMessageAt = chat.lastMessageAt
            binding.chatSubtitle.text = when {
                lastMessageAt != null -> {
                    Instant.ofEpochMilli(lastMessageAt)
                        .atZone(ZoneId.systemDefault())
                        .format(formatter)
                }
                chat.messageCount != null -> context.getString(R.string.dialog_message_count, chat.messageCount)
                else -> context.getString(R.string.dialog_empty)
            }

            val strokeColor = if (item.isSelected) {
                ContextCompat.getColor(context, R.color.colorPrimary)
            } else {
                ContextCompat.getColor(context, R.color.colorOutlineVariant)
            }
            binding.chatCard.strokeColor = strokeColor
            binding.chatCard.setOnClickListener { onSelect(chat) }
            binding.chatDelete.setOnClickListener { onDelete(chat) }
        }
    }

    private class ChatDiffCallback : DiffUtil.ItemCallback<ChatItem>() {
        override fun areItemsTheSame(oldItem: ChatItem, newItem: ChatItem): Boolean {
            return oldItem.chat.id == newItem.chat.id
        }

        override fun areContentsTheSame(oldItem: ChatItem, newItem: ChatItem): Boolean {
            return oldItem == newItem
        }
    }
}
