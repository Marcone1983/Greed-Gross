package com.greedandgross.app.adapters

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.greedandgross.app.R
import com.greedandgross.app.models.ChatMessage
import java.text.SimpleDateFormat
import java.util.*

class ChatAdapter(private val messages: List<ChatMessage>) : 
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    
    companion object {
        private const val VIEW_TYPE_USER = 1
        private const val VIEW_TYPE_BOT = 2
        
        private fun formatTime(timestamp: Long): String {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
    }
    
    override fun getItemViewType(position: Int): Int {
        return if (messages[position].isUser) VIEW_TYPE_USER else VIEW_TYPE_BOT
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_USER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_chat_user, parent, false)
                UserMessageViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_chat_bot, parent, false)
                BotMessageViewHolder(view)
            }
        }
    }
    
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        when (holder) {
            is UserMessageViewHolder -> holder.bind(message)
            is BotMessageViewHolder -> holder.bind(message)
        }
    }
    
    override fun getItemCount() = messages.size
    
    class UserMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.messageText)
        private val timeText: TextView = itemView.findViewById(R.id.timeText)
        
        fun bind(message: ChatMessage) {
            messageText.text = message.text
            timeText.text = ChatAdapter.formatTime(message.timestamp)
        }
    }
    
    class BotMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.messageText)
        private val timeText: TextView = itemView.findViewById(R.id.timeText)
        private val imageView: ImageView = itemView.findViewById(R.id.imageView)
        
        fun bind(message: ChatMessage) {
            if (message.isImage && !message.imageUrl.isNullOrEmpty()) {
                messageText.visibility = View.GONE
                imageView.visibility = View.VISIBLE
                Glide.with(itemView.context)
                    .load(message.imageUrl)
                    .into(imageView)
            } else {
                messageText.visibility = View.VISIBLE
                imageView.visibility = View.GONE
                messageText.text = message.text
                
                // Long press per copiare messaggio
                messageText.setOnLongClickListener {
                    copyToClipboard(itemView.context, message.text)
                    true
                }
            }
            timeText.text = ChatAdapter.formatTime(message.timestamp)
        }
        
        private fun copyToClipboard(context: Context, text: String) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Greed&Gross", text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "Testo copiato!", Toast.LENGTH_SHORT).show()
        }
    }
}