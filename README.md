# ScamShield — Scam & Phishing Checker

**ScamShield** is a production-ready Android security application written in pure **Java** designed to help users evaluate suspicious messages, emails, URLs, QR codes, and screenshots for potential scam and phishing indicators before they respond or click.

---

## 🛡️ Core Highlights

- **Multi-Modal Input Protection**:
  - **Pasted Text**: SMS, WhatsApp, Telegram, banking alerts, delivery notices, crypto offers, job offers.
  - **URL / Domain Analysis**: Protocol verification (HTTPS vs HTTP), IP-address URLs, URL shortening expansion, brand typosquatting, high-risk disposable TLDs, deep subdomain stacking, and RFC compliance.
  - **Screenshot OCR Analysis**: Seamless image import with an **editable text review step** ensuring OCR mistakes never lead to misleading security assessments.
  - **QR Code Inspector**: Isolated QR code destination preview with domain breakdown and a strict safety gate: *"Open only after reviewing"*.
  - **System Share Sheet Integration**: Direct share receiver (`ACTION_SEND`) for incoming text, URLs, and screenshots from any Android app.

- **Calibrated 4-Tier Risk Assessment Engine**:
  - `0 - 20`: **No obvious scam indicators detected** *(Always accompanied by clear reminders that 100% certainty is impossible)*
  - `21 - 50`: **Needs attention**
  - `51 - 75`: **Suspicious**
  - `76 - 100`: **Likely scam**

- **Explainable Security (No Black-Box "AI Says So")**:
  - Every flagged assessment provides detailed indicators containing:
    - **Title** (e.g., *Urgency & Pressure Tactics*, *Gift Card Payment Demand*)
    - **Severity Badge** (Critical / Warning / Informational)
    - **Plain-English Explanation** of why fraudsters employ this tactic
    - **Detected Evidence Snippet** showing the exact matching pattern
  - Contextual **Recommended Next Actions** (e.g., *"Do not send money"*, *"Verify sender via independent channel"*)
  - **Human Verification Reminders** highlighting what automated checks cannot verify (e.g., spoofed Caller ID or email display names)

- **Strict Privacy Engineering & Data Minimization**:
  - **Zero Invasive Permissions**: Does **NOT** require `READ_SMS`, `READ_CALL_LOG`, `READ_CONTACTS`, or `AccessibilityService`.
  - **User-Initiated Submission**: Only content intentionally provided by the user is analyzed.
  - **Offline-First by Design**: Complete Layer 1 & 2 heuristic engines run 100% on-device with zero network latency and zero external data transmission.
  - **Data Sovereignty**: Scan history can be toggled off, individual scans can be deleted, and a single tap can permanently erase all local data.
  - **Cloud Backup Protection**: Excluded from Android Auto-Backup rules (`data_extraction_rules.xml`).

- **Interactive User Feedback & False-Positive Handling**:
  - Users can rate each assessment (*Helpful*, *Incorrect*, *Not sure*) to refine threat detection without collecting private message contents.

- **Scam Prevention Education Hub**:
  - 9 comprehensive cybersecurity awareness modules covering:
    1. Phishing & Spear-Phishing
    2. Fake Delivery & Parcel Surcharges
    3. Work-From-Home & Job Scams
    4. Crypto & High-Yield Investment Fraud
    5. Romance & Catfishing Scams
    6. Bank & Government Impersonation
    7. Tech Support & Browser Pop-up Scams
    8. Quishing (Malicious QR Codes)
    9. Account Takeover & 2FA Code Theft

---

## 🏗️ Architecture Overview

```
ScamShield
├── app/
│   ├── src/main/java/com/scamshield/app/
│   │   ├── data/
│   │   │   ├── local/          # SQLite database helper & SharedPreferences
│   │   │   ├── model/          # ScamAnalysisResult, RiskLevel, Indicators, Feedback, Education
│   │   │   └── repository/     # ScamAnalysisRepository, HistoryRepository, EducationRepository
│   │   ├── domain/
│   │   │   ├── detector/       # TextIndicatorDetector, UrlIndicatorDetector, HomographDetector
│   │   │   └── engine/         # ScamAnalysisEngine (Interface), LocalHeuristicEngine, RemoteAIEngine, HybridAnalysisEngine
│   │   ├── ui/
│   │   │   ├── home/           # Dashboard hero & quick action tiles
│   │   │   ├── message/        # Paste & analyze messages with realistic samples
│   │   │   ├── url/            # URL protocol & structure inspector
│   │   │   ├── screenshot/     # Image OCR & editable review workflow
│   │   │   ├── qr/             # QR scanner with safe destination inspection
│   │   │   ├── result/         # Risk score dial, indicators list & recommendations
│   │   │   ├── history/        # Filterable scan log with individual & bulk deletion
│   │   │   ├── education/      # Cyber hygiene guides & red flag checklists
│   │   │   ├── settings/       # Theme, privacy controls, data erasure, offline toggle
│   │   │   ├── share/          # System Share Sheet receiver
│   │   │   └── widget/         # Custom Material 3 animated RiskScoreView
│   │   └── util/               # SecuritySanitizer, ShareUtils
│   ├── src/main/res/           # Complete Material 3 layouts, vector drawables & string resources
│   └── src/test/java/          # Unit test suites (Heuristics, URLs, Scoring calibration, Sanitizer)
```

---

## 🚀 Getting Started in Android Studio

1. Launch Android Studio.
2. Select **Open** and choose this project directory:
   ```
   C:\Users\USER\.gemini\antigravity\scratch\ScamShield
   ```
3. Android Studio will automatically recognize the Gradle 8.7 wrapper and configure the project.
4. Run the app on an Android device or emulator running **API 26+ (Android 8.0 to Android 15+)**.

---

## 🧪 Running Unit Tests

Run unit tests directly from Android Studio or via command line:
```bash
./gradlew testDebugUnitTest
```
All unit tests verify:
- Clean message low-risk calibration (0-20 score)
- Critical urgency and bank phishing detection
- Direct IP address and homograph punycode URLs
- URL shorteners and typosquatting prevention
- Memory DOS protection and null-byte sanitization
