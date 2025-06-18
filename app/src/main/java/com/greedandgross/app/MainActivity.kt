package com.greedandgross.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.greedandgross.app.billing.BillingManager
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    
    private lateinit var billingManager: BillingManager
    private val OWNER_UID = "eqDvGiUzc6SZQS4FUuvQi0jTMhy1"
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Inizializza Firebase e login automatico
        initializeFirebaseAuth()
        
        // Inizializza billing
        billingManager = BillingManager(this)
        billingManager.startConnection()
        
        setupClickListeners()
        setupSecretMarconeActivation()
    }
    
    private fun setupClickListeners() {
        findViewById<CardView>(R.id.cardBreeding).setOnClickListener {
            animateCardClick(it) {
                startActivity(Intent(this, BreedingChatActivity::class.java))
            }
        }
        
        findViewById<CardView>(R.id.cardGlobalChat).setOnClickListener {
            animateCardClick(it) {
                checkPremiumAndNavigate(GlobalChatActivity::class.java)
            }
        }
        
        findViewById<CardView>(R.id.cardMyStrains).setOnClickListener {
            animateCardClick(it) {
                startActivity(Intent(this, MyStrainsActivity::class.java))
            }
        }
        
        findViewById<CardView>(R.id.cardSettings).setOnClickListener {
            animateCardClick(it) {
                checkPremiumAndNavigate(SettingsActivity::class.java)
            }
        }
    }
    
    private fun checkPremiumAndNavigate(targetActivity: Class<*>) {
        if (BuildConfig.DEBUG) {
            // In debug mode, tutti possono entrare per testare
            startActivity(Intent(this@MainActivity, targetActivity))
            return
        }
        
        // Ensure user is authenticated before checking admin status
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            android.util.Log.d("MainActivity", "User not authenticated, signing in...")
            // Wait for authentication
            FirebaseAuth.getInstance().signInAnonymously()
                .addOnSuccessListener {
                    android.util.Log.d("MainActivity", "Auth successful, checking admin...")
                    checkIfMarconeAdmin { isMarcone ->
                        navigateBasedOnStatus(isMarcone, targetActivity)
                    }
                }
                .addOnFailureListener { error ->
                    android.util.Log.e("MainActivity", "Auth failed: ${error.message}")
                    // Auth failed, go to paywall
                    startActivity(Intent(this@MainActivity, PaywallActivity::class.java))
                }
        } else {
            android.util.Log.d("MainActivity", "User already authenticated, checking admin...")
            checkIfMarconeAdmin { isMarcone ->
                navigateBasedOnStatus(isMarcone, targetActivity)
            }
        }
    }
    
    private fun navigateBasedOnStatus(isMarcone: Boolean, targetActivity: Class<*>) {
        if (isMarcone) {
            android.util.Log.d("MainActivity", "Marcone admin confirmed - full access")
            startActivity(Intent(this@MainActivity, targetActivity))
        } else {
            android.util.Log.d("MainActivity", "Not Marcone admin - checking billing")
            // Altri utenti - check billing
            lifecycleScope.launch {
                billingManager.isPremium.collect { isPremium ->
                    if (isPremium) {
                        startActivity(Intent(this@MainActivity, targetActivity))
                    } else {
                        startActivity(Intent(this@MainActivity, PaywallActivity::class.java))
                    }
                }
            }
        }
    }
    
    private fun checkIfMarconeAdmin(callback: (Boolean) -> Unit) {
        val prefs = getSharedPreferences("greed_gross_prefs", MODE_PRIVATE)
        
        // Get or create persistent username
        val persistentUsername = prefs.getString("persistent_username", null)
        
        if (persistentUsername == null) {
            // First time - ask user to choose username
            showUsernameDialog { chosenUsername ->
                prefs.edit().putString("persistent_username", chosenUsername).apply()
                android.util.Log.d("AdminCheck", "User chose username: $chosenUsername")
                
                // Check if it's Marcone (case insensitive + variations)
                val isMarcone = isOwnerUsername(chosenUsername)
                if (isMarcone) {
                    prefs.edit().putBoolean("is_marcone_admin", true).apply()
                    android.util.Log.d("AdminCheck", "Owner admin activated for: $chosenUsername")
                }
                callback(isMarcone)
            }
        } else {
            android.util.Log.d("AdminCheck", "Existing persistent username: $persistentUsername")
            
            // Check if this username is Marcone
            val isMarcone = isOwnerUsername(persistentUsername)
            if (isMarcone) {
                val isMarconeAdmin = prefs.getBoolean("is_marcone_admin", false)
                if (!isMarconeAdmin) {
                    prefs.edit().putBoolean("is_marcone_admin", true).apply()
                    android.util.Log.d("AdminCheck", "Admin status restored for owner")
                }
            }
            callback(isMarcone)
        }
    }
    
    private fun isOwnerUsername(username: String?): Boolean {
        if (username == null) return false
        val cleanUsername = username.lowercase().trim()
        return cleanUsername in listOf("marcone", "marcone1983", "owner", "admin", "greedgross", "greed&gross")
    }
    
    private fun generatePersistentUsername(): String {
        // Simple check - if this is the first install and they want to be Marcone
        // For now return random, but could be modified for special cases
        val adjectives = listOf("Expert", "Master", "Pro", "Elite", "Legendary")
        val nouns = listOf("Breeder", "Grower", "Cultivator", "Geneticist", "Farmer")
        val number = kotlin.random.Random.nextInt(1000, 9999)
        
        // SPECIAL: If device has specific marker, return "Marcone"
        val prefs = getSharedPreferences("greed_gross_prefs", MODE_PRIVATE)
        val isMarconeDevice = prefs.getBoolean("is_marcone_device", false)
        
        return if (isMarconeDevice) {
            "Marcone"
        } else {
            "${adjectives.random()}${nouns.random()}_$number"
        }
    }
    
    private fun showUsernameDialog(callback: (String) -> Unit) {
        val input = EditText(this)
        input.hint = "Inserisci il tuo username"
        
        AlertDialog.Builder(this)
            .setTitle("🌿 Benvenuto in Greed & Gross!")
            .setMessage("Scegli il tuo username per la community:")
            .setView(input)
            .setPositiveButton("Conferma") { _, _ ->
                val username = input.text.toString().trim()
                if (username.isNotEmpty()) {
                    callback(username)
                } else {
                    // Se vuoto, genera random
                    val randomUsername = generateRandomUsername()
                    callback(randomUsername)
                }
            }
            .setNegativeButton("Random") { _, _ ->
                val randomUsername = generateRandomUsername()
                callback(randomUsername)
            }
            .setCancelable(false)
            .show()
    }
    
    private fun generateRandomUsername(): String {
        val adjectives = listOf("Expert", "Master", "Pro", "Elite", "Legendary")
        val nouns = listOf("Breeder", "Grower", "Cultivator", "Geneticist", "Farmer")
        val number = kotlin.random.Random.nextInt(1000, 9999)
        return "${adjectives.random()}${nouns.random()}_$number"
    }
    
    private fun setupSecretMarconeActivation() {
        // Long press su logo per attivare Marcone mode direttamente
        findViewById<View>(R.id.appLogo)?.setOnLongClickListener {
            forceMarconeActivation()
            true
        }
        
        // Triple tap su titolo app per backup activation
        var tapCount = 0
        findViewById<View>(R.id.appTitle)?.setOnClickListener {
            tapCount++
            if (tapCount >= 3) {
                forceMarconeActivation()
                tapCount = 0
            }
        }
    }
    
    private fun forceMarconeActivation() {
        val prefs = getSharedPreferences("greed_gross_prefs", MODE_PRIVATE)
        prefs.edit().apply {
            putString("persistent_username", "Marcone")
            putBoolean("is_marcone_admin", true)
            putBoolean("is_marcone_device", true)
            apply()
        }
        
        AlertDialog.Builder(this)
            .setTitle("👑 OWNER MODE ACTIVATED")
            .setMessage("Benvenuto Marcone!\n\n✅ Admin privileges attivati\n✅ GenoBank access abilitato\n✅ Unlimited breeding abilitato")
            .setPositiveButton("OK") { _, _ ->
                // Restart activity per applicare cambiamenti
                recreate()
            }
            .show()
            
        android.util.Log.d("MainActivity", "🚀 FORCE MARCONE ACTIVATION - All admin privileges granted")
    }
    
    private fun initializeFirebaseAuth() {
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        
        if (currentUser == null) {
            // Auto login anonimo se non già autenticato
            auth.signInAnonymously()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        android.util.Log.d("MainActivity", "Auto login successful")
                    }
                }
        }
    }
    
    private fun animateCardClick(view: View, action: () -> Unit) {
        view.animate()
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(100)
            .withEndAction {
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .withEndAction {
                        action()
                    }
                    .start()
            }
            .start()
    }
}