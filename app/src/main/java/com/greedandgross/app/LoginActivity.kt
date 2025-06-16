package com.greedandgross.app

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    
    private lateinit var loginTab: Button
    private lateinit var registerTab: Button
    private lateinit var usernameField: TextInputEditText
    private lateinit var passwordField: TextInputEditText
    private lateinit var confirmPasswordLayout: TextInputLayout
    private lateinit var confirmPasswordField: TextInputEditText
    private lateinit var submitButton: Button
    private lateinit var quickMarcone: Button
    private lateinit var quickGuest: Button
    
    private var isLoginMode = true
    private lateinit var auth: FirebaseAuth
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        
        auth = FirebaseAuth.getInstance()
        
        setupViews()
        setupClickListeners()
        animateEntrance()
    }
    
    private fun setupViews() {
        loginTab = findViewById(R.id.loginTab)
        registerTab = findViewById(R.id.registerTab)
        usernameField = findViewById(R.id.usernameField)
        passwordField = findViewById(R.id.passwordField)
        confirmPasswordLayout = findViewById(R.id.confirmPasswordLayout)
        confirmPasswordField = findViewById(R.id.confirmPasswordField)
        submitButton = findViewById(R.id.submitButton)
        quickMarcone = findViewById(R.id.quickMarcone)
        quickGuest = findViewById(R.id.quickGuest)
        
        updateFormMode()
    }
    
    private fun setupClickListeners() {
        loginTab.setOnClickListener {
            if (!isLoginMode) {
                isLoginMode = true
                updateFormMode()
                animateTabSwitch()
            }
        }
        
        registerTab.setOnClickListener {
            if (isLoginMode) {
                isLoginMode = false
                updateFormMode()
                animateTabSwitch()
            }
        }
        
        submitButton.setOnClickListener {
            performAuthentication()
        }
        
        quickMarcone.setOnClickListener {
            quickLogin("Marcone", "👑 Boss Access")
        }
        
        quickGuest.setOnClickListener {
            quickLogin("Guest${System.currentTimeMillis() % 1000}", "👤 Guest Mode")
        }
    }
    
    private fun updateFormMode() {
        if (isLoginMode) {
            // Login Mode
            loginTab.setBackgroundResource(R.drawable.tab_selected)
            loginTab.setTextColor(getColor(R.color.black))
            registerTab.setBackgroundResource(R.drawable.tab_unselected)
            registerTab.setTextColor(getColor(R.color.green_secondary))
            
            confirmPasswordLayout.visibility = View.GONE
            submitButton.text = "🚀 LOGIN"
            
            usernameField.hint = "🧬 Username or Email"
        } else {
            // Register Mode
            registerTab.setBackgroundResource(R.drawable.tab_selected)
            registerTab.setTextColor(getColor(R.color.black))
            loginTab.setBackgroundResource(R.drawable.tab_unselected)
            loginTab.setTextColor(getColor(R.color.green_secondary))
            
            confirmPasswordLayout.visibility = View.VISIBLE
            submitButton.text = "🌱 CREATE ACCOUNT"
            
            usernameField.hint = "🧬 Username"
        }
    }
    
    private fun animateTabSwitch() {
        submitButton.animate()
            .scaleX(0.9f)
            .scaleY(0.9f)
            .setDuration(100)
            .withEndAction {
                submitButton.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .start()
            }
            .start()
    }
    
    private fun animateEntrance() {
        val logo = findViewById<View>(R.id.logo)
        val title = findViewById<View>(R.id.appTitle)
        val subtitle = findViewById<View>(R.id.subtitle)
        
        // Fade in animation
        listOf(logo, title, subtitle).forEachIndexed { index, view ->
            view.alpha = 0f
            view.animate()
                .alpha(1f)
                .setDuration(800)
                .setStartDelay(index * 200L)
                .start()
        }
        
        // Scale animation for logo
        logo.scaleX = 0.5f
        logo.scaleY = 0.5f
        logo.animate()
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(1000)
            .start()
    }
    
    private fun performAuthentication() {
        val username = usernameField.text.toString().trim()
        val password = passwordField.text.toString()
        
        android.util.Log.d("LoginActivity", "Attempting login with username: $username")
        
        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "⚠️ Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (!isLoginMode) {
            val confirmPassword = confirmPasswordField.text.toString()
            
            if (confirmPassword.isEmpty()) {
                Toast.makeText(this, "⚠️ Please fill all fields", Toast.LENGTH_SHORT).show()
                return
            }
            
            if (password != confirmPassword) {
                Toast.makeText(this, "❌ Passwords don't match", Toast.LENGTH_SHORT).show()
                return
            }
        }
        
        // Fake authentication with loading
        animateLogin(username)
    }
    
    private fun quickLogin(username: String, message: String) {
        android.util.Log.d("LoginActivity", "Quick login: $username")
        
        usernameField.setText(username)
        passwordField.setText("cannabis123")
        
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        animateLogin(username)
    }
    
    private fun animateLogin(username: String) {
        submitButton.isEnabled = false
        submitButton.text = "🔄 Connecting..."
        
        // Fake loading animation
        val progressAnimator = ObjectAnimator.ofFloat(submitButton, "alpha", 1f, 0.5f, 1f)
        progressAnimator.duration = 1500
        progressAnimator.repeatCount = 2
        
        // Save username immediately
        getSharedPreferences("greed_gross_prefs", MODE_PRIVATE)
            .edit()
            .putString("username", username)
            .putString("login_type", if (isLoginMode) "login" else "register")
            .apply()
        
        // Start animation
        progressAnimator.start()
        
        // Firebase anonymous auth
        auth.signInAnonymously()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    lifecycleScope.launch {
                        delay(1500) // Wait for animation
                        
                        Toast.makeText(this@LoginActivity, "✅ Welcome $username!", Toast.LENGTH_SHORT).show()
                        
                        // Navigate to main app
                        val intent = Intent(this@LoginActivity, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    }
                } else {
                    submitButton.isEnabled = true
                    submitButton.text = if (isLoginMode) "🚀 LOGIN" else "🌱 CREATE ACCOUNT"
                    val error = task.exception?.message ?: "Unknown error"
                    Toast.makeText(this@LoginActivity, "❌ Connection failed: $error", Toast.LENGTH_LONG).show()
                    android.util.Log.e("LoginActivity", "Auth failed", task.exception)
                }
            }
    }
}