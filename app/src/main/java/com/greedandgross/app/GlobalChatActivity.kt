package com.greedandgross.app

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.greedandgross.app.adapters.GlobalChatAdapter
import com.greedandgross.app.models.GlobalChatMessage
import kotlin.random.Random
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.greedandgross.app.utils.LanguageManager

class GlobalChatActivity : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var inputMessage: EditText
    private lateinit var sendButton: ImageButton
    private lateinit var loadingIndicator: ProgressBar
    private lateinit var onlineUsersText: TextView
    private lateinit var chatAdapter: GlobalChatAdapter
    
    private val messages = mutableListOf<GlobalChatMessage>()
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private var currentUserName = ""
    private var messagesListener: ValueEventListener? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_global_chat)
        
        setupFirebase()
        setupViews()
        setupRecyclerView()
        authenticateUser()
    }
    
    private fun setupFirebase() {
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference
    }
    
    private fun setupViews() {
        recyclerView = findViewById(R.id.recyclerView)
        inputMessage = findViewById(R.id.inputMessage)
        sendButton = findViewById(R.id.sendButton)
        loadingIndicator = findViewById(R.id.loadingIndicator)
        onlineUsersText = findViewById(R.id.onlineUsersText)
        
        sendButton.setOnClickListener {
            sendMessage()
        }
        
        findViewById<ImageButton>(R.id.backButton)?.setOnClickListener {
            finish()
        }
    }
    
    private fun setupRecyclerView() {
        chatAdapter = GlobalChatAdapter(messages)
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@GlobalChatActivity)
            adapter = chatAdapter
        }
    }
    
    private fun authenticateUser() {
        loadingIndicator.visibility = View.VISIBLE
        addSystemMessage("🔄 Connessione a Firebase...")
        
        android.util.Log.d("GlobalChat", "Starting Firebase auth...")
        
        if (BuildConfig.DEBUG) {
            // In debug, simula connessione riuscita
            currentUserName = generateRandomUsername()
            addSystemMessage("✅ [DEBUG] Connesso come $currentUserName")
            addSystemMessage("⚠️ Chat offline - Firebase rules restrictive")
            addSystemMessage("🔧 Configura regole: global_chat: .read/.write = auth != null")
            loadingIndicator.visibility = View.GONE
            return
        }
        
        auth.signInAnonymously()
            .addOnCompleteListener(this) { task ->
                android.util.Log.d("GlobalChat", "Auth complete: ${task.isSuccessful}")
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    android.util.Log.d("GlobalChat", "User UID: ${user?.uid}")
                    
                    currentUserName = generateRandomUsername()
                    addSystemMessage("✅ Connesso come $currentUserName")
                    addSystemMessage("🔧 UID: ${user?.uid}")
                    
                    setupChatListener()
                    addUserToOnlineList()
                    
                    // Messaggio di benvenuto
                    addSystemMessage(LanguageManager.getString("chat_welcome"))
                } else {
                    val error = task.exception?.message ?: "Errore sconosciuto"
                    android.util.Log.e("GlobalChat", "Auth failed: $error", task.exception)
                    addSystemMessage("❌ Errore autenticazione: $error")
                    addSystemMessage("🔧 Verifica regole Firebase Database")
                    addSystemMessage("📋 Rules needed: global_chat: .read/.write = auth != null")
                }
                loadingIndicator.visibility = View.GONE
            }
    }
    
    private fun generateRandomUsername(): String {
        val adjectives = listOf("Expert", "Master", "Pro", "Elite", "Legendary", "Supreme", "Ultimate", "Divine")
        val nouns = listOf("Breeder", "Grower", "Cultivator", "Geneticist", "Botanist", "Farmer", "Hybridizer", "Specialist")
        val number = Random.nextInt(1000, 9999)
        
        return "${adjectives.random()}${nouns.random()}_$number"
    }
    
    private fun setupChatListener() {
        messagesListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    messages.clear()
                    for (messageSnapshot in snapshot.children) {
                        try {
                            val message = messageSnapshot.getValue(GlobalChatMessage::class.java)
                            message?.let { messages.add(it) }
                        } catch (e: Exception) {
                            android.util.Log.e("GlobalChat", "Error parsing message", e)
                        }
                    }
                
                    chatAdapter.notifyDataSetChanged()
                    if (messages.isNotEmpty()) {
                        recyclerView.scrollToPosition(messages.size - 1)
                    }
                    
                    updateOnlineUsersCount()
                } catch (e: Exception) {
                    android.util.Log.e("GlobalChat", "Error in onDataChange", e)
                    addSystemMessage("Errore aggiornamento chat: ${e.message}")
                }
            }
            
            override fun onCancelled(error: DatabaseError) {
                android.util.Log.e("GlobalChat", "Database cancelled", error.toException())
                addSystemMessage("Errore nel caricamento messaggi: ${error.message}")
            }
        }
        
        database.child("chat_messages")
            .orderByChild("timestamp")
            .limitToLast(50)
            .addValueEventListener(messagesListener!!)
    }
    
    private fun addUserToOnlineList() {
        val userRef = database.child("online_users").child(auth.currentUser?.uid ?: "")
        userRef.setValue(mapOf(
            "username" to currentUserName,
            "lastSeen" to System.currentTimeMillis()
        ))
        
        // Rimuovi utente quando disconnette
        userRef.onDisconnect().removeValue()
    }
    
    private fun updateOnlineUsersCount() {
        database.child("online_users").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val count = snapshot.childrenCount
                onlineUsersText.text = "$count breeders online"
            }
            
            override fun onCancelled(error: DatabaseError) {
                onlineUsersText.text = "Breeders online"
            }
        })
    }
    
    private fun sendMessage() {
        val message = inputMessage.text.toString().trim()
        if (message.isEmpty()) {
            addSystemMessage("⚠️ Scrivi un messaggio prima di inviare")
            return
        }
        
        if (currentUserName.isEmpty()) {
            addSystemMessage("⚠️ Connessione in corso...")
            return
        }
        
        if (auth.currentUser == null) {
            addSystemMessage("❌ Errore autenticazione, riconnettendo...")
            authenticateUser()
            return
        }
        
        lifecycleScope.launch {
            try {
                val chatMessage = GlobalChatMessage(
                    id = database.child("chat_messages").push().key ?: "",
                    username = currentUserName,
                    message = message,
                    timestamp = System.currentTimeMillis(),
                    userId = auth.currentUser?.uid ?: ""
                )
                
                database.child("chat_messages").child(chatMessage.id).setValue(chatMessage)
                    .addOnSuccessListener {
                        android.util.Log.d("GlobalChat", "Message sent successfully")
                        inputMessage.text.clear()
                    }
                    .addOnFailureListener { e ->
                        android.util.Log.e("GlobalChat", "Failed to send message", e)
                        addSystemMessage("❌ Errore invio: ${e.message}")
                    }
            } catch (e: Exception) {
                android.util.Log.e("GlobalChat", "Send message exception", e)
                addSystemMessage("❌ Errore: ${e.message}")
            }
        }
    }
    
    private fun addSystemMessage(message: String) {
        val systemMessage = GlobalChatMessage(
            id = "system_${System.currentTimeMillis()}",
            username = "Sistema",
            message = message,
            timestamp = System.currentTimeMillis(),
            userId = "system",
            isSystem = true
        )
        
        messages.add(systemMessage)
        chatAdapter.notifyItemInserted(messages.size - 1)
        recyclerView.scrollToPosition(messages.size - 1)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        try {
            messagesListener?.let {
                database.child("chat_messages").removeEventListener(it)
            }
            // Rimuovi utente dalla lista online
            auth.currentUser?.uid?.let { uid ->
                database.child("online_users").child(uid).removeValue()
            }
        } catch (e: Exception) {
            android.util.Log.e("GlobalChat", "Error in onDestroy", e)
        }
    }
}