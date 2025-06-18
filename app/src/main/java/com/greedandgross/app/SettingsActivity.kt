package com.greedandgross.app

import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.widget.ImageButton
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.greedandgross.app.utils.LanguageManager

class SettingsActivity : AppCompatActivity() {
    
    private lateinit var languageGroup: RadioGroup
    private lateinit var radioItalian: RadioButton
    private lateinit var radioSpanish: RadioButton
    private lateinit var radioEnglish: RadioButton
    private lateinit var privacyPolicyLink: TextView
    private lateinit var termsOfServiceLink: TextView
    private lateinit var deleteAccountLink: TextView
    
    private var secretTapCount = 0
    
    companion object {
        private const val PRIVACY_POLICY_URL = "https://greed-gross.web.app/privacy_policy_web.html"
        private const val TERMS_OF_SERVICE_URL = "https://greed-gross.web.app/terms_of_service_web.html"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        
        // Check permissions first
        if (!checkUserPermissions()) {
            startActivity(Intent(this, PaywallActivity::class.java))
            finish()
            return
        }
        
        setupViews()
        loadCurrentLanguage()
    }
    
    private fun checkUserPermissions(): Boolean {
        val prefs = getSharedPreferences("greed_gross_prefs", MODE_PRIVATE)
        val username = prefs.getString("persistent_username", null)
        
        // Marcone ha sempre accesso
        if (username == "Marcone") {
            return true
        }
        
        // In debug mode tutti possono accedere
        if (BuildConfig.DEBUG) {
            return true
        }
        
        // Altri utenti devono essere premium
        // TODO: Implementare check premium Firebase/billing
        return false
    }
    
    private fun setupViews() {
        findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            finish()
        }
        
        // Accesso segreto al pannello admin - 7 tap su "Modalità Proprietario" (solo per Marcone)
        findViewById<TextView>(R.id.ownerModeText).setOnClickListener {
            val prefs = getSharedPreferences("greed_gross_prefs", MODE_PRIVATE)
            val username = prefs.getString("persistent_username", null)
            
            android.util.Log.d("SettingsActivity", "Owner mode tap - Username: $username, Tap count: ${secretTapCount + 1}")
            
            if (username == "Marcone") {
                secretTapCount++
                android.util.Log.d("SettingsActivity", "Marcone tap count: $secretTapCount")
                if (secretTapCount >= 7) {
                    // ACCESSO ADMIN SEGRETO!
                    android.util.Log.d("SettingsActivity", "Opening AdminActivity")
                    startActivity(Intent(this, AdminActivity::class.java))
                    secretTapCount = 0
                }
            } else {
                android.util.Log.d("SettingsActivity", "Not Marcone - resetting count")
                secretTapCount = 0
            }
        }
        
        languageGroup = findViewById(R.id.languageGroup)
        radioItalian = findViewById(R.id.radioItalian)
        radioSpanish = findViewById(R.id.radioSpanish)
        radioEnglish = findViewById(R.id.radioEnglish)
        
        // Legal links
        privacyPolicyLink = findViewById(R.id.privacyPolicyLink)
        termsOfServiceLink = findViewById(R.id.termsOfServiceLink)
        deleteAccountLink = findViewById(R.id.deleteAccountLink)
        
        setupLegalLinks()
        
        languageGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.radioItalian -> LanguageManager.setLanguage(LanguageManager.ITALIAN)
                R.id.radioSpanish -> LanguageManager.setLanguage(LanguageManager.SPANISH)
                R.id.radioEnglish -> LanguageManager.setLanguage(LanguageManager.ENGLISH)
            }
        }
    }
    
    private fun loadCurrentLanguage() {
        when (LanguageManager.getCurrentLanguage()) {
            LanguageManager.ITALIAN -> radioItalian.isChecked = true
            LanguageManager.SPANISH -> radioSpanish.isChecked = true
            LanguageManager.ENGLISH -> radioEnglish.isChecked = true
        }
    }
    
    private fun setupLegalLinks() {
        privacyPolicyLink.setOnClickListener {
            android.util.Log.d("SettingsActivity", "Privacy Policy clicked - URL: $PRIVACY_POLICY_URL")
            openWebUrl(PRIVACY_POLICY_URL)
        }
        
        termsOfServiceLink.setOnClickListener {
            android.util.Log.d("SettingsActivity", "Terms of Service clicked - URL: $TERMS_OF_SERVICE_URL")
            openWebUrl(TERMS_OF_SERVICE_URL)
        }
        
        deleteAccountLink.setOnClickListener {
            android.util.Log.d("SettingsActivity", "Delete Account clicked")
            showDeleteAccountDialog()
        }
    }
    
    private fun openWebUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            // Fallback se non c'è browser
            android.widget.Toast.makeText(this, "Impossibile aprire il link", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun showDeleteAccountDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("🗑️ Delete Account & Data")
            .setMessage("This will permanently delete:\n\n• Your account and login data\n• All chat messages\n• Saved strain collections\n• App preferences\n\nThis action cannot be undone.\n\nTo proceed, contact: greedgross@gmail.com")
            .setPositiveButton("Contact Support") { _, _ ->
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:greedgross@gmail.com")
                    putExtra(Intent.EXTRA_SUBJECT, "GDPR Data Deletion Request")
                    putExtra(Intent.EXTRA_TEXT, "I request deletion of all my personal data according to GDPR.\n\nUser ID: ${com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid}\n\nPlease confirm when data has been deleted.")
                }
                try {
                    startActivity(intent)
                } catch (e: Exception) {
                    android.widget.Toast.makeText(this, "No email app found", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}