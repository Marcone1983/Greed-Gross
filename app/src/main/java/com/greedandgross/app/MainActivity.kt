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
    private val OWNER_UID = "Marcone1983"
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Inizializza Firebase e login automatico
        initializeFirebaseAuth()
        
        // Inizializza billing
        billingManager = BillingManager(this)
        billingManager.startConnection()
        
        setupClickListeners()
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
        val currentUser = FirebaseAuth.getInstance().currentUser
        val isOwner = currentUser?.uid == OWNER_UID
        
        if (BuildConfig.DEBUG) {
            // In debug mode, tutti possono entrare per testare
            startActivity(Intent(this@MainActivity, targetActivity))
            return
        }
        
        lifecycleScope.launch {
            billingManager.isPremium.collect { isPremium ->
                if (isOwner || isPremium) {
                    startActivity(Intent(this@MainActivity, targetActivity))
                } else {
                    startActivity(Intent(this@MainActivity, PaywallActivity::class.java))
                }
            }
        }
    }
    
    private fun initializeFirebaseAuth() {
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        
        if (currentUser == null) {
            if (BuildConfig.DEBUG) {
                // In debug, mostra dialog per scegliere username
                showDebugLoginDialog()
            } else {
                // In produzione, login anonimo automatico
                auth.signInAnonymously()
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            android.util.Log.d("MainActivity", "Production login successful")
                        }
                    }
            }
        }
    }
    
    private fun showDebugLoginDialog() {
        val input = EditText(this)
        input.hint = "Username (es: Mario123, TestUser, Marcone1983)"
        
        AlertDialog.Builder(this)
            .setTitle("🔧 Debug Login")
            .setMessage("Scegli un username per testare l'app:")
            .setView(input)
            .setPositiveButton("Login") { _, _ ->
                val username = input.text.toString().ifEmpty { "TestUser${System.currentTimeMillis() % 1000}" }
                performDebugLogin(username)
            }
            .setNegativeButton("Random") { _, _ ->
                performDebugLogin("User${System.currentTimeMillis() % 1000}")
            }
            .setCancelable(false)
            .show()
    }
    
    private fun performDebugLogin(username: String) {
        val auth = FirebaseAuth.getInstance()
        auth.signInAnonymously()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // In debug, simula l'username scelto nei log
                    android.util.Log.d("MainActivity", "Debug login as: $username (real UID: ${auth.currentUser?.uid})")
                    // Salva username scelto per mostrarlo nell'UI
                    getSharedPreferences("greed_gross_prefs", MODE_PRIVATE)
                        .edit()
                        .putString("debug_username", username)
                        .apply()
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