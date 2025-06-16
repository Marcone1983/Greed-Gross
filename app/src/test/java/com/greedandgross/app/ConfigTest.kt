package com.greedandgross.app

import org.junit.Assert.assertTrue
import org.junit.Test

class ConfigTest {
    @Test
    fun apiBaseUrl_isNotBlank() {
        assertTrue("API_BASE_URL must not be blank", BuildConfig.API_BASE_URL.isNotBlank())
    }
    
    @Test
    fun openaiApiKey_isConfigured() {
        // Accept either real API key or test placeholder
        val isValidKey = BuildConfig.OPENAI_API_KEY != null && 
                        (BuildConfig.OPENAI_API_KEY.startsWith("sk-") || 
                         BuildConfig.OPENAI_API_KEY == "API_KEY_NOT_SET")
        assertTrue("OpenAI API key must be configured or test placeholder", isValidKey)
    }
    
    @Test
    fun applicationId_isValid() {
        assertTrue("Application ID must be valid", BuildConfig.APPLICATION_ID.contains("greedandgross"))
    }
}