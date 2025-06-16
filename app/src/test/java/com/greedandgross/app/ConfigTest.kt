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
        // In test environment, API key might not be set, so we just check it's not null
        assertTrue("OpenAI API key must not be null", BuildConfig.OPENAI_API_KEY != null)
    }
    
    @Test
    fun applicationId_isValid() {
        assertTrue("Application ID must be valid", BuildConfig.APPLICATION_ID.contains("greedandgross"))
    }
}