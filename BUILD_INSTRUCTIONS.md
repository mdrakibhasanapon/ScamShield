# ScamShield — Build, Test & Release Guide

This document provides complete instructions for building, running tests, and generating production-ready Android App Bundles (AAB) for publication on the Google Play Store.

---

## 1. Prerequisites

- **Java Development Kit**: JDK 17 (or newer)
- **Android SDK**: API 34+ (Android 14) installed with Build Tools 34.0.0+
- **Gradle**: 8.7 (provided via project wrapper or standalone)
- **Android Studio**: Android Studio Hedgehog, Iguana, Jellyfish, or newer.

---

## 2. Opening in Android Studio

1. Open Android Studio (`studio64.exe`).
2. Click **File** → **Open...**
3. Navigate to:
   ```
   C:\Users\USER\.gemini\antigravity\scratch\ScamShield
   ```
4. Click **OK**.
5. Wait for the initial Gradle sync to complete. Android Studio will automatically configure the project, dependencies, and build variants.

---

## 3. Running Unit Tests

Execute the automated test suite locally to verify detection heuristics, URL parsers, and risk score calibration:

```powershell
# Set Java 17 and Android SDK environment if running from terminal
$env:JAVA_HOME = "C:\Users\USER\.gemini\antigravity\scratch\jdk-17.0.20.1+1"
$env:ANDROID_HOME = "C:\Users\USER\AppData\Local\Android\Sdk"

# Run tests
.\gradlew testDebugUnitTest
```

### Verified Test Suites:
- `LocalHeuristicEngineTest`: Validates benign message handling (score ≤ 20), urgent banking phishing (score ≥ 76), gift card payment red flags, Bitcoin wallet detection, and USPS delivery smishing.
- `UrlIndicatorDetectorTest`: Validates raw IP literal addresses, URL shortening expansions, high-risk disposable TLDs (.xyz, .top), and PayPal typosquatting.
- `RiskScoringCalibrationTest`: Asserts score ranges: Low (0-20), Attention (21-50), Suspicious (51-75), High Risk (76-100).
- `SecuritySanitizerTest`: Asserts input bounds, null byte stripping, and 100,000-character payload truncation.

---

## 4. Building the Debug APK

To compile and produce a debug APK for device testing:

```powershell
.\gradlew assembleDebug
```

The compiled APK will be located at:
```
app/build/outputs/apk/debug/app-debug.apk
```

Install directly to a connected device or emulator:
```powershell
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 5. Generating Release Android App Bundle (AAB) for Google Play

Google Play requires the **Android App Bundle (.aab)** format for all new applications.

### Step 1: Create a Release Keystore
If you do not already have an upload keystore, generate one using `keytool`:

```powershell
keytool -genkey -v -keystore scamshield-release.jks -alias scamshield -keyalg RSA -keysize 2048 -validity 10000
```
Store `scamshield-release.jks` in a secure location outside public version control.

### Step 2: Configure Signing in `app/build.gradle`
Add signing credentials securely via environment variables or a local `keystore.properties` file:

```groovy
android {
    signingConfigs {
        release {
            storeFile file(System.getenv("KEYSTORE_PATH") ?: "scamshield-release.jks")
            storePassword System.getenv("KEYSTORE_PASSWORD") ?: "your-store-password"
            keyAlias System.getenv("KEY_ALIAS") ?: "scamshield"
            keyPassword System.getenv("KEY_PASSWORD") ?: "your-key-password"
        }
    }
    buildTypes {
        release {
            signingConfig signingConfigs.release
            minifyEnabled true
            shrinkResources true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
}
```

### Step 3: Build the Release AAB
Run the bundle task:

```powershell
.\gradlew bundleRelease
```

The resulting bundle will be generated at:
```
app/build/outputs/bundle/release/app-release.aab
```

This `.aab` file is ready for direct upload to the Google Play Console under **Production**, **Open Testing**, or **Internal Testing**.
