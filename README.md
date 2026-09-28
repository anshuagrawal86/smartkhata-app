# SmartKhata AI (स्मार्ट खाता) 📱
### Multilingual Voice, Text & Video Diary Ledger Android App

**SmartKhata AI** is a smart, offline-first personal and business ledger (खतौनी / खाता) and diary Android application. It allows you to speak, type, or record short video messages in **English, Hindi, and Hinglish** (e.g., *"Maine Ramesh ko 500 diye kal chai ke"* or *"Anita gave 1200 for groceries"*), automatically extracts transaction details (person, amount, debit/credit, due date), tracks running balances per contact, schedules exact alarm reminders, and provides portable local backup & restore across phones.

---

## 🌟 Key Features

### 1. Multi-Modal Input (Voice, Typing, Video)
- **Voice Entry (बोलकर)**:
  - Powered by Android `SpeechRecognizer` (`hi-IN` & `en-IN`) for **100% free, offline, instant speech-to-text**.
  - Optional: Direct audio file analysis using Google Gemini 2.0 Flash (Free Tier) for mixed dialects and slang.
- **Typing (लिखकर)**:
  - Type naturally in Romanized Hindi (Hinglish), Hindi (Devanagari), or English.
  - Real-time instant preview extracts person name, transaction amount, debit/credit type, and reason as you type.
- **Video Messages (वीडियो संदेश)**:
  - Capture short video notes or camera snapshots of bills, receipts, or agreements.
  - Stored locally in app storage with automatic ledger entry linking.

### 2. Intelligent Auto-Tagging & Search
- Automatically tags every entry with:
  - **Person Name** (e.g., *Ramesh, Pooja, Suresh*)
  - **Amount & Currency** (handles numeric figures and words like *sau, hazaar, dedh sau*)
  - **Transaction Type**:
    - **You Gave (दिया / Debit)**: Green/Red clear visual differentiation
    - **You Got (लिया / Credit)**
  - **Dates & Due Dates**: Detects *"kal"*, *"aaj"*, *"agle hafte"*
  - **Category / Reason Notes**
- **Instant Search**: Search across thousands of entries by contact name, amount range, or keyword notes.

### 3. Real-Time Balance Calculations
- **Overall Financial Dashboard**:
  - **Net Balance** (Total to collect minus total to pay)
  - **You'll Get (दिया)**: Total money you have lent / need to collect
  - **You'll Give (लिया)**: Total money you have received / owe to others
- **Individual Contact Ledgers**:
  - Open any contact to see their complete chronological statement.
  - Real-time balance: *"Ramesh owes you ₹500"* or *"You owe Ramesh ₹200"*.
  - One-tap button to share statement summary via WhatsApp or SMS.

### 4. Person-Linked Reminders & Alarms
- Set exact alarms on specific dates and times directly tied to ledger entries and contacts.
- Utilizes Android `AlarmManager` with `setExactAndAllowWhileIdle()`, firing even when the app is closed or after device reboot.
- Direct quick-action button to send a polite WhatsApp payment reminder to the contact.

### 5. 100% Local Storage, Backup & Restore
- **Zero Required Cloud Fees**: Works fully offline on-device with SQLite / Room database.
- **Generate Backup**:
  - Bundles all contacts, entries, reminders, and audio/video files into a portable `.skbackup` compressed archive.
  - Export to Google Drive, WhatsApp, Email, or File Manager with one tap.
- **Restore Backup**:
  - Pick a `.skbackup` file on any new Android phone to immediately restore all your accounts, entries, media files, and reminders.

---

## 🛠️ Technology Stack & Architecture

- **Language**: Kotlin 2.0+
- **UI Toolkit**: Jetpack Compose + Material Design 3
- **Local Database**: AndroidX Room (SQLite) with Reactive Kotlin `Flow`
- **Speech Recognition**: Android Native `SpeechRecognizer` (`hi-IN`, `en-IN`)
- **Multimodal AI (Free Tier)**: Gemini 2.0 Flash REST API (optional free key from Google AI Studio)
- **Local Parsing**: `LocalHinglishParser` (offline regex & linguistic token pattern matching)
- **Alarms & Reminders**: Android `AlarmManager` + `BroadcastReceiver` + Notification Channels
- **Media**: Android `MediaRecorder` & FileProvider

