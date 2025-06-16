package com.greedandgross.app.adapters

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.greedandgross.app.R
import com.greedandgross.app.models.GlobalChatMessage
import java.text.SimpleDateFormat
import java.util.*

class GlobalChatAdapter(private val messages: List<GlobalChatMessage>) : 
    RecyclerView.Adapter<GlobalChatAdapter.MessageViewHolder>() {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_global_chat_message, parent, false)
        return MessageViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(messages[position])
    }
    
    override fun getItemCount() = messages.size
    
    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val usernameText: TextView = itemView.findViewById(R.id.usernameText)
        private val messageText: TextView = itemView.findViewById(R.id.messageText)
        private val timeText: TextView = itemView.findViewById(R.id.timeText)
        
        fun bind(message: GlobalChatMessage) {
            if (message.isSystem) {
                usernameText.text = "🤖 Sistema"
                usernameText.setTextColor(itemView.context.getColor(R.color.green_primary))
                messageText.setTypeface(null, Typeface.ITALIC)
                messageText.setTextColor(itemView.context.getColor(R.color.text_secondary))
            } else {
                usernameText.text = message.username
                usernameText.setTextColor(itemView.context.getColor(R.color.text_primary))
                messageText.setTypeface(null, Typeface.NORMAL)
                messageText.setTextColor(itemView.context.getColor(R.color.text_primary))
            }
            
            messageText.text = message.message
            timeText.text = formatTime(message.timestamp)
            
            // Long press per copiare messaggio
            messageText.setOnLongClickListener {
                copyToClipboard(itemView.context, message.message)
                true
            }
        }
        
        private fun formatTime(timestamp: Long): String {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
        
        private fun copyToClipboard(context: Context, text: String) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Greed&Gross", text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "Messaggio copiato!", Toast.LENGTH_SHORT).show()
        }
    }
}