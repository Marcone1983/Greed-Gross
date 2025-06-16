package com.greedandgross.app.utils

import android.content.Context
import android.content.SharedPreferences

object LanguageManager {
    private const val PREF_KEY = "selected_language"
    const val ITALIAN = "it"
    const val SPANISH = "es"
    const val ENGLISH = "en"
    
    private lateinit var prefs: SharedPreferences
    
    fun init(context: Context) {
        prefs = context.getSharedPreferences("greed_gross_prefs", Context.MODE_PRIVATE)
    }
    
    fun getCurrentLanguage(): String = prefs.getString(PREF_KEY, ITALIAN) ?: ITALIAN
    
    fun setLanguage(language: String) {
        prefs.edit().putString(PREF_KEY, language).apply()
    }
    
    // Traduzioni per l'app
    fun getString(key: String): String {
        val lang = getCurrentLanguage()
        return when (key) {
            "welcome_breeding" -> when (lang) {
                ENGLISH -> "Welcome to the breeding laboratory! 🧬\n\n🎁 FREE TRIAL: You can make 1 free cross to test the AI.\n\nEnter two strains you want to cross!"
                SPANISH -> "¡Bienvenido al laboratorio de breeding! 🧬\n\n🎁 PRUEBA GRATIS: Puedes hacer 1 cruce gratis para probar la IA.\n\n¡Ingresa dos cepas que quieras cruzar!"
                else -> "Benvenuto nel laboratorio di breeding! 🧬\n\n🎁 PROVA GRATUITA: Puoi fare 1 incrocio gratis per testare l'AI.\n\nInserisci due strain che vuoi incrociare!"
            }
            "analyzing" -> when (lang) {
                ENGLISH -> "🧬 Analyzing..."
                SPANISH -> "🧬 Analizando..."
                else -> "🧬 Analizzando..."
            }
            "generating_image" -> when (lang) {
                ENGLISH -> "🎨 Generating image..."
                SPANISH -> "🎨 Generando imagen..."
                else -> "🎨 Generando immagine..."
            }
            "save_strain_prompt" -> when (lang) {
                ENGLISH -> "💾 Want to save this strain to your collection?\nWrite 'save [name]' to save it!"
                SPANISH -> "💾 ¿Quieres guardar esta cepa en tu colección?\n¡Escribe 'guardar [nombre]' para guardarla!"
                else -> "💾 Vuoi salvare questo strain nella tua collezione?\nScrivi 'salva [nome]' per salvarlo!"
            }
            "chat_welcome" -> when (lang) {
                ENGLISH -> "Welcome to the global breeders chat! 🌿\nShare tips and experiences with other growers."
                SPANISH -> "¡Bienvenido al chat global de cultivadores! 🌿\nComparte consejos y experiencias con otros cultivadores."
                else -> "Benvenuto nella chat globale dei breeders! 🌿\nCondividi consigli e esperienze con altri coltivatori."
            }
            "my_strains" -> when (lang) {
                ENGLISH -> "My Strains"
                SPANISH -> "Mis Cepas"
                else -> "I Miei Strain"
            }
            "settings" -> when (lang) {
                ENGLISH -> "Settings"
                SPANISH -> "Configuración"
                else -> "Impostazioni"
            }
            "owner_mode" -> when (lang) {
                ENGLISH -> "👑 Owner Mode Active"
                SPANISH -> "👑 Modo Propietario Activo"
                else -> "👑 Modalità Proprietario Attiva"
            }
            else -> key
        }
    }
    
    // Per dire all'AI in che lingua rispondere
    fun getAILanguagePrompt(): String = when (getCurrentLanguage()) {
        ENGLISH -> "Respond in English."
        SPANISH -> "Responde en español."
        else -> "Rispondi in italiano."
    }
}