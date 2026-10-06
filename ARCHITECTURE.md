# ScamShield — System Architecture & Technical Specifications

---

## 1. Architectural Philosophy

ScamShield is designed as a **defense-in-depth, privacy-first cybersecurity consumer application**. Rather than relying on a single, opaque cloud AI model, ScamShield implements a **layered, deterministic-first detection architecture** that guarantees high responsiveness, complete offline capability, and explainability.

---

## 2. Layered Detection Pipeline

```
                     ┌──────────────────────────────┐
                     │   User Submission Input      │
                     │  (Text / URL / Image / QR)   │
                     └──────────────┬───────────────┘
                                    │
                                    ▼
                     ┌──────────────────────────────┐
                     │   Security Sanitizer         │
                     │  - Truncation (100k limit)   │
                     │  - Null-byte stripping       │
                     │  - RFC URI normalization     │
                     └──────────────┬───────────────┘
                                    │
                                    ▼
                     ┌──────────────────────────────┐
                     │  Layer 1: Text Heuristics    │
                     │  - Urgency & threat patterns │
                     │  - Gift card & wire demands  │
                     │  - Crypto wallet & seed bait │
                     │  - Credential harvesting     │
                     │  - Secrecy & pressure signs  │
                     └──────────────┬───────────────┘
                                    │
                                    ▼
                     ┌──────────────────────────────┐
                     │  Layer 2: URL & Domain Eng.  │
                     │  - Protocol check (HTTP/S)   │
                     │  - IP address host detection │
                     │  - Punycode & IDN homographs │
                     │  - High-abuse disposable TLDs│
                     │  - Typosquatting / spoofing  │
                     │  - URL shortener expansion   │
                     └──────────────┬───────────────┘
                                    │
                        Offline or  │  Online & AI Enabled
                        AI Disabled │
                                    ├──────────────────────────┐
                                    │                          │
                                    ▼                          ▼
                     ┌──────────────────────┐   ┌──────────────────────────────┐
                     │ LocalHeuristicResult │   │ Layer 3: Remote AI Engine    │
                     │ (Instant On-Device)  │   │ - Structured JSON Contract   │
                     │                      │   │ - Deep contextual analysis   │
                     └──────────────┬───────┘   │ - Safe timeout & error fallb.│
                                    │           └──────────────┬───────────────┘
                                    │                          │
                                    ▼                          ▼
                     ┌─────────────────────────────────────────┐
                     │  Result Fusion & Risk Calibration       │
                     │  - Max calibrated score (0..100)        │
                     │  - Deduplicated indicators              │
                     │  - Actionable recommendations           │
                     │  - Human verification boundaries        │
                     └─────────────────────────────────────────┘
```

---

## 3. Risk Scoring & Calibration Model

The final score is a normalized risk assessment on a standard scale from `0` to `100`:

$$\text{Risk Score} = \min\left(100, \sum_{i} w_i + \text{SynergyBonus}\right)$$

### Severity Weights ($w_i$):
- **CRITICAL** (+35 points): Direct credential harvesting, gift card demands, known crypto extortion, punycode spoofing, IP literal URLs.
- **WARNING** (+18 points): Disposable phishing TLDs (.xyz, .top), URL shorteners, fake delivery surcharges, unencrypted HTTP.
- **INFO** (+8 points): Generic sensitive login keywords in URL path.
- **Synergy Bonus** (+15 points): Triggered when two or more distinct critical attack vectors co-occur (e.g., artificial urgency coupled with credential harvesting).
- **Clean Content Baseline** (5 points): Acknowledges that zero automated indicators detected cannot guarantee 100% absolute safety.

### Normalized Risk Tiers:
| Score Band | Classification Label | Color Semantics | User Guidance |
|---|---|---|---|
| **0 – 20** | **No obvious scam indicators detected** | Calm Green (`#15803D`) | Standard vigilance. Note limitations. |
| **21 – 50** | **Needs attention** | Caution Amber (`#B45309`) | Unusual patterns detected. Verify sender independently. |
| **51 – 75** | **Suspicious** | Warning Orange (`#C2410C`) | High likelihood of phishing or social engineering. Do not reply. |
| **76 – 100** | **Likely scam** | Danger Red (`#B91C1C`) | Critical scam indicators. Do not click links or send money. |

---

## 4. API Contract Documentation (Remote AI Engine)

### Request Contract (`POST /v1/analyze`)
```json
{
  "content": "URGENT: Your bank account is locked. Verify at http://chase-update.top/login",
  "scanType": "MESSAGE",
  "clientVersion": "1.0.0"
}
```

### Response Contract (`200 OK`)
```json
{
  "riskLevel": "HIGH_RISK",
  "score": 88,
  "indicators": [
    {
      "id": "AI_URGENCY",
      "title": "Artificial Urgency Tactics",
      "explanation": "Pressures victim to react immediately before verifying with bank.",
      "severity": "CRITICAL"
    },
    {
      "id": "AI_UNOFFICIAL_DOMAIN",
      "title": "Unregistered Domain Spoofing",
      "explanation": "Domain does not belong to JPMorgan Chase & Co.",
      "severity": "CRITICAL"
    }
  ],
  "whyFlagged": [
    "Threatens immediate account lockout to induce panic.",
    "Directs user to an unofficial domain mimicking a major banking institution."
  ],
  "recommendations": [
    "Do not click the link.",
    "Call Chase using the phone number on the back of your card."
  ],
  "limitations": "Automated models cannot detect compromised legitimate sender accounts."
}
```

---

## 5. Monetization-Ready Architecture

ScamShield is designed with a non-deceptive, transparent freemium architecture:

### Free Tier:
- Unlimited on-device message checks (Local Heuristic Engine)
- Unlimited URL safety inspection
- Basic screenshot OCR analysis
- QR code safety inspector
- Full access to all 9 Scam Education guides
- Local scan history (up to 20 items)

### Premium Tier (Google Play Billing):
- Cloud AI Threat Intelligence Engine (Layer 3)
- Real-time zero-day domain reputation feeds
- Unlimited historical scan retention & export
- Family Shield: Share verified safe reports across family members

> **Ethical Monetization Rule:** Critical security warnings and danger notifications are **never** gated behind a paywall.

---

## 6. Future Roadmap

1. **ML Kit Live Camera Scanner**: Direct real-time bounding box recognition of suspicious URLs on physical letters and bills.
2. **On-Device LLM (Gemini Nano via AICore)**: High-speed local generative AI inference for devices supporting Android AICore.
3. **Enterprise Threat Feed Sync**: Periodic offline database updates containing hashes of actively circulating phishing kits.
