package com.greedandgross.app

import android.app.Application
import com.greedandgross.app.BuildConfig
import com.greedandgross.app.utils.LanguageManager
import timber.log.Timber

class MainApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(ReleaseTree())
        }
        
        Timber.d("Application started")
        
        // Initialize Language Manager
        LanguageManager.init(this)
    }
}

/** No-op logging in release builds */
class ReleaseTree : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) { 
        // No-op in release builds for performance and security
    }
}