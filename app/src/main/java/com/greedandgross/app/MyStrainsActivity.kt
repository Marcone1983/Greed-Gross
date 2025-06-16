package com.greedandgross.app

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.greedandgross.app.adapters.MyStrainsAdapter
import com.greedandgross.app.models.SavedStrain

class MyStrainsActivity : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var loadingIndicator: ProgressBar
    private lateinit var emptyText: TextView
    private lateinit var strainsAdapter: MyStrainsAdapter
    
    private val strains = mutableListOf<SavedStrain>()
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private var strainsListener: ValueEventListener? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_strains)
        
        setupFirebase()
        setupViews()
        setupRecyclerView()
        loadMyStrains()
    }
    
    private fun setupFirebase() {
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference
    }
    
    private fun setupViews() {
        recyclerView = findViewById(R.id.recyclerView)
        loadingIndicator = findViewById(R.id.loadingIndicator)
        emptyText = findViewById(R.id.emptyText)
        
        findViewById<ImageButton>(R.id.backButton)?.setOnClickListener {
            finish()
        }
    }
    
    private fun setupRecyclerView() {
        strainsAdapter = MyStrainsAdapter(strains) { strain ->
            // Click su strain per vedere dettagli
            showStrainDetails(strain)
        }
        
        recyclerView.apply {
            layoutManager = GridLayoutManager(this@MyStrainsActivity, 2)
            adapter = strainsAdapter
        }
    }
    
    private fun loadMyStrains() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            // Auto-login anonimo
            loadingIndicator.visibility = View.VISIBLE
            emptyText.text = "Connessione in corso..."
            emptyText.visibility = View.VISIBLE
            
            auth.signInAnonymously()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        loadMyStrains() // Riprova dopo login
                    } else {
                        emptyText.text = "Errore connessione: ${task.exception?.message}"
                        loadingIndicator.visibility = View.GONE
                    }
                }
            return
        }
        
        loadingIndicator.visibility = View.VISIBLE
        
        strainsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                strains.clear()
                for (strainSnapshot in snapshot.children) {
                    val strain = strainSnapshot.getValue(SavedStrain::class.java)
                    strain?.let { strains.add(it) }
                }
                
                strainsAdapter.notifyDataSetChanged()
                
                if (strains.isEmpty()) {
                    emptyText.text = "Nessun strain salvato.\nCrea il tuo primo incrocio nel Breeding Laboratory!"
                    emptyText.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                } else {
                    emptyText.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                }
                
                loadingIndicator.visibility = View.GONE
            }
            
            override fun onCancelled(error: DatabaseError) {
                android.util.Log.e("MyStrains", "Failed to load strains", error.toException())
                emptyText.text = "Errore nel caricamento: ${error.message}"
                emptyText.visibility = View.VISIBLE
                loadingIndicator.visibility = View.GONE
            }
        }
        
        database.child("user_strains").child(userId)
            .orderByChild("createdAt")
            .addValueEventListener(strainsListener!!)
    }
    
    private fun showStrainDetails(strain: SavedStrain) {
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("🌿 " + strain.name)
            .setMessage("""
                ${strain.description}
                
                📊 Dettagli:
                • Genitori: ${strain.parentStrain1} × ${strain.parentStrain2}
                • Tipo: ${strain.phenotype}
                • Creato: ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.ITALIAN).format(strain.createdAt)}
            """.trimIndent())
            .setPositiveButton("Chiudi") { dialog, _ -> dialog.dismiss() }
            .create()
        
        dialog.show()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        strainsListener?.let {
            val userId = auth.currentUser?.uid
            if (userId != null) {
                database.child("user_strains").child(userId).removeEventListener(it)
            }
        }
    }
}