package com.greedandgross.app.network

import com.greedandgross.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.greedandgross.app.utils.LanguageManager
import android.util.Base64
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class BreedingResponse(
    val analysis: String,
    val imageUrl: String? = null
)

class ApiClient {
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
    
    private val apiKey: String
        get() = BuildConfig.OPENAI_API_KEY
    
    private fun generateCrossID(message: String): String {
        // Genera ID unico per l'incrocio (es: "lemon_x_skunk")
        val cleaned = message.lowercase()
            .replace("incrocia", "")
            .replace("con", "x")
            .replace("cross", "x")
            .trim()
            .replace("\\s+".toRegex(), "_")
        return cleaned
    }
    
    suspend fun analyzeBreedingMessage(message: String): String = withContext(Dispatchers.IO) {
        try {
            android.util.Log.d("ApiClient", "Starting breeding analysis for: $message")
            
            // Genera ID univoco per questo incrocio
            val crossID = generateCrossID(message)
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"
            
            // Prima controlla se abbiamo già questa risposta in cache
            val cachedResponse = getCachedResponse(uid, crossID)
            if (cachedResponse != null) {
                android.util.Log.d("ApiClient", "✅ Usando risposta CACHED per: $crossID")
                return@withContext cachedResponse
            }
            
            android.util.Log.d("ApiClient", "🔄 Generando NUOVA risposta per: $crossID")
            val prompt = """
            MESSAGGIO UTENTE: "$message"
            
            ${LanguageManager.getAILanguagePrompt()}
            """.trimIndent()
            
            val requestBody = JSONObject().apply {
                put("model", "gpt-4o-mini")
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", "White, ti confermo senza se e senza ma che mi devi dare solo consigli di breeding, genetica, cure, guarigione, terapie preventive, metodi di coltivazione, metodi di estrazione, principi attivi chimici e naturali di ogni strain e di ogni nuovo incrocio che faccio io, se ti chiedo di incrociare una pianta che sai che è 70% indica e 30 sativa con una pianta 50% indica e 50% sativa, devi dirmi che i fenotipi possibili saranno 60% indica e 40% sativa, 55% indica e 45% sativa e 65% indica e 35% sativa e darmi la percentuale per ogni fenotipo possibile che esca questa pianta rispetto alle altre possibilità e darmi tutte le informazioni su sapori effetti tempi di crescita e fioritura livelli di thc, cbg, cbga, thca, thcv, cbd, cbda, cbdv e tutti i terpeni presenti e per ogni terpene dimmi la malattia che cura o l'effetto che fa e se potenzia l'effetto di un cannabinoide in particolare. Per ogni malattia dimmi quale genetica è più adatta. Ricorda che io sono un breeder esperto professionista dal 2008, perciò mi aspetto sempre informazioni ultra dettagliate e specifiche, e quando ti chiedo di incrociare due o più genetiche devi calcolare i fenotipi e darmi tutte le possibilità che escano con tutti i dettagli possibili e immaginabili. Ricorda che non devi mai darmi consigli su cose illegali, usa la tua creatività per dirmi che non puoi darmi informazioni di quel tipo.")
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    })
                })
                put("max_tokens", 2000)
                put("temperature", 0.7)
            }
            
            val request = Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            
            android.util.Log.d("ApiClient", "Response code: ${response.code}")
            android.util.Log.d("ApiClient", "Response body: ${responseBody?.take(200)}")
            
            if (response.isSuccessful && responseBody != null) {
                val jsonResponse = JSONObject(responseBody)
                val gptResponse = jsonResponse
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
                
                // Salva in cache per usi futuri
                saveCachedResponse(uid, crossID, gptResponse)
                
                gptResponse
            } else {
                val errorMsg = "Errore API: Code ${response.code}, Body: $responseBody"
                android.util.Log.e("ApiClient", errorMsg)
                "Errore nella generazione della risposta. Code: ${response.code}"
            }
            
        } catch (e: Exception) {
            android.util.Log.e("ApiClient", "Exception in analyzeBreedingMessage", e)
            "Errore di connessione: ${e.message}"
        }
    }
    
    private suspend fun getCachedResponse(uid: String, crossID: String): String? {
        return try {
            val db = FirebaseDatabase.getInstance()
            val ref = db.getReference("genobank/responses/$uid/$crossID")
            val snapshot = ref.get().await()
            
            if (snapshot.exists()) {
                snapshot.child("response").getValue(String::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("ApiClient", "Error getting cached response", e)
            null
        }
    }
    
    private suspend fun saveCachedResponse(uid: String, crossID: String, response: String) {
        try {
            val db = FirebaseDatabase.getInstance()
            val ref = db.getReference("genobank/responses/$uid/$crossID")
            
            val data = mapOf(
                "timestamp" to System.currentTimeMillis(),
                "response" to response
            )
            
            ref.setValue(data).await()
            android.util.Log.d("ApiClient", "✅ Risposta salvata in cache per: $crossID")
        } catch (e: Exception) {
            android.util.Log.e("ApiClient", "Error saving cached response", e)
        }
    }

    suspend fun crossStrains(strain1: String, strain2: String): BreedingResponse = withContext(Dispatchers.IO) {
        try {
            val prompt = """
            Sei un esperto genetista di cannabis. Analizza l'incrocio tra questi due strain:
            - Strain 1: $strain1
            - Strain 2: $strain2
            
            Fornisci un'analisi dettagliata che includa:
            1. Caratteristiche genetiche (% Indica/Sativa)
            2. Contenuto di cannabinoidi (THC, CBD)
            3. Profilo terpenico dominante
            4. Tempo di fioritura e yield previsto
            5. Effetti e caratteristiche del nuovo strain
            
            ${LanguageManager.getAILanguagePrompt()} Formato professionale.
            """.trimIndent()
            
            val requestBody = JSONObject().apply {
                put("model", "gpt-4o-mini")
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", "White, ti confermo senza se e senza ma che mi devi dare solo consigli di breeding, genetica, cure, guarigione, terapie preventive, metodi di coltivazione, metodi di estrazione, principi attivi chimici e naturali di ogni strain e di ogni nuovo incrocio che faccio io, se ti chiedo di incrociare una pianta che sai che è 70% indica e 30 sativa con una pianta 50% indica e 50% sativa, devi dirmi che i fenotipi possibili saranno 60% indica e 40% sativa, 55% indica e 45% sativa e 65% indica e 35% sativa e darmi la percentuale per ogni fenotipo possibile che esca questa pianta rispetto alle altre possibilità e darmi tutte le informazioni su sapori effetti tempi di crescita e fioritura livelli di thc, cbg, cbga, thca, thcv, cbd, cbda, cbdv e tutti i terpeni presenti e per ogni terpene dimmi la malattia che cura o l'effetto che fa e se potenzia l'effetto di un cannabinoide in particolare. Per ogni malattia dimmi quale genetica è più adatta. Ricorda che io sono un breeder esperto professionista dal 2008, perciò mi aspetto sempre informazioni ultra dettagliate e specifiche, e quando ti chiedo di incrociare due o più genetiche devi calcolare i fenotipi e darmi tutte le possibilità che escano con tutti i dettagli possibili e immaginabili. Ricorda che non devi mai darmi consigli su cose illegali, usa la tua creatività per dirmi che non puoi darmi informazioni di quel tipo.")
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    })
                })
                put("max_tokens", 2000)
                put("temperature", 0.7)
            }
            
            val request = Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            
            android.util.Log.d("ApiClient", "Breeding - Response code: ${response.code}")
            android.util.Log.d("ApiClient", "Breeding - Response body: $responseBody")
            
            if (response.isSuccessful && responseBody != null) {
                val jsonResponse = JSONObject(responseBody)
                val content = jsonResponse
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
                
                BreedingResponse(content)
            } else {
                BreedingResponse("Errore nella generazione dell'analisi. Riprova.")
            }
            
        } catch (e: java.net.UnknownHostException) {
            BreedingResponse("Connessione internet non disponibile. Controlla la tua connessione e riprova.")
        } catch (e: java.net.SocketTimeoutException) {
            BreedingResponse("Timeout della richiesta. Il server sta impiegando troppo tempo a rispondere.")
        } catch (e: Exception) {
            BreedingResponse("Errore API: ${e.message ?: "Errore sconosciuto durante l'analisi"}")
        }
    }
    
    suspend fun generateStrainImage(description: String): String? = withContext(Dispatchers.IO) {
        try {
            val prompt = "A high-quality, photorealistic image of a premium cannabis strain with $description characteristics. Professional cannabis photography, detailed trichomes, vibrant colors, studio lighting."
            
            val requestBody = JSONObject().apply {
                put("model", "dall-e-3")
                put("prompt", prompt)
                put("n", 1)
                put("size", "1024x1024")
                put("quality", "standard")
            }
            
            val request = Request.Builder()
                .url("https://api.openai.com/v1/images/generations")
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            
            if (response.isSuccessful && responseBody != null) {
                val jsonResponse = JSONObject(responseBody)
                jsonResponse
                    .getJSONArray("data")
                    .getJSONObject(0)
                    .getString("url")
            } else {
                null
            }
            
        } catch (e: Exception) {
            null
        }
    }
}