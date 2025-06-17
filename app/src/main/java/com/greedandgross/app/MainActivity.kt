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
        
        // HARDCODED: Marcone sempre accesso, altri pagano
        val isMarcone = true // TODO: Implementare check reale per Marcone
        
        if (isMarcone) {
            startActivity(Intent(this@MainActivity, targetActivity))
        } else {
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
        // Check if current session has Marcone admin flag
        val prefs = getSharedPreferences("greed_gross_prefs", MODE_PRIVATE)
        val isMarconeAdmin = prefs.getBoolean("is_marcone_admin", false)
        
        if (isMarconeAdmin) {
            callback(true)
        } else {
            // Check Firebase database for admin status
            val database = com.google.firebase.database.FirebaseDatabase.getInstance().reference
            database.child("admins").child("marcone")
                .addListenerForSingleValueEvent(object : com.google.firebase.database.ValueEventListener {
                    override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                        val isAdmin = snapshot.getValue(Boolean::class.java) ?: false
                        if (isAdmin) {
                            prefs.edit().putBoolean("is_marcone_admin", true).apply()
                        }
                        callback(isAdmin)
                    }
                    
                    override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                        callback(false)
                    }
                })
        }
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