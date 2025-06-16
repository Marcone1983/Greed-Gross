package com.greedandgross.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class MainActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
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
                // BYPASS PROPRIETARIO: Tu puoi sempre accedere!
                val isOwner = true // Marcone1983 sempre premium
                
                if (isOwner) {
                    startActivity(Intent(this, GlobalChatActivity::class.java))
                } else {
                    startActivity(Intent(this, PaywallActivity::class.java))
                }
            }
        }
        
        findViewById<CardView>(R.id.cardMyStrains).setOnClickListener {
            animateCardClick(it) {
                startActivity(Intent(this, MyStrainsActivity::class.java))
            }
        }
        
        findViewById<CardView>(R.id.cardSettings).setOnClickListener {
            animateCardClick(it) {
                // BYPASS PROPRIETARIO: Tu puoi sempre accedere alle impostazioni!
                val isOwner = true // Marcone1983 sempre premium
                
                if (isOwner) {
                    startActivity(Intent(this, SettingsActivity::class.java))
                } else {
                    startActivity(Intent(this, PaywallActivity::class.java))
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