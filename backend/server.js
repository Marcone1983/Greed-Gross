const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
const compression = require('compression');
const morgan = require('morgan');
const rateLimit = require('express-rate-limit');
const mongoose = require('mongoose');
const { body, validationResult } = require('express-validator');
const admin = require('firebase-admin');
require('dotenv').config();

const app = express();
const PORT = process.env.PORT || 3000;

// Initialize Firebase Admin
try {
  if (process.env.FIREBASE_SERVICE_ACCOUNT) {
    const serviceAccount = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT);
    admin.initializeApp({
      credential: admin.credential.cert(serviceAccount),
      databaseURL: process.env.FIREBASE_DATABASE_URL || "https://greed-gross-default-rtdb.firebaseio.com"
    });
    console.log('✅ Firebase Admin initialized');
  } else {
    console.log('⚠️ Firebase Admin not initialized - FIREBASE_SERVICE_ACCOUNT not found');
  }
} catch (error) {
  console.error('❌ Firebase Admin initialization error:', error);
}

// Security middleware
app.use(helmet());
app.use(cors({
  origin: process.env.ALLOWED_ORIGINS?.split(',') || ['http://localhost:3000'],
  credentials: true
}));

// Rate limiting
const limiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  max: 100, // limit each IP to 100 requests per windowMs
  message: {
    error: 'Too many requests from this IP, please try again later.'
  }
});
app.use(limiter);

// Body parsing middleware
app.use(compression());
app.use(morgan('combined'));
app.use(express.json({ limit: '10mb' }));
app.use(express.urlencoded({ extended: true }));

// MongoDB connection
if (process.env.MONGODB_URI) {
  mongoose.connect(process.env.MONGODB_URI, {
    useNewUrlParser: true,
    useUnifiedTopology: true,
  }).then(() => {
    console.log('✅ Connected to MongoDB');
  }).catch((err) => {
    console.error('❌ MongoDB connection error:', err);
  });
}

// Strain Schema
const strainSchema = new mongoose.Schema({
  name: { type: String, required: true, unique: true },
  genetics: { type: String, required: true },
  thc: { type: Number, min: 0, max: 35 },
  cbd: { type: Number, min: 0, max: 25 },
  indica: { type: Number, min: 0, max: 100 },
  sativa: { type: Number, min: 0, max: 100 },
  effects: [String],
  flavors: [String],
  difficulty: { type: String, enum: ['Easy', 'Medium', 'Hard'] },
  flowering_time: { type: String },
  yield: { type: String },
  description: String,
  image_url: String,
  breeder: String,
  created_at: { type: Date, default: Date.now },
  updated_at: { type: Date, default: Date.now }
});

const Strain = mongoose.model('Strain', strainSchema);

// Breeding History Schema
const breedingSchema = new mongoose.Schema({
  user_id: { type: String, required: true },
  parent1: { type: String, required: true },
  parent2: { type: String, required: true },
  result_strain: { type: String, required: true },
  ai_response: { type: String, required: true },
  image_url: String,
  created_at: { type: Date, default: Date.now }
});

const BreedingHistory = mongoose.model('BreedingHistory', breedingSchema);

// Routes

// Health check
app.get('/health', (req, res) => {
  res.json({
    status: 'healthy',
    timestamp: new Date().toISOString(),
    uptime: process.uptime(),
    version: '1.0.0'
  });
});

// Get all strains
app.get('/api/strains', async (req, res) => {
  try {
    const { page = 1, limit = 20, search, difficulty, type } = req.query;
    const query = {};
    
    if (search) {
      query.$or = [
        { name: { $regex: search, $options: 'i' } },
        { genetics: { $regex: search, $options: 'i' } },
        { effects: { $in: [new RegExp(search, 'i')] } }
      ];
    }
    
    if (difficulty) {
      query.difficulty = difficulty;
    }
    
    if (type === 'indica') {
      query.indica = { $gt: 60 };
    } else if (type === 'sativa') {
      query.sativa = { $gt: 60 };
    }
    
    const strains = await Strain.find(query)
      .limit(limit * 1)
      .skip((page - 1) * limit)
      .sort({ name: 1 });
    
    const total = await Strain.countDocuments(query);
    
    res.json({
      strains,
      totalPages: Math.ceil(total / limit),
      currentPage: page,
      total
    });
  } catch (error) {
    res.status(500).json({ error: 'Failed to fetch strains' });
  }
});

// Get strain by ID
app.get('/api/strains/:id', async (req, res) => {
  try {
    const strain = await Strain.findById(req.params.id);
    if (!strain) {
      return res.status(404).json({ error: 'Strain not found' });
    }
    res.json(strain);
  } catch (error) {
    res.status(500).json({ error: 'Failed to fetch strain' });
  }
});

// Create new strain (admin only)
app.post('/api/strains', [
  body('name').isLength({ min: 1 }).withMessage('Name is required'),
  body('genetics').isLength({ min: 1 }).withMessage('Genetics is required'),
  body('thc').isFloat({ min: 0, max: 35 }).withMessage('THC must be between 0-35%'),
  body('cbd').isFloat({ min: 0, max: 25 }).withMessage('CBD must be between 0-25%')
], async (req, res) => {
  const errors = validationResult(req);
  if (!errors.isEmpty()) {
    return res.status(400).json({ errors: errors.array() });
  }
  
  try {
    const strain = new Strain(req.body);
    await strain.save();
    res.status(201).json(strain);
  } catch (error) {
    if (error.code === 11000) {
      res.status(400).json({ error: 'Strain name already exists' });
    } else {
      res.status(500).json({ error: 'Failed to create strain' });
    }
  }
});

// Save breeding result
app.post('/api/breeding/save', [
  body('user_id').isLength({ min: 1 }).withMessage('User ID is required'),
  body('parent1').isLength({ min: 1 }).withMessage('Parent 1 is required'),
  body('parent2').isLength({ min: 1 }).withMessage('Parent 2 is required'),
  body('result_strain').isLength({ min: 1 }).withMessage('Result strain is required'),
  body('ai_response').isLength({ min: 1 }).withMessage('AI response is required')
], async (req, res) => {
  const errors = validationResult(req);
  if (!errors.isEmpty()) {
    return res.status(400).json({ errors: errors.array() });
  }
  
  try {
    const breeding = new BreedingHistory(req.body);
    await breeding.save();
    res.status(201).json(breeding);
  } catch (error) {
    res.status(500).json({ error: 'Failed to save breeding result' });
  }
});

// Get user's breeding history
app.get('/api/breeding/history/:userId', async (req, res) => {
  try {
    const { page = 1, limit = 10 } = req.query;
    const history = await BreedingHistory.find({ user_id: req.params.userId })
      .limit(limit * 1)
      .skip((page - 1) * limit)
      .sort({ created_at: -1 });
    
    const total = await BreedingHistory.countDocuments({ user_id: req.params.userId });
    
    res.json({
      history,
      totalPages: Math.ceil(total / limit),
      currentPage: page,
      total
    });
  } catch (error) {
    res.status(500).json({ error: 'Failed to fetch breeding history' });
  }
});

// Analytics endpoint
app.get('/api/analytics/popular-strains', async (req, res) => {
  try {
    const popularStrains = await BreedingHistory.aggregate([
      {
        $group: {
          _id: '$parent1',
          count: { $sum: 1 }
        }
      },
      { $sort: { count: -1 } },
      { $limit: 10 }
    ]);
    
    res.json(popularStrains);
  } catch (error) {
    res.status(500).json({ error: 'Failed to fetch analytics' });
  }
});

// Admin endpoint to set Marcone as admin
app.post('/api/admin/setMarcone', async (req, res) => {
  try {
    const { uid, secret } = req.body;
    
    // Simple security check
    if (secret !== process.env.ADMIN_SECRET) {
      return res.status(403).json({ error: 'Unauthorized' });
    }
    
    if (!uid) {
      return res.status(400).json({ error: 'UID is required' });
    }
    
    // Set custom claims
    await admin.auth().setCustomUserClaims(uid, { 
      admin: true,
      owner: true,
      marcone: true
    });
    
    console.log(`✅ Set admin claims for UID: ${uid}`);
    
    res.json({ 
      success: true, 
      message: 'Marcone is now admin!',
      uid: uid
    });
    
  } catch (error) {
    console.error('❌ Error setting admin claims:', error);
    res.status(500).json({ error: 'Failed to set admin claims' });
  }
});

// Error handling middleware
app.use((err, req, res, next) => {
  console.error(err.stack);
  res.status(500).json({
    error: 'Something went wrong!',
    message: process.env.NODE_ENV === 'development' ? err.message : 'Internal server error'
  });
});

// 404 handler
app.use('*', (req, res) => {
  res.status(404).json({ error: 'API endpoint not found' });
});

app.listen(PORT, () => {
  console.log(`🚀 Greed & Gross Backend server running on port ${PORT}`);
  console.log(`📝 Environment: ${process.env.NODE_ENV || 'development'}`);
});

module.exports = app;