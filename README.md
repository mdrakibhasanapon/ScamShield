# ScamShield - Scam & Phishing Checker

**ScamShield** is an advanced, production-ready Android security application written in pure **Java** designed to help users evaluate suspicious messages, emails, URLs, QR codes, screenshots, network traffic, voice calls, and chat applications for scam, malware, and phishing indicators.

---

## 🌟 Core Highlights

- **Multi-Modal Threat Detection**:
  - **Pasted Text & SMS**: SMS, WhatsApp, Telegram, banking alerts, delivery notices, crypto offers, job offers.
  - **URL & Domain Analysis**: Protocol verification (HTTPS vs HTTP), IP-address URLs, URL shortening expansion, brand typosquatting, high-risk disposable TLDs, deep subdomain stacking, and RFC compliance.
  - **Screenshot OCR Analysis**: Seamless image import with an **editable text review step** ensuring OCR mistakes never lead to misleading security assessments.
  - **QR Code Inspector**: Isolated QR code destination preview with domain breakdown and a strict safety gate: *"Open only after reviewing"*.
  - **Universal Messaging Watchdog**: On-device notification listener scanning incoming messages from WhatsApp, Telegram, Facebook Messenger, Signal, Discord, and Viber.
  - **Network & Data Watchdog**: Real-time monitoring of app bandwidth consumption, detecting suspicious background uploads or data exfiltration.
  - **Microphone & Voice Phishing Shield**: On-device voice transcript analysis against high-pressure vishing patterns, bank impersonation, and fraudulent payment coercion.
  - **Location & Regional Threat Radar**: Real-time detection of unencrypted public Wi-Fi access points and regional scam advisories.
  - **Trusted Contact Sync**: Local phonebook whitelist ensuring communications from saved family and friends are trusted.

- **Calibrated 4-Tier Risk Assessment Engine**:
  - `0 - 20`: **Low Risk** *(No obvious scam indicators detected)*
  - `21 - 50`: **Needs Attention**
  - `51 - 75`: **Suspicious**
  - `76 - 100`: **High Risk / Likely Scam**

- **Explainable Security (No Black-Box Decisions)**:
  - Every flagged assessment provides detailed indicators:
    - **Title** (e.g., *Urgency & Pressure Tactics*, *Bank Fraud Impersonation*)
    - **Severity Badge** (Critical / Warning / Informational)
    - **Plain-Language Explanation** of fraudster tactics
    - **Detected Evidence Snippet** highlighting matched triggers
  - Actionable **Recommended Next Steps** and emergency advice.

- **Tactile Alerting & Notifications**:
  - Haptic feedback and customized vibration patterns (`SO-S` emergency cadence for Critical threats) alerting users even when the phone is on silent.

- **Strict Privacy Engineering & Zero Cloud Exfiltration**:
  - **100% On-Device Processing**: Local heuristic scanners, local OCR, and local voice transcripts. Zero user data is uploaded to remote servers.
  - **Data Sovereignty**: Scan history can be paused, individual scans can be deleted, and a single tap can permanently erase all local databases.
  - **16 KB Page Size Compliant**: Built with modern CameraX and Android 15+ native memory alignment.

---

## 🏗️ Architecture Overview

```text
ScamShield
├── app/
│   ├── src/main/java/com/scamshield/app/
│   │   ├── ads/              # AdMob integration & ad banner controller
│   │   ├── data/
│   │   │   ├── local/        # SQLite database helper & SharedPreferences
│   │   │   ├── model/        # ScamAnalysisResult, RiskLevel, Indicators, Contact, PrivacyProfile
│   │   │   └── repository/   # ScamAnalysisRepository, HistoryRepository, EducationRepository
│   │   ├── domain/
│   │   │   ├── detector/     # TextIndicatorDetector, UrlIndicatorDetector, HomographDetector
│   │   │   ├── engine/       # LocalHeuristicEngine, HybridAnalysisEngine
│   │   │   └── scanner/      # AppPrivacyAuditor, LocationScamRadar, VoiceScamDetector
│   │   ├── receiver/         # Boot & broadcast receivers
│   │   ├── service/          # UniversalMessageListenerService
│   │   ├── ui/
│   │   │   ├── contacts/     # Trusted contacts & safe sender whitelist
│   │   │   ├── education/    # Cyber hygiene guides & red flag checklists
│   │   │   ├── history/      # Scan history & threat timeline
│   │   │   ├── home/         # Dashboard & threat shields hub
│   │   │   ├── message/      # Message analysis
│   │   │   ├── network/      # Data usage & exfiltration monitor
│   │   │   ├── privacy/      # App permission & spyware auditor
│   │   │   ├── qr/           # QR code inspector
│   │   │   ├── result/       # Threat breakdown & recommendations
│   │   │   ├── scan/         # Full device security audit
│   │   │   ├── screenshot/   # OCR screenshot scanner
│   │   │   ├── settings/     # Security toggles & data erasure
│   │   │   └── url/          # URL inspector
│   │   └── util/             # SecuritySanitizer, VibrationHelper, PermissionHelper, ContactSyncHelper
│   ├── src/main/res/         # Material 3 layouts, drawables & strings
│   └── src/test/java/        # JUnit test suites
```

---

## 🚀 Getting Started in Android Studio

1. Clone or download this repository:
   ```bash
   git clone https://github.com/mdrakibhasanapon/ScamShield.git
   ```
2. Launch Android Studio, select **Open**, and select the `ScamShield` folder.
3. Android Studio will automatically recognize the Gradle wrapper and sync the project dependencies.
4. Run the app on an Android device or emulator running **API 26+ (Android 8.0 to Android 15+)**.

---

## 🧪 Running Unit Tests

Run all unit test suites directly from Android Studio or via command line:
```bash
./gradlew testDebugUnitTest
```
Unit test coverage includes:
- Clean message low-risk calibration (0-20 score)
- Critical urgency and bank phishing detection
- Direct IP address and homograph punycode URLs
- URL shorteners and typosquatting prevention
- Voice scam & regional location threat detection
- Universal messaging extractor and permission audit calibration
- Memory DOS protection and null-byte sanitization
