package com.greedandgross.app

import android.os.Bundle
import android.widget.ImageButton
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity
import com.greedandgross.app.utils.LanguageManager

class SettingsActivity : AppCompatActivity() {
    
    private lateinit var languageGroup: RadioGroup
    private lateinit var radioItalian: RadioButton
    private lateinit var radioSpanish: RadioButton
    private lateinit var radioEnglish: RadioButton
    
    private var secretTapCount = 0
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        
        setupViews()
        loadCurrentLanguage()
    }
    
    private fun setupViews() {
        findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            finish()
        }
        
        // Accesso segreto al pannello admin - 7 tap su "Modalità Proprietario"
        findViewById<TextView>(R.id.ownerModeText).setOnClickListener {
            secretTapCount++
            if (secretTapCount >= 7) {
                // ACCESSO ADMIN SEGRETO!
                startActivity(Intent(this, AdminActivity::class.java))
                secretTapCount = 0
            }
        }
        
        languageGroup = findViewById(R.id.languageGroup)
        radioItalian = findViewById(R.id.radioItalian)
        radioSpanish = findViewById(R.id.radioSpanish)
        radioEnglish = findViewById(R.id.radioEnglish)
        
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
}