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
        // First verify user is authenticated
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            android.util.Log.d("AdminCheck", "User not authenticated")
            callback(false)
            return
        }
        
        android.util.Log.d("AdminCheck", "User authenticated: ${currentUser.uid}")
        
        // Check if current session has Marcone admin flag
        val prefs = getSharedPreferences("greed_gross_prefs", MODE_PRIVATE)
        val isMarconeAdmin = prefs.getBoolean("is_marcone_admin", false)
        
        android.util.Log.d("AdminCheck", "Cached admin status: $isMarconeAdmin")
        
        if (isMarconeAdmin) {
            callback(true)
        } else {
            // Check Firebase database for admin status
            android.util.Log.d("AdminCheck", "Checking Firebase database...")
            val database = com.google.firebase.database.FirebaseDatabase.getInstance().reference
            database.child("admins").child("Marcone")
                .addListenerForSingleValueEvent(object : com.google.firebase.database.ValueEventListener {
                    override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                        android.util.Log.d("AdminCheck", "Firebase snapshot exists: ${snapshot.exists()}")
                        android.util.Log.d("AdminCheck", "Firebase snapshot value: ${snapshot.value}")
                        
                        val isAdmin = snapshot.getValue(Boolean::class.java) ?: false
                        android.util.Log.d("AdminCheck", "Is admin: $isAdmin")
                        
                        if (isAdmin) {
                            prefs.edit().putBoolean("is_marcone_admin", true).apply()
                            android.util.Log.d("AdminCheck", "Admin status cached")
                        }
                        callback(isAdmin)
                    }
                    
                    override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                        android.util.Log.e("AdminCheck", "Database error: ${error.message}")
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