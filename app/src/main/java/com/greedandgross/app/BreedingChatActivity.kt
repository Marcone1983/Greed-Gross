package com.greedandgross.app

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.greedandgross.app.adapters.ChatAdapter
import com.greedandgross.app.models.ChatMessage
import com.greedandgross.app.models.SavedStrain
import com.greedandgross.app.network.ApiClient
import kotlinx.coroutines.*
import androidx.lifecycle.lifecycleScope
import com.greedandgross.app.utils.LanguageManager
import com.greedandgross.app.BuildConfig

class BreedingChatActivity : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var inputMessage: EditText
    private lateinit var sendButton: ImageButton
    private lateinit var loadingIndicator: ProgressBar
    private lateinit var chatAdapter: ChatAdapter
    private val messages = mutableListOf<ChatMessage>()
    private val apiClient = ApiClient()
    private lateinit var prefs: SharedPreferences
    private var isTrialUsed = false
    private var lastGeneratedStrain: Triple<String, String, String>? = null // (originalMessage, analysis, imageUrl)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_breeding_chat)
        
        prefs = getSharedPreferences("greed_gross_prefs", MODE_PRIVATE)
        isTrialUsed = prefs.getBoolean("trial_used", false)
        
        setupViews()
        loadSavedChat()
        setupRecyclerView()
        
        // Messaggio di benvenuto
        val welcomeMessage = LanguageManager.getString("welcome_breeding")
        
        addMessage(ChatMessage(
            welcomeMessage,
            false,
            System.currentTimeMillis()
        ))
    }
    
    private fun setupViews() {
        recyclerView = findViewById(R.id.recyclerView)
        inputMessage = findViewById(R.id.inputMessage)
        sendButton = findViewById(R.id.sendButton)
        loadingIndicator = findViewById(R.id.loadingIndicator)
        
        sendButton.setOnClickListener {
            sendMessage()
        }
        
        findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            finish()
        }
    }
    
    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter(messages)
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@BreedingChatActivity)
            adapter = chatAdapter
        }
    }
    
    private fun sendMessage() {
        val message = inputMessage.text.toString().trim()
        if (message.isEmpty()) return
        
        if (BuildConfig.DEBUG) {
            continueWithMessage()
            return
        }
        
        // Check if Marcone admin
        checkIfMarconeAdmin { isMarcone ->
            if (isMarcone) {
                continueWithMessage()
            } else if (isTrialUsed) {
                startActivity(Intent(this@BreedingChatActivity, PaywallActivity::class.java))
            } else {
                continueWithMessage()
            }
        }
    }
    
    private fun continueWithMessage() {
        val message = inputMessage.text.toString().trim()
        
        // Aggiungi messaggio utente
        addMessage(ChatMessage(message, true, System.currentTimeMillis()))
        inputMessage.text.clear()
        
        // Mostra loading
        loadingIndicator.visibility = View.VISIBLE
        sendButton.isEnabled = false
        
        // VERA CHIAMATA API OPENAI - NON SIMULAZIONE!
        lifecycleScope.launch {
            try {
                addMessage(ChatMessage(LanguageManager.getString("analyzing"), false, System.currentTimeMillis()))
                
                // Rimuovo debug API key - ora funziona!
                
                // SEMPRE usa analyzeBreedingMessage - è più intelligente!
                val response = apiClient.analyzeBreedingMessage(message)
                addMessage(ChatMessage(response, false, System.currentTimeMillis()))
                
                // Se il messaggio sembra un incrocio, genera anche l'immagine
                if (message.contains("x", true) || message.contains("incrocia", true) || message.contains("cross", true)) {
                    addMessage(ChatMessage(LanguageManager.getString("generating_image"), false, System.currentTimeMillis()))
                    val imageUrl = apiClient.generateStrainImage("hybrid cannabis strain based on: $message")
                    if (imageUrl != null) {
                        addMessage(ChatMessage(
                            "Immagine generata:",
                            false,
                            System.currentTimeMillis(),
                            isImage = true,
                            imageUrl = imageUrl
                        ))
                        
                        // Offri di salvare lo strain
                        addMessage(ChatMessage(
                            LanguageManager.getString("save_strain_prompt"),
                            false,
                            System.currentTimeMillis()
                        ))
                        
                        // Store per salvare dopo
                        lastGeneratedStrain = Triple(message, response, imageUrl)
                    } else {
                        addMessage(ChatMessage("❌ Errore generazione immagine", false, System.currentTimeMillis()))
                    }
                }
                
                // Check se vuole salvare uno strain
                if (message.startsWith("salva", true) && lastGeneratedStrain != null) {
                    val strainName = message.removePrefix("salva").trim()
                    saveStrain(strainName, lastGeneratedStrain!!)
                }
            } catch (e: Exception) {
                android.util.Log.e("BreedingChat", "Error in breeding analysis", e)
                addMessage(ChatMessage(
                    "Errore: ${e.message}\n\nControlla:\n1. Connessione internet\n2. API key configurata\n3. Firebase funzionante",
                    false,
                    System.currentTimeMillis()
                ))
            }
            
            // Marca trial come usato 
            val currentUser = FirebaseAuth.getInstance().currentUser
            val isOwner = if (BuildConfig.DEBUG) {
                true // In debug tutti possono usare
            } else {
                currentUser?.uid == "eqDvGiUzc6SZQS4FUuvQi0jTMhy1" // In produzione solo Marcone
            }
            if (!isTrialUsed && !isOwner) {
                prefs.edit().putBoolean("trial_used", true).apply()
                isTrialUsed = true
                
                // Messaggio di avviso trial finito
                addMessage(ChatMessage(
                    "🎁 Prova gratuita terminata!\n💎 Abbonati per incroci illimitati con AI.",
                    false,
                    System.currentTimeMillis()
                ))
            }
            
            loadingIndicator.visibility = View.GONE
            sendButton.isEnabled = true
        }
    }
    
    private fun parseStrains(message: String): List<String> {
        // GPT-4 CONOSCE GIÀ TUTTI GLI STRAIN - NON SERVE DATABASE!
        // Parsing intelligente del messaggio per estrarre nomi strain
        val cleanMessage = message.lowercase()
        val strains = mutableListOf<String>()
        
        // Pattern comuni per identificare strain nei messaggi
        val patterns = listOf(
            "incrocia\\s+([^\\s]+(?:\\s+[^\\s]+)?)\\s+con\\s+([^\\s]+(?:\\s+[^\\s]+)?)".toRegex(),
            "([^\\s]+(?:\\s+[^\\s]+)?)\\s+x\\s+([^\\s]+(?:\\s+[^\\s]+)?)".toRegex(),
            "([^\\s]+(?:\\s+[^\\s]+)?)\\s+e\\s+([^\\s]+(?:\\s+[^\\s]+)?)".toRegex()
        )
        
        for (pattern in patterns) {
            val match = pattern.find(cleanMessage)
            if (match != null) {
                strains.add(match.groupValues[1].trim())
                strains.add(match.groupValues[2].trim())
                break
            }
        }
        
        // Se non trova pattern, passa tutto il messaggio a GPT-4 che è intelligente
        if (strains.isEmpty()) {
            strains.add(message)
        }
        
        return strains.distinct()
    }
    
    
    private fun addMessage(message: ChatMessage) {
        messages.add(message)
        chatAdapter.notifyItemInserted(messages.size - 1)
        recyclerView.scrollToPosition(messages.size - 1)
        saveChatToPrefs()
    }
    
    private fun saveChatToPrefs() {
        val chatJson = org.json.JSONArray()
        messages.forEach { message ->
            val messageJson = org.json.JSONObject()
            messageJson.put("text", message.text)
            messageJson.put("isUser", message.isUser)
            messageJson.put("timestamp", message.timestamp)
            chatJson.put(messageJson)
        }
        prefs.edit().putString("saved_chat", chatJson.toString()).apply()
    }
    
    private fun loadSavedChat() {
        val savedChatString = prefs.getString("saved_chat", null)
        if (savedChatString != null) {
            try {
                val chatJson = org.json.JSONArray(savedChatString)
                for (i in 0 until chatJson.length()) {
                    val messageJson = chatJson.getJSONObject(i)
                    val message = ChatMessage(
                        messageJson.getString("text"),
                        messageJson.getBoolean("isUser"),
                        messageJson.getLong("timestamp")
                    )
                    messages.add(message)
                }
                chatAdapter.notifyDataSetChanged()
                if (messages.isNotEmpty()) {
                    recyclerView.scrollToPosition(messages.size - 1)
                }
            } catch (e: Exception) {
                android.util.Log.e("BreedingChat", "Error loading saved chat", e)
            }
        }
    }
    
    private fun saveStrain(name: String, strainData: Triple<String, String, String>) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            addMessage(ChatMessage("❌ Devi essere connesso per salvare strain", false, System.currentTimeMillis()))
            return
        }
        
        if (name.isBlank()) {
            addMessage(ChatMessage("❌ Inserisci un nome per lo strain! Es: 'salva Purple Dream'", false, System.currentTimeMillis()))
            return
        }
        
        addMessage(ChatMessage("💾 Salvando strain...", false, System.currentTimeMillis()))
        
        val (originalMessage, analysis, imageUrl) = strainData
        val strainId = FirebaseDatabase.getInstance().reference.push().key ?: return
        
        // Parse parent strains from original message
        val parents = parseParentStrains(originalMessage)
        
        // Estrai tutti i dati dalla risposta GPT
        val extractedData = extractStrainData(analysis)
        
        val savedStrain = SavedStrain(
            id = strainId,
            name = name,
            description = analysis,
            imageUrl = imageUrl,
            parentStrain1 = parents.first,
            parentStrain2 = parents.second,
            characteristics = extractedData["characteristics"] ?: "",
            thcContent = extractedData["thc"] ?: "",
            cbdContent = extractedData["cbd"] ?: "",
            phenotype = extractedData["phenotype"] ?: "Hybrid",
            floweringTime = extractedData["flowering"] ?: "",
            yield = extractedData["yield"] ?: "",
            effects = extractedData["effects"] ?: "",
            terpenes = extractedData["terpenes"] ?: "",
            medicalUse = extractedData["medical"] ?: "",
            createdBy = userId,
            createdAt = System.currentTimeMillis()
        )
        
        FirebaseDatabase.getInstance().reference
            .child("user_strains")
            .child(userId)
            .child(strainId)
            .setValue(savedStrain)
            .addOnSuccessListener {
                addMessage(ChatMessage("✅ Strain '$name' salvato nella tua collezione!", false, System.currentTimeMillis()))
                lastGeneratedStrain = null // Reset
            }
            .addOnFailureListener { e ->
                addMessage(ChatMessage("❌ Errore salvataggio: ${e.message}", false, System.currentTimeMillis()))
            }
    }
    
    private fun extractStrainData(analysis: String): Map<String, String> {
        val data = mutableMapOf<String, String>()
        
        // Estrai THC
        val thcPattern = "THC[:：]?\\s*([0-9]+[.,]?[0-9]*[-–]?[0-9]*[.,]?[0-9]*%?)".toRegex(RegexOption.IGNORE_CASE)
        thcPattern.find(analysis)?.let { data["thc"] = it.groupValues[1] }
        
        // Estrai CBD
        val cbdPattern = "CBD[:：]?\\s*([0-9]+[.,]?[0-9]*[-–]?[0-9]*[.,]?[0-9]*%?)".toRegex(RegexOption.IGNORE_CASE)
        cbdPattern.find(analysis)?.let { data["cbd"] = it.groupValues[1] }
        
        // Estrai fenotipo (Indica/Sativa/Hybrid)
        when {
            analysis.contains("sativa dominant", ignoreCase = true) -> data["phenotype"] = "Sativa Dominant"
            analysis.contains("indica dominant", ignoreCase = true) -> data["phenotype"] = "Indica Dominant"
            analysis.contains("50/50", ignoreCase = true) -> data["phenotype"] = "50/50 Hybrid"
            analysis.contains("hybrid", ignoreCase = true) -> data["phenotype"] = "Hybrid"
            analysis.contains("sativa", ignoreCase = true) -> data["phenotype"] = "Sativa"
            analysis.contains("indica", ignoreCase = true) -> data["phenotype"] = "Indica"
        }
        
        // Estrai tempo fioritura
        val floweringPattern = "(?:flowering|fioritura)[:：]?\\s*([0-9]+[-–]?[0-9]*\\s*(?:weeks?|settimane?))".toRegex(RegexOption.IGNORE_CASE)
        floweringPattern.find(analysis)?.let { data["flowering"] = it.groupValues[1] }
        
        // Estrai yield
        val yieldPattern = "(?:yield|resa)[:：]?\\s*([0-9]+[-–]?[0-9]*\\s*(?:g/m²|gr/m²))".toRegex(RegexOption.IGNORE_CASE)
        yieldPattern.find(analysis)?.let { data["yield"] = it.groupValues[1] }
        
        // Estrai terpeni
        val terpenePattern = "(?:terpeni|terpenes?)[:：]?\\s*([^.\\n]+)".toRegex(RegexOption.IGNORE_CASE)
        terpenePattern.find(analysis)?.let { 
            val terpenes = it.groupValues[1]
                .replace("dominanti", "")
                .replace("principali", "")
                .trim()
            data["terpenes"] = terpenes
        }
        
        // Estrai effetti
        val effectsPattern = "(?:effetti|effects?)[:：]?\\s*([^.\\n]+)".toRegex(RegexOption.IGNORE_CASE)
        effectsPattern.find(analysis)?.let { data["effects"] = it.groupValues[1].trim() }
        
        // Estrai malattie/usi medici
        val medicalPattern = "(?:medic[ao]|malattie|condizioni|therapeutic|medical use)[:：]?\\s*([^.\\n]+)".toRegex(RegexOption.IGNORE_CASE)
        medicalPattern.find(analysis)?.let { 
            data["medical"] = it.groupValues[1].trim()
            // Aggiungi ai characteristics se trovato
            data["characteristics"] = "Uso medico: ${it.groupValues[1].trim()}"
        }
        
        // Se non trova characteristics specifici, usa i primi 200 caratteri
        if (!data.containsKey("characteristics")) {
            data["characteristics"] = analysis.take(200) + "..."
        }
        
        return data
    }
    
    private fun parseParentStrains(message: String): Pair<String, String> {
        // Simple parsing for "strain1 x strain2" or "incrocia strain1 con strain2"
        val crossPattern = "(\\w+[\\s\\w]*?)\\s*[xX×]\\s*(\\w+[\\s\\w]*?)".toRegex()
        val incrociaPattern = "incrocia\\s+(\\w+[\\s\\w]*?)\\s+con\\s+(\\w+[\\s\\w]*?)".toRegex()
        
        crossPattern.find(message)?.let {
            return it.groupValues[1].trim() to it.groupValues[2].trim()
        }
        
        incrociaPattern.find(message)?.let {
            return it.groupValues[1].trim() to it.groupValues[2].trim()
        }
        
        return "Unknown" to "Unknown"
    }
    
    private fun checkIfMarconeAdmin(callback: (Boolean) -> Unit) {
        // Check if current session has Marcone admin flag
        val prefs = getSharedPreferences("greed_gross_prefs", MODE_PRIVATE)
        val isMarconeAdmin = prefs.getBoolean("is_marcone_admin", false)
        
        if (isMarconeAdmin) {
            callback(true)
        } else {
            // Check Firebase database for admin status
            val database = FirebaseDatabase.getInstance().reference
            database.child("admins").child("Marcone")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val isAdmin = snapshot.getValue(Boolean::class.java) ?: false
                        if (isAdmin) {
                            prefs.edit().putBoolean("is_marcone_admin", true).apply()
                        }
                        callback(isAdmin)
                    }
                    
                    override fun onCancelled(error: DatabaseError) {
                        callback(false)
                    }
                })
        }
    }
}