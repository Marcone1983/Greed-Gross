const mongoose = require('mongoose');

const strainSchema = new mongoose.Schema({
  name: {
    type: String,
    required: [true, 'Strain name is required'],
    unique: true,
    trim: true,
    maxlength: [100, 'Strain name cannot exceed 100 characters']
  },
  genetics: {
    type: String,
    required: [true, 'Genetics information is required'],
    trim: true,
    maxlength: [200, 'Genetics cannot exceed 200 characters']
  },
  thc: {
    type: Number,
    min: [0, 'THC cannot be negative'],
    max: [35, 'THC cannot exceed 35%'],
    default: 0
  },
  cbd: {
    type: Number,
    min: [0, 'CBD cannot be negative'],
    max: [25, 'CBD cannot exceed 25%'],
    default: 0
  },
  indica: {
    type: Number,
    min: [0, 'Indica percentage cannot be negative'],
    max: [100, 'Indica percentage cannot exceed 100%'],
    default: 50
  },
  sativa: {
    type: Number,
    min: [0, 'Sativa percentage cannot be negative'],
    max: [100, 'Sativa percentage cannot exceed 100%'],
    default: 50
  },
  effects: [{
    type: String,
    trim: true,
    enum: [
      'Relaxed', 'Happy', 'Euphoric', 'Uplifted', 'Creative',
      'Focused', 'Energetic', 'Sleepy', 'Hungry', 'Giggly',
      'Talkative', 'Aroused', 'Tingly', 'Pain Relief', 'Anti-anxiety'
    ]
  }],
  flavors: [{
    type: String,
    trim: true,
    enum: [
      'Earthy', 'Sweet', 'Citrus', 'Pine', 'Woody', 'Diesel',
      'Skunk', 'Spicy', 'Herbal', 'Flowery', 'Berry', 'Fruity',
      'Mint', 'Tropical', 'Vanilla', 'Coffee', 'Chocolate', 'Cheese'
    ]
  }],
  difficulty: {
    type: String,
    enum: {
      values: ['Easy', 'Medium', 'Hard'],
      message: 'Difficulty must be Easy, Medium, or Hard'
    },
    default: 'Medium'
  },
  flowering_time: {
    type: String,
    trim: true,
    validate: {
      validator: function(v) {
        return /^\d{1,2}-\d{1,2} weeks?$/.test(v) || /^\d{1,2} weeks?$/.test(v);
      },
      message: 'Flowering time should be in format "8-10 weeks" or "9 weeks"'
    }
  },
  yield: {
    type: String,
    trim: true,
    enum: ['Low', 'Medium', 'High', 'Very High'],
    default: 'Medium'
  },
  description: {
    type: String,
    trim: true,
    maxlength: [1000, 'Description cannot exceed 1000 characters']
  },
  image_url: {
    type: String,
    trim: true,
    validate: {
      validator: function(v) {
        return !v || /^https?:\/\/.+\.(jpg|jpeg|png|gif|webp)$/i.test(v);
      },
      message: 'Image URL must be a valid image URL'
    }
  },
  breeder: {
    type: String,
    trim: true,
    maxlength: [100, 'Breeder name cannot exceed 100 characters']
  },
  lineage: {
    parent1: {
      type: String,
      trim: true
    },
    parent2: {
      type: String,
      trim: true
    }
  },
  medical_uses: [{
    type: String,
    enum: [
      'Pain', 'Anxiety', 'Depression', 'Insomnia', 'Appetite Loss',
      'Nausea', 'Inflammation', 'Seizures', 'PTSD', 'ADHD',
      'Migraines', 'Arthritis', 'Glaucoma', 'Fibromyalgia'
    ]
  }],
  growing_info: {
    indoor_yield: String,
    outdoor_yield: String,
    height: String,
    climate: {
      type: String,
      enum: ['Indoor', 'Outdoor', 'Both']
    }
  },
  popularity_score: {
    type: Number,
    default: 0,
    min: 0
  },
  verified: {
    type: Boolean,
    default: false
  },
  created_at: {
    type: Date,
    default: Date.now
  },
  updated_at: {
    type: Date,
    default: Date.now
  }
});

// Validate that indica + sativa equals 100
strainSchema.pre('save', function(next) {
  if (this.indica + this.sativa !== 100) {
    this.sativa = 100 - this.indica;
  }
  this.updated_at = new Date();
  next();
});

// Index for search performance
strainSchema.index({ name: 'text', genetics: 'text', description: 'text' });
strainSchema.index({ difficulty: 1 });
strainSchema.index({ indica: 1, sativa: 1 });
strainSchema.index({ thc: -1 });
strainSchema.index({ popularity_score: -1 });

// Virtual for strain type
strainSchema.virtual('type').get(function() {
  if (this.indica > 70) return 'Indica';
  if (this.sativa > 70) return 'Sativa';
  return 'Hybrid';
});

// Transform output
strainSchema.set('toJSON', {
  virtuals: true,
  transform: function(doc, ret) {
    delete ret._id;
    delete ret.__v;
    return ret;
  }
});

module.exports = mongoose.model('Strain', strainSchema);