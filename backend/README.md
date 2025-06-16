# Greed & Gross Backend Server

Backend API server for the Greed & Gross cannabis breeding simulation app.

## Features

- RESTful API for strain management
- User breeding history tracking
- MongoDB integration
- Rate limiting and security
- OpenAI API proxy (optional)
- Firebase authentication support
- Analytics endpoints

## Setup

1. Install dependencies:
```bash
npm install
```

2. Copy environment variables:
```bash
cp .env.example .env
```

3. Configure your environment variables in `.env`

4. Start MongoDB (if running locally):
```bash
mongod
```

5. Run the server:
```bash
# Development
npm run dev

# Production
npm start
```

## API Endpoints

### Health
- `GET /health` - Health check

### Strains
- `GET /api/strains` - Get all strains (with pagination and filters)
- `GET /api/strains/:id` - Get strain by ID
- `POST /api/strains` - Create new strain (admin only)

### Breeding
- `POST /api/breeding/save` - Save breeding result
- `GET /api/breeding/history/:userId` - Get user's breeding history

### Analytics
- `GET /api/analytics/popular-strains` - Get most popular strains

## Database Schema

### Strain
```javascript
{
  name: String,
  genetics: String,
  thc: Number (0-35),
  cbd: Number (0-25),
  indica: Number (0-100),
  sativa: Number (0-100),
  effects: [String],
  flavors: [String],
  difficulty: String,
  flowering_time: String,
  yield: String,
  description: String,
  image_url: String,
  breeder: String
}
```

### Breeding History
```javascript
{
  user_id: String,
  parent1: String,
  parent2: String,
  result_strain: String,
  ai_response: String,
  image_url: String,
  created_at: Date
}
```

## Security Features

- Helmet.js for security headers
- CORS configuration
- Rate limiting (100 requests per 15 minutes)
- Input validation with express-validator
- Compression middleware
- Request logging with Morgan

## Environment Variables

See `.env.example` for all required environment variables.

## Docker Support

```dockerfile
FROM node:18-alpine
WORKDIR /app
COPY package*.json ./
RUN npm ci --only=production
COPY . .
EXPOSE 3000
CMD ["npm", "start"]
```

## Testing

```bash
npm test
```

## License

ISC - Copyright (c) 2025 Marcone1983