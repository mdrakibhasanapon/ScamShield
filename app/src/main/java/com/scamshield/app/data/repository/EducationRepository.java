package com.scamshield.app.data.repository;

import com.scamshield.app.data.model.EducationArticle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EducationRepository {

    public static List<EducationArticle> getArticles() {
        List<EducationArticle> list = new ArrayList<>();

        // 1. Phishing & Spear-Phishing
        list.add(new EducationArticle(
                "edu_phishing",
                "Phishing & Spear-Phishing",
                "How fake emails & messages lure you into divulging passwords and data",
                "Email & Messaging",
                "Phishing involves deceptive messages sent by criminals masquerading as trusted contacts, banks, or services to trick you into clicking malicious links or disclosing private credentials.",
                Arrays.asList(
                        "Urgent threats to close your account immediately",
                        "Subtle typos in the sender's email domain (e.g., support@paypa1.com)",
                        "Generic greetings like 'Dear Customer' rather than your name",
                        "Requests to re-authenticate or verify personal security questions"
                ),
                Arrays.asList(
                        "Never click links inside unexpected verification emails",
                        "Manually open your browser and navigate to the verified portal directly",
                        "Enable multi-factor authentication (MFA) with authenticator apps, not SMS"
                ),
                "An email claiming your Netflix subscription has expired with a link to an unverified payment update domain."
        ));

        // 2. Fake Delivery Scams
        list.add(new EducationArticle(
                "edu_delivery",
                "Fake Delivery & Parcel Surcharges",
                "SMS alerts claiming incomplete delivery addresses or unpaid customs fees",
                "SMS / Smishing",
                "Smishing attackers send mass SMS messages claiming you have a parcel awaiting delivery that requires a tiny payment ($1 to $3) to reschedule, aiming to capture your credit card number.",
                Arrays.asList(
                        "Unsolicited SMS from an unknown mobile number about an unspecified package",
                        "Shortened URLs (bit.ly, tinyurl) or domains with strange extensions (.xyz, .top)",
                        "Demands for small fee payments to release the delivery"
                ),
                Arrays.asList(
                        "Check your recent purchases inside your shopping apps (Amazon, eBay, etc.)",
                        "Track packages solely using tracking numbers on official courier websites",
                        "Never enter payment card details on links received via SMS"
                ),
                "'USPS: Your package has an unpaid redelivery fee of $1.50. Update details at link-delivery-usps.com'"
        ));

        // 3. Fake Job & Employment Fraud
        list.add(new EducationArticle(
                "edu_job_scams",
                "Work-From-Home & Job Offer Scams",
                "Unsolicited high-paying remote job offers requiring upfront payments",
                "Career & Employment",
                "Fraudulent recruiters offer high hourly pay for minimal remote tasks, then instruct you to purchase equipment with a counterfeit check or forward mysterious packages.",
                Arrays.asList(
                        "Job offers extended without an interview or formal application",
                        "Communication conducted entirely over Telegram, WhatsApp, or Signal",
                        "You are sent a check to buy office equipment from a 'designated vendor'"
                ),
                Arrays.asList(
                        "Research companies on LinkedIn and contact their HR department directly",
                        "Never deposit a check and wire back a portion of the funds",
                        "Legitimate employers provide required work hardware at no cost to you"
                ),
                "'Earn $500/day as a remote packaging inspector. Deposit this $2,000 equipment check and wire the surplus.'"
        ));

        // 4. Crypto & Investment Fraud
        list.add(new EducationArticle(
                "edu_crypto",
                "Crypto & High-Yield Investment Fraud",
                "Promises of guaranteed returns, fake exchanges, and 'pig butchering'",
                "Finance & Crypto",
                "Scammers build online rapport or advertise trading algorithms that show fake exponential profits, only to lock your funds and demand endless 'tax fees' when you try to withdraw.",
                Arrays.asList(
                        "Guarantees of zero-risk, high-yield profits",
                        "Unsolicited investment tips from romantic interests met online",
                        "Requests to deposit cryptocurrency into unfamiliar exchanges",
                        "Demands for upfront 'withdrawal taxes' or 'gas fees'"
                ),
                Arrays.asList(
                        "Understand that all investments carry risk; guaranteed return is a scam hallmark",
                        "Never send crypto to private wallet addresses based on internet advice",
                        "Never share your 12- or 24-word wallet recovery seed phrase"
                ),
                "'Deposit $500 into our automated AI crypto pool and earn $5,000 every week guaranteed.'"
        ));

        // 5. Romance Scams
        list.add(new EducationArticle(
                "edu_romance",
                "Romance & Catfishing Scams",
                "Emotional manipulation designed to empty your bank account",
                "Social & Dating",
                "Scammers adopt attractive fake personas on dating apps or social networks, quickly profess love, and fabricate emergencies requiring urgent financial assistance.",
                Arrays.asList(
                        "Rapid declarations of love before meeting in person",
                        "Excuses to avoid video calls (broken camera, deployed overseas)",
                        "Sudden emergency medical bills, stranded travel funds, or lucrative investment advice"
                ),
                Arrays.asList(
                        "Perform a reverse image search on the person's profile photos",
                        "Never send money, gift cards, or crypto to someone you haven't met in person",
                        "Confide in trusted friends or family if an online acquaintance requests money"
                ),
                "An overseas engineer in love with you who urgently needs $1,500 wired to clear emergency customs papers."
        ));

        // 6. Bank & Government Impersonation
        list.add(new EducationArticle(
                "edu_bank_gov",
                "Bank & Government Impersonation",
                "Caller ID spoofing and urgent threats of legal action or account freezes",
                "Authority Impersonation",
                "Criminals pose as the IRS, Social Security Administration, or your bank's fraud department, claiming your funds are in danger unless moved to a 'safe government locker'.",
                Arrays.asList(
                        "Caller claims to be law enforcement threatening immediate arrest",
                        "Demands to withdraw cash and deposit into a Bitcoin ATM or buy retail gift cards",
                        "Caller instructs you to lie to your bank teller if asked questions"
                ),
                Arrays.asList(
                        "Hang up immediately. Government agencies never call threatening arrest",
                        "Call your bank using the phone number printed on the back of your debit card",
                        "There is never any such thing as a 'safe government holding account'"
                ),
                "'This is Officer Miller from the Federal Police. Your SSN was tied to a drug crime. Wire funds now to clear your name.'"
        ));

        // 7. Tech Support Scams
        list.add(new EducationArticle(
                "edu_tech_support",
                "Tech Support & Pop-up Scams",
                "Fake virus warnings urging you to call a toll-free number",
                "Technical & Software",
                "Browser pop-ups lock your screen with loud audio alarms claiming your computer is infected with spyware, instructing you to call a support number where scammers install remote access tools.",
                Arrays.asList(
                        "Full-screen browser pop-ups that prevent you from closing the tab",
                        "Technician asks you to download AnyDesk, TeamViewer, or UltraViewer",
                        "Technician claims your computer has thousands of errors and demands hundreds of dollars"
                ),
                Arrays.asList(
                        "Force-close your browser using Task Manager or swipe away the app",
                        "Neither Microsoft nor Apple will ever display a phone number to call for virus support",
                        "Never grant remote control of your computer or phone to unknown callers"
                ),
                "'VIRUS ALERT: Windows Defender has blocked your computer. Call 1-800-XXX-XXXX immediately.'"
        ));

        // 8. Quishing (QR Code Scams)
        list.add(new EducationArticle(
                "edu_qr_scams",
                "Quishing (Malicious QR Codes)",
                "Tampered parking meters, restaurant menus, and physical stickers",
                "Physical & QR",
                "Scammers place counterfeit QR code stickers over genuine parking meters or flyers. Scanning them takes you to identical spoofed payment portals.",
                Arrays.asList(
                        "QR code stickers visibly pasted on top of existing signage or parking meters",
                        "QR code destination URL uses unexpected domains or shorteners",
                        "Landing page immediately demands credit card or login credentials"
                ),
                Arrays.asList(
                        "Inspect physical QR stickers to verify they haven't been pasted over the original",
                        "Always review the decoded destination domain before opening the link",
                        "Use ScamShield's QR Scanner to inspect the hidden link safely before proceeding"
                ),
                "A physical sticker pasted over a city parking meter directing drivers to a fraudulent payment portal."
        ));

        // 9. Account Takeover & 2FA Theft
        list.add(new EducationArticle(
                "edu_account_takeover",
                "Account Takeover & 2FA Interception",
                "How scammers bypass two-factor authentication codes",
                "Account Security",
                "Scammers who have already learned your password trigger a legitimate 2FA code, then trick you into revealing the code by claiming it is to 'cancel an unauthorized charge'.",
                Arrays.asList(
                        "You receive an unsolicited SMS with a 6-digit verification code",
                        "Caller asks: 'I just sent a code to your phone, please read it back to verify your identity'",
                        "Text explicitly says 'Do not share this code with anyone', including company staff"
                ),
                Arrays.asList(
                        "Never read back or paste security codes sent to your phone to anyone",
                        "If you receive unexpected codes, log in directly and change your password immediately",
                        "Switch to hardware security keys or authenticator apps (TOTP) instead of SMS 2FA"
                ),
                "'A fraudster is attempting to drain your account. Read back the 6-digit confirmation code we texted you to cancel it.'"
        ));

        return list;
    }

    public static EducationArticle getArticleById(String id) {
        for (EducationArticle article : getArticles()) {
            if (article.getId().equals(id)) {
                return article;
            }
        }
        return null;
    }
}
