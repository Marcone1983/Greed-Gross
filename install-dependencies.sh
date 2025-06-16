#!/bin/bash

echo "🌿 Installazione Greed & Gross App..."
echo "Questo potrebbe richiedere alcuni minuti..."

# Cambia il registry npm a uno più veloce
npm config set registry https://registry.npmmirror.com

# Installa le dipendenze principali una alla volta
echo "📦 Installando React e React Native..."
npm install react@18.2.0 --save --legacy-peer-deps
npm install react-native@0.72.6 --save --legacy-peer-deps

echo "📦 Installando navigazione..."
npm install @react-navigation/native@^6.1.7 --save --legacy-peer-deps
npm install @react-navigation/stack@^6.3.17 --save --legacy-peer-deps

echo "📦 Installando componenti UI..."
npm install react-native-linear-gradient@^2.8.3 --save --legacy-peer-deps
npm install react-native-vector-icons@^10.0.0 --save --legacy-peer-deps
npm install react-native-safe-area-context@^4.6.3 --save --legacy-peer-deps
npm install react-native-screens@^3.22.0 --save --legacy-peer-deps

echo "📦 Installando dipendenze per chat..."
npm install axios@^1.4.0 --save --legacy-peer-deps
npm install socket.io-client@^4.7.2 --save --legacy-peer-deps

echo "✅ Installazione completata!"
echo "Per avviare l'app su Android: npx react-native run-android"
echo "Per avviare l'app su iOS: npx react-native run-ios"