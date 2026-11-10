# Privacy Policy for ScamShield

**Last Updated:** November 11, 2026

**ScamShield** ("we", "our", or "us") is dedicated to protecting your privacy and security. This Privacy Policy details how our mobile application collects, uses, and safeguards information when you use ScamShield.

ScamShield is engineered with a **Strict On-Device Architecture**. We prioritize **Zero Cloud Exfiltration** and data minimization. We do not sell, monetize, rent, or trade your personal data.

---

## 1. Summary of On-Device Processing

* **Local-First Threat Analysis:** All heuristic evaluations, URL inspections, optical character recognition (OCR), and scam risk assessments are computed directly on your device CPU/GPU.
* **No Remote Cloud Storage:** We do not host central servers that store your scanned text, emails, screenshots, contacts, audio transcripts, or location coordinates.
* **Zero Tracking:** We do not profile your usage or track your identity across apps or websites.

---

## 2. Permissions & Data Access Disclosures

In accordance with Google Play's User Data and Prominent Disclosure Policies, here is why specific Android permissions are requested:

### A. Notifications & Universal Messaging (`BIND_NOTIFICATION_LISTENER_SERVICE`)
* **Purpose:** To detect phishing URLs and fraud attempts in incoming message notifications from supported communication applications (e.g., WhatsApp, Telegram, Facebook Messenger, Signal, SMS).
* **Data Handling:** Message notification text is analyzed in volatile device memory and discarded immediately after threat score calculation. No message contents are transmitted to external servers.

### B. Microphone & Audio Access (`RECORD_AUDIO`)
* **Purpose:** Powers the Voice Phishing (Vishing) Shield, analyzing spoken phone conversation transcripts against extortion patterns, bank fraud impersonation, and fraudulent payment demands.
* **Data Handling:** Audio transcripts are processed strictly in real-time RAM on-device. Audio files are never recorded, compressed, stored to disk, or transmitted over the internet.

### C. Location Access (`ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`)
* **Purpose:** Powers the Regional Threat Radar, identifying unencrypted/spoofed public Wi-Fi access points and correlating threat advisories relevant to the user's general geographic vicinity.
* **Data Handling:** Location coordinates are ephemeral, processed purely on-device, and never uploaded to remote databases or shared with third parties.

### D. Contacts & Safe Senders (`READ_CONTACTS`)
* **Purpose:** Allows users to sync trusted family, friends, and verified organizations into a local Safe Sender Whitelist, preventing false-positive alerts when communicating with recognized numbers.
* **Data Handling:** Contact names and numbers are stored exclusively in your device's local SQLite database. They are never transmitted off your device.

### E. Installed Applications (`QUERY_ALL_PACKAGES`)
* **Purpose:** Powers the App Permission & Privacy Guard (Antivirus/Malware Scanner) to inspect installed package permissions (detecting hidden spyware, unauthorized mic/camera access, or suspicious sideloaded APKs).
* **Data Handling:** Application inventories are inspected locally and are never transmitted to external analytics providers.

### F. Network & Data Usage (`PACKAGE_USAGE_STATS`, `BIND_VPN_SERVICE`)
* **Purpose:** Monitors per-app network traffic on-device to alert you if a suspicious background app is exfiltrating data or downloading unauthorized files.
* **Data Handling:** VPN filtering and traffic statistics are processed on a local virtual loopback interface on-device without routing data through remote proxy servers.

---

## 3. Third-Party Services & Advertising

ScamShield integrates the **Google Mobile Ads (AdMob) SDK** to display optional non-intrusive advertisements:
* AdMob may collect and process pseudonymous device identifiers (such as the Google Advertising ID), IP addresses, and diagnostic crash logs in accordance with Google's Privacy Policy.
* For more information on how Google manages advertising data, visit: [Google Privacy & Terms](https://policies.google.com/technologies/ads).

---

## 4. Data Retention & Deletion Rights

* **User Data Sovereignty:** You have total control over your local security records.
* **One-Tap Data Erasure:** Navigating to `Settings -> Erase All Data` immediately purges all scan logs, cached assessments, and trusted contacts from your device's SQLite database.
* **Uninstallation:** Uninstalling the application deletes all locally stored preferences, databases, and cached records completely.

---

## 5. Security & Protection Measures

* All internal databases are sandboxed within the application's private internal storage directory (`Context.MODE_PRIVATE`).
* ScamShield explicitly disables cloud backups (`android:allowBackup="false"`) and configures strict `data_extraction_rules` to prevent Android automated cloud backups from copying sensitive security logs.

---

## 6. Children's Privacy

ScamShield does not knowingly collect or solicit personal information from children under the age of 13.

---

## 7. Changes to This Privacy Policy

We may update our Privacy Policy periodically to reflect technological or regulatory requirements. Any revisions will be published in this repository with an updated revision date.

---

## 8. Contact Us

If you have questions, feedback, or concerns regarding this Privacy Policy or ScamShield's privacy practices:
* **Developer:** Md. Rakib Hasan Apon
* **GitHub Repository:** [https://github.com/mdrakibhasanapon/ScamShield](https://github.com/mdrakibhasanapon/ScamShield)
