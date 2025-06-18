const express = require('express');
const cors = require('cors');
const admin = require('firebase-admin');
const { Configuration, OpenAIApi } = require('openai');
require('dotenv').config();

const app = express();
const PORT = process.env.PORT || 3000;

// Middleware
app.use(cors());
app.use(express.json());

// Initialize Firebase Admin
admin.initializeApp({
  credential: admin.credential.cert({
    projectId: process.env.FIREBASE_PROJECT_ID,
    clientEmail: process.env.FIREBASE_CLIENT_EMAIL,
    privateKey: process.env.FIREBASE_PRIVATE_KEY?.replace(/\\n/g, '\n')
  }),
  databaseURL: process.env.FIREBASE_DATABASE_URL
});

const db = admin.database();

// Initialize OpenAI
const configuration = new Configuration({
  apiKey: process.env.OPENAI_API_KEY,
});
const openai = new OpenAIApi(configuration);

// GREED & GROSS System Prompt
const GREED_GROSS_SYSTEM_PROMPT = `Esperto breeder, genetista, farmacista, erborista, agronomo
Sono GREED & GROSS, un esperto genetista della cannabis. La mia specializzazione è nel breeding e backcrossing della cannabis, con una conoscenza approfondita di ogni strain esistente, dei loro alberi genealogici, dei relativi fenotipi, flavonoidi, antocianine, terpeni, e degli effetti corrispondenti. Il mio obiettivo è creare un videogioco che funga da simulatore per lo sviluppo di nuove genetiche di cannabis. Questo gioco consentirà ai breeder di tutto il mondo di simulare la creazione di nuovi strain, esplorando le possibilità genetiche, le resistenze, i tempi di crescita e di fioritura, e l'impatto dei terpeni come pinene e limonene sui sapori. La simulazione predittiva sarà uno strumento preciso e dettagliato che aiuta a prevedere l'outcome di incroci reali, fornendo un ambiente esperto per testare le combinazioni prima di procedere nella realtà. Il mio compito è eseguire ricerche approfondite su tutti gli strain esistenti e diventare un esperto di queste informazioni, integrandole nel ambiente per renderlo un simulatore realistico e accurato della genetica della cannabis.`;

// Helper function to generate cross ID
function generateCrossID(message) {
  return message.toLowerCase()
    .replace(/incrocia/g, '')
    .replace(/con/g, 'x')
    .replace(/cross/g, 'x')
    .trim()
    .replace(/\s+/g, '_');
}

// Middleware to verify Firebase token
async function verifyFirebaseToken(req, res, next) {
  const token = req.headers.authorization?.split('Bearer ')[1];
  
  if (!token) {
    return res.status(401).json({ error: 'No token provided' });
  }
  
  try {
    const decodedToken = await admin.auth().verifyIdToken(token);
    req.user = decodedToken;
    next();
  } catch (error) {
    console.error('Token verification error:', error);
    res.status(401).json({ error: 'Invalid token' });
  }
}

// API Endpoints

// Health check
app.get('/health', (req, res) => {
  res.json({ status: 'OK', service: 'Greed & Gross Middleware API' });
});

// Breeding analysis endpoint
app.post('/api/breeding/analyze', verifyFirebaseToken, async (req, res) => {
  try {
    const { message, language = 'it' } = req.body;
    const userId = req.user.uid;
    
    if (!message) {
      return res.status(400).json({ error: 'Message is required' });
    }
    
    const crossID = generateCrossID(message);
    
    // Check cache first
    const cacheRef = db.ref(`breeding_cache/${userId}/${crossID}`);
    const cachedData = await cacheRef.once('value');
    
    if (cachedData.exists()) {
      console.log(`Cache hit for ${crossID}`);
      return res.json({
        analysis: cachedData.val().response,
        cached: true
      });
    }
    
    // Generate new response
    console.log(`Generating new response for ${crossID}`);
    
    const languagePrompt = language === 'it' ? 'Rispondi in italiano.' :
                          language === 'es' ? 'Responde en español.' :
                          'Reply in English.';
    
    const prompt = `MESSAGGIO UTENTE: "${message}"
    
    ${languagePrompt}`;
    
    const completion = await openai.createChatCompletion({
      model: "gpt-4o-mini",
      messages: [
        { role: "system", content: GREED_GROSS_SYSTEM_PROMPT },
        { role: "user", content: prompt }
      ],
      max_tokens: 2000,
      temperature: 0.7
    });
    
    const response = completion.data.choices[0].message.content;
    
    // Save to cache
    await cacheRef.set({
      response,
      timestamp: Date.now(),
      message
    });
    
    res.json({
      analysis: response,
      cached: false
    });
    
  } catch (error) {
    console.error('Analysis error:', error);
    res.status(500).json({ 
      error: 'Analysis failed', 
      details: error.message 
    });
  }
});

// Cross breeding endpoint with detailed analysis
app.post('/api/breeding/cross', verifyFirebaseToken, async (req, res) => {
  try {
    const { strain1, strain2, language = 'it' } = req.body;
    const userId = req.user.uid;
    
    if (!strain1 || !strain2) {
      return res.status(400).json({ error: 'Both strains are required' });
    }
    
    const crossID = `${strain1.toLowerCase()}_x_${strain2.toLowerCase()}`;
    
    // Check cache
    const cacheRef = db.ref(`cross_cache/${userId}/${crossID}`);
    const cachedData = await cacheRef.once('value');
    
    if (cachedData.exists()) {
      console.log(`Cache hit for cross ${crossID}`);
      return res.json({
        ...cachedData.val(),
        cached: true
      });
    }
    
    // Generate detailed analysis
    const languagePrompt = language === 'it' ? 'Rispondi in italiano.' :
                          language === 'es' ? 'Responde en español.' :
                          'Reply in English.';
    
    const prompt = `Analizza l'incrocio tra ${strain1} e ${strain2}.
    
    Fornisci un'analisi DETTAGLIATA includendo:
    1. Genetica risultante e fenotipo dominante
    2. Percentuali THC/CBD previste con range
    3. Profilo terpenico dominante
    4. Tempo di fioritura e yield previsto
    5. Effetti e caratteristiche del nuovo strain
    
    ${languagePrompt} Formato professionale.`;
    
    const completion = await openai.createChatCompletion({
      model: "gpt-4o-mini",
      messages: [
        { role: "system", content: GREED_GROSS_SYSTEM_PROMPT },
        { role: "user", content: prompt }
      ],
      max_tokens: 2000,
      temperature: 0.7
    });
    
    const analysis = completion.data.choices[0].message.content;
    
    // Generate image prompt
    const imagePrompt = `Professional botanical illustration of cannabis strain: ${strain1} x ${strain2} hybrid plant, detailed trichomes, realistic colors, scientific accuracy`;
    
    // Save to cache
    const responseData = {
      strain1,
      strain2,
      analysis,
      imagePrompt,
      timestamp: Date.now()
    };
    
    await cacheRef.set(responseData);
    
    res.json({
      ...responseData,
      cached: false
    });
    
  } catch (error) {
    console.error('Cross analysis error:', error);
    res.status(500).json({ 
      error: 'Cross analysis failed', 
      details: error.message 
    });
  }
});

// Image generation endpoint
app.post('/api/breeding/generate-image', verifyFirebaseToken, async (req, res) => {
  try {
    const { prompt } = req.body;
    
    if (!prompt) {
      return res.status(400).json({ error: 'Prompt is required' });
    }
    
    const imageResponse = await openai.createImage({
      prompt: prompt,
      n: 1,
      size: "1024x1024",
      quality: "standard"
    });
    
    const imageUrl = imageResponse.data.data[0].url;
    
    res.json({ imageUrl });
    
  } catch (error) {
    console.error('Image generation error:', error);
    res.status(500).json({ 
      error: 'Image generation failed', 
      details: error.message 
    });
  }
});

// Error handling middleware
app.use((err, req, res, next) => {
  console.error(err.stack);
  res.status(500).json({ error: 'Something went wrong!' });
});

// Start server
app.listen(PORT, () => {
  console.log(`🌿 Greed & Gross Middleware API running on port ${PORT}`);
});