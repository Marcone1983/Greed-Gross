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
        
        // Check username-based permissions
        checkUserPermissions { hasAccess ->
            if (hasAccess) {
                startActivity(Intent(this@MainActivity, targetActivity))
            } else {
                startActivity(Intent(this@MainActivity, PaywallActivity::class.java))
            }
        }
    }
    
    private fun checkUserPermissions(callback: (Boolean) -> Unit) {
        val prefs = getSharedPreferences("greed_gross_prefs", MODE_PRIVATE)
        val username = prefs.getString("persistent_username", null)
        
        if (username == null) {
            // First time - get username from user
            getUsernameFromPrefsOrDialog { finalUsername ->
                checkPermissionsForUsername(finalUsername, callback)
            }
        } else {
            checkPermissionsForUsername(username, callback)
        }
    }
    
    private fun checkPermissionsForUsername(username: String, callback: (Boolean) -> Unit) {
        android.util.Log.d("MainActivity", "Checking permissions for username: $username")
        
        // Check if Marcone admin
        if (username == "Marcone") {
            android.util.Log.d("MainActivity", "Marcone admin confirmed - full access")
            callback(true)
            return
        }
        
        // Check if premium user based on username
        checkIfPremiumUser(username) { isPremium ->
            android.util.Log.d("MainActivity", "User $username premium status: $isPremium")
            callback(isPremium)
        }
    }
    
    private fun checkIfPremiumUser(username: String, callback: (Boolean) -> Unit) {
        // Check local premium status first
        lifecycleScope.launch {
            billingManager.isPremium.collect { isPremium ->
                if (isPremium) {
                    // User has active premium - save to Firebase for persistence
                    savePremiumStatusToFirebase(username, true)
                    callback(true)
                } else {
                    // Check Firebase for premium status
                    checkPremiumStatusFromFirebase(username, callback)
                }
            }
        }
    }
    
    private fun savePremiumStatusToFirebase(username: String, isPremium: Boolean) {
        val database = com.google.firebase.database.FirebaseDatabase.getInstance()
        val premiumRef = database.getReference("premium_users").child(username)
        
        val premiumData = mapOf(
            "isPremium" to isPremium,
            "lastUpdated" to System.currentTimeMillis(),
            "platform" to "android"
        )
        
        premiumRef.setValue(premiumData)
            .addOnSuccessListener {
                android.util.Log.d("MainActivity", "Premium status saved for $username")
            }
            .addOnFailureListener { error ->
                android.util.Log.e("MainActivity", "Failed to save premium status: ${error.message}")
            }
    }
    
    private fun checkPremiumStatusFromFirebase(username: String, callback: (Boolean) -> Unit) {
        val database = com.google.firebase.database.FirebaseDatabase.getInstance()
        val premiumRef = database.getReference("premium_users").child(username)
        
        premiumRef.get()
            .addOnSuccessListener { snapshot ->
                val isPremium = snapshot.child("isPremium").getValue(Boolean::class.java) ?: false
                android.util.Log.d("MainActivity", "Firebase premium status for $username: $isPremium")
                callback(isPremium)
            }
            .addOnFailureListener { error ->
                android.util.Log.e("MainActivity", "Failed to check premium status: ${error.message}")
                callback(false)
            }
    }
    
    private fun getUsernameFromPrefsOrDialog(callback: (String) -> Unit) {
        val prefs = getSharedPreferences("greed_gross_prefs", MODE_PRIVATE)
        val username = prefs.getString("persistent_username", null)
        
        if (username != null) {
            callback(username)
        } else {
            showUsernameDialog { chosenUsername ->
                prefs.edit().putString("persistent_username", chosenUsername).apply()
                android.util.Log.d("MainActivity", "User chose username: $chosenUsername")
                callback(chosenUsername)
            }
        }
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
        // Long press su logo per attivare Marcone mode
        // TODO: Implementare se necessario
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