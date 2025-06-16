package com.greedandgross.app

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {
    
    private lateinit var progressBar: ProgressBar
    private lateinit var progressText: TextView
    private lateinit var logo: ImageView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        
        progressBar = findViewById(R.id.progressBar)
        progressText = findViewById(R.id.progressText)
        logo = findViewById(R.id.logo)
        
        startLoading()
    }
    
    private fun startLoading() {
        val progressAnimator = ObjectAnimator.ofInt(progressBar, "progress", 0, 100)
        progressAnimator.duration = 3000
        progressAnimator.interpolator = DecelerateInterpolator()
        
        progressAnimator.addUpdateListener {
            val progress = it.animatedValue as Int
            progressText.text = "$progress%"
            
            when(progress) {
                25 -> progressText.text = "Connessione AI laboratory..."
                50 -> progressText.text = "Inizializzazione genetica..."
                75 -> progressText.text = "Preparazione laboratorio..."
                90 -> progressText.text = "Quasi pronti..."
                100 -> {
                    progressText.text = "Benvenuto nel laboratorio!"
                    Handler(Looper.getMainLooper()).postDelayed({
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }, 500)
                }
            }
        }
        
        // Animazione logo
        logo.animate()
            .scaleX(1.2f)
            .scaleY(1.2f)
            .setDuration(1500)
            .withEndAction {
                logo.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(1500)
                    .start()
            }
            .start()
        
        progressAnimator.start()
    }
}