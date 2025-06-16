package com.greedandgross.app

import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.greedandgross.app.billing.BillingManager
import kotlinx.coroutines.launch

class PaywallActivity : AppCompatActivity() {
    
    private lateinit var billingManager: BillingManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_paywall)
        
        billingManager = BillingManager(this)
        
        setupViews()
        setupBilling()
    }
    
    private fun setupViews() {
        findViewById<ImageButton>(R.id.closeButton).setOnClickListener {
            finish()
        }
        
        findViewById<Button>(R.id.subscribeButton).setOnClickListener {
            billingManager.purchasePremium(this)
        }
        
        findViewById<Button>(R.id.restoreButton).setOnClickListener {
            billingManager.restorePurchases()
        }
    }
    
    private fun setupBilling() {
        billingManager.startConnection()
        
        lifecycleScope.launch {
            billingManager.isPremium.collect { isPremium ->
                if (isPremium) {
                    setResult(RESULT_OK)
                    finish()
                }
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        billingManager.endConnection()
    }
}