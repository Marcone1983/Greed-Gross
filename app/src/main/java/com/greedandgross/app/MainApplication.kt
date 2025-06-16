package com.greedandgross.app

import android.app.Application
import com.greedandgross.app.utils.LanguageManager

class MainApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Language Manager
        LanguageManager.init(this)
    }
}