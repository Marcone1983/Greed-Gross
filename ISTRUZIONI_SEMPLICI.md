# 🌿 ISTRUZIONI SEMPLICI PER GREED & GROSS

## Cosa fare ORA:

### 1. Installa le dipendenze (NUOVO METODO)
```bash
cd greed-gross-app
./install-dependencies.sh
```

Questo installerà tutto piano piano, evitando i timeout.

### 2. Se l'installazione funziona, avvia l'app:
```bash
npx react-native run-android
```

### 3. Se hai ancora problemi con npm:
Prova questo comando alternativo:
```bash
npm config set registry https://registry.npmmirror.com
npm install
```

### 4. Alternative se Termux non funziona:

#### OPZIONE A: Usa un servizio online
1. Vai su https://expo.dev
2. Crea un account gratuito
3. Carica i file dell'app
4. Loro gestiranno tutto online

#### OPZIONE B: Usa un PC Windows/Mac
1. Installa Node.js dal sito ufficiale
2. Copia la cartella greed-gross-app sul PC
3. Apri il terminale nella cartella
4. Esegui: npm install
5. Poi: npx react-native run-android

## 🆘 Se sei bloccato:

1. **Errore di timeout**: Prova con una connessione WiFi diversa
2. **Errore di permessi**: Esegui `termux-setup-storage` e riprova
3. **Errore di spazio**: Libera spazio su Android

## 📱 Risultato finale:
- Un'app Android con:
  - Logo verde/nero all'avvio
  - Chat per simulare incroci di cannabis
  - Chat globale tra breeder
  - Tutto funzionante con la tua API key

## ⚠️ IMPORTANTE:
La tua API key è già configurata nel file. Non condividere mai il file `config/api.js` con nessuno!