---

## 📂 Project Structure

```
andriod app/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   ├── java/com/smartkhata/app/
│   │   │   ├── SmartKhataApp.kt                 # Application class & notification setup
│   │   │   ├── MainActivity.kt                  # Main Compose navigation hub
│   │   │   ├── data/
│   │   │   │   ├── model/Models.kt              # Domain data models & types
│   │   │   │   ├── local/
│   │   │   │   │   ├── AppDatabase.kt           # Room Database definition
│   │   │   │   │   ├── entity/Entities.kt       # Contact, Entry, Reminder entities
│   │   │   │   │   ├── dao/Daos.kt              # Room DAOs with reactive Flows
│   │   │   │   │   └── converters/Converters.kt # Type converters
│   │   │   │   ├── parser/
│   │   │   │   │   ├── LocalHinglishParser.kt   # Offline Hinglish & Hindi NLP extractor
│   │   │   │   │   └── GeminiHinglishParser.kt  # Free Gemini 2.0 Flash Multimodal engine
│   │   │   │   └── repository/
│   │   │   │       ├── LedgerRepository.kt      # Balance math & contact ledger logic
│   │   │   │       └── BackupRepository.kt      # ZIP backup export and restore engine
│   │   │   ├── reminders/
│   │   │   │   ├── ReminderReceiver.kt          # Alarm notification broadcast receiver
│   │   │   │   └── ReminderScheduler.kt         # Exact alarm scheduler
│   │   │   ├── util/
│   │   │   │   ├── AudioRecorderUtil.kt         # Voice note audio recorder
│   │   │   │   ├── SpeechRecognizerHelper.kt    # Native Android speech-to-text helper
│   │   │   │   └── Formatters.kt                # Currency & date formatters
│   │   │   └── ui/
│   │   │       ├── theme/                       # Material 3 colors, typography, theme
│   │   │       ├── navigation/Screen.kt         # Navigation destinations
│   │   │       ├── components/                  # Reusable UI cards & components
│   │   │       └── screens/
│   │   │           ├── dashboard/               # Summary, search, and entries
│   │   │           ├── newentry/                # Voice, typing, video capture modal
│   │   │           ├── contactledger/           # Person ledger & statement sharing
│   │   │           ├── reminders/               # Reminder list, scheduler & WhatsApp share
│   │   │           ├── contacts/                # All contacts directory
│   │   │           └── backup/                  # Backup generation, restore, API key settings
│   │   └── res/                                 # Strings, adaptive icons, themes, file paths
│   └── build.gradle.kts
├── gradle/
│   ├── libs.versions.toml                       # Dependency version catalog
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── build.gradle.kts                             # Root build script
├── settings.gradle.kts                          # Project settings
└── gradlew.bat                                  # Windows Gradle wrapper
```

---

## 🚀 How to Run & Build

1. **Open in Android Studio**:
   - Launch **Android Studio**.
   - Select **Open** and choose this folder: `c:\Users\Anshu\source\andriod app`.
   - Android Studio will automatically sync the Gradle files and download required SDK components.
2. **Build Debug APK via Command Line**:
   ```bash
   ./gradlew assembleDebug
   ```
   The APK will be generated at:
   `app/build/outputs/apk/debug/app-debug.apk`
3. **Run on Phone or Emulator**:
   - Enable USB Debugging on your Android phone and connect via USB, or start an Android Emulator.
   - Click the green **Run** button (▶) in Android Studio, or run:
     ```bash
     ./gradlew installDebug
     ```

---

## 🔒 Free Tier & Privacy Guarantee

- **No Server Subscriptions**: You do not need any paid backend, Firebase Blaze plan, or AWS server.
- **Your Data Stays on Your Device**: All transactions, audio voice notes, and videos are saved directly inside your phone's private storage.
- **Free AI Key (Optional)**: If you want Gemini 2.0 Flash for video/deep voice parsing, get a 100% free API key from [Google AI Studio](https://aistudio.google.com/) and paste it in **Backup & Settings -> Gemini API Key**.
