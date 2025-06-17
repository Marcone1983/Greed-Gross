package com.greedandgross.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

class AdminActivity : AppCompatActivity() {
    
    private lateinit var totalCrossesText: TextView
    private lateinit var todayCountText: TextView
    private lateinit var topStrainsRecycler: RecyclerView
    private lateinit var exportButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var database: DatabaseReference
    
    private val crossesList = mutableListOf<AdminCrossData>()
    private val topStrains = mutableMapOf<String, Int>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)
        
        // Solo Marcone può accedere!
        if (!isOwner()) {
            finish()
            return
        }
        
        setupViews()
        setupFirebase()
        loadDashboard()
    }
    
    private fun isOwner(): Boolean {
        return if (BuildConfig.DEBUG) {
            // In debug mode, sempre owner per Marcone
            true
        } else {
            // In produzione, check UID reale
            val currentUser = FirebaseAuth.getInstance().currentUser
            val OWNER_UID = "eqDvGiUzc6SZQS4FUuvQi0jTMhy1"
            currentUser?.uid == OWNER_UID
        }
    }
    
    private fun setupViews() {
        findViewById<ImageButton>(R.id.backButton).setOnClickListener { finish() }
        
        totalCrossesText = findViewById(R.id.totalCrossesText)
        todayCountText = findViewById(R.id.todayCountText)
        topStrainsRecycler = findViewById(R.id.topStrainsRecycler)
        exportButton = findViewById(R.id.exportButton)
        progressBar = findViewById(R.id.progressBar)
        
        exportButton.setOnClickListener { exportDatabase() }
        
        setupRecyclerView()
    }
    
    private fun setupFirebase() {
        database = FirebaseDatabase.getInstance().reference
    }
    
    private fun setupRecyclerView() {
        topStrainsRecycler.layoutManager = LinearLayoutManager(this)
        // Adapter verrà impostato dopo il caricamento dati
    }
    
    private fun loadDashboard() {
        progressBar.visibility = View.VISIBLE
        
        database.child("genobank").child("responses")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    crossesList.clear()
                    topStrains.clear()
                    
                    var totalCrosses = 0
                    var todayCrosses = 0
                    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    
                    for (userSnapshot in snapshot.children) {
                        val userId = userSnapshot.key ?: continue
                        
                        for (crossSnapshot in userSnapshot.children) {
                            val crossId = crossSnapshot.key ?: continue
                            val timestamp = crossSnapshot.child("timestamp").getValue(Long::class.java) ?: 0
                            val response = crossSnapshot.child("response").getValue(String::class.java) ?: ""
                            
                            totalCrosses++
                            
                            // Conta incroci di oggi
                            val crossDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timestamp))
                            if (crossDate == today) {
                                todayCrosses++
                            }
                            
                            // Conta popolarità strain
                            topStrains[crossId] = topStrains.getOrDefault(crossId, 0) + 1
                            
                            // Aggiungi ai dati admin
                            crossesList.add(AdminCrossData(
                                crossId = crossId,
                                userId = userId,
                                timestamp = timestamp,
                                responseLength = response.length
                            ))
                        }
                    }
                    
                    updateUI(totalCrosses, todayCrosses)
                    progressBar.visibility = View.GONE
                }
                
                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@AdminActivity, "Errore caricamento: ${error.message}", Toast.LENGTH_SHORT).show()
                    progressBar.visibility = View.GONE
                }
            })
    }
    
    private fun updateUI(total: Int, today: Int) {
        totalCrossesText.text = "💎 Incroci Totali: $total"
        todayCountText.text = "🔥 Oggi: $today"
        
        // Top 10 strain più popolari
        val topList = topStrains.toList().sortedByDescending { it.second }.take(10)
        val adapter = TopStrainsAdapter(topList)
        topStrainsRecycler.adapter = adapter
    }
    
    private fun exportDatabase() {
        progressBar.visibility = View.VISIBLE
        exportButton.isEnabled = false
        
        database.child("genobank")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        val jsonString = snapshot.value?.let { JSONObject(it as Map<*, *>).toString(2) } ?: "{}"
                        
                        // Salva file
                        val fileName = "genobank_export_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.json"
                        val file = File(getExternalFilesDir(null), fileName)
                        
                        FileWriter(file).use { writer ->
                            writer.write(jsonString)
                        }
                        
                        // Condividi file
                        val uri = androidx.core.content.FileProvider.getUriForFile(
                            this@AdminActivity,
                            "${packageName}.fileprovider",
                            file
                        )
                        
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/json"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            putExtra(Intent.EXTRA_SUBJECT, "GenoBank Export - ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())}")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        
                        startActivity(Intent.createChooser(shareIntent, "Esporta GenoBank"))
                        
                        Toast.makeText(this@AdminActivity, "✅ Database esportato: $fileName", Toast.LENGTH_LONG).show()
                        
                    } catch (e: Exception) {
                        Toast.makeText(this@AdminActivity, "❌ Errore export: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                    
                    progressBar.visibility = View.GONE
                    exportButton.isEnabled = true
                }
                
                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@AdminActivity, "❌ Errore export: ${error.message}", Toast.LENGTH_SHORT).show()
                    progressBar.visibility = View.GONE
                    exportButton.isEnabled = true
                }
            })
    }
}

data class AdminCrossData(
    val crossId: String,
    val userId: String,
    val timestamp: Long,
    val responseLength: Int
)

class TopStrainsAdapter(private val strains: List<Pair<String, Int>>) : RecyclerView.Adapter<TopStrainsAdapter.ViewHolder>() {
    
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val strainName: TextView = view.findViewById(R.id.strainName)
        val strainCount: TextView = view.findViewById(R.id.strainCount)
        val rankNumber: TextView = view.findViewById(R.id.rankNumber)
    }
    
    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_top_strain, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (strain, count) = strains[position]
        holder.rankNumber.text = "#${position + 1}"
        holder.strainName.text = strain.replace("_", " × ").uppercase()
        holder.strainCount.text = "$count volte"
    }
    
    override fun getItemCount() = strains.size
}