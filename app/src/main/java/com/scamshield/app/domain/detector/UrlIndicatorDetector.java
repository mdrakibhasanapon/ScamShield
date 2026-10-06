package com.scamshield.app.domain.detector;

import com.scamshield.app.data.model.DetectedIndicator;
import com.scamshield.app.data.model.IndicatorCategory;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UrlIndicatorDetector {

    // Regex for extracting URLs from arbitrary text
    public static final Pattern URL_EXTRACT_PATTERN = Pattern.compile(
            "(?i)\\b((?:https?://|www\\d{0,3}[.]|[a-z0-9.\\-]+[.][a-z]{2,63}/)(?:[^\\s()<>]+|\\((?:[^\\s()<>]+|(?:\\([^\\s()<>]+\\)))*\\))+(?:\\((?:[^\\s()<>]+|(?:\\([^\\s()<>]+\\)))*\\)|[^\\s`!()\\[\\]{};:'\".,<>?«»“”‘’]))"
    );

    // IP address host pattern (IPv4)
    private static final Pattern IP_HOST_PATTERN = Pattern.compile(
            "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
    );

    // Known URL shorteners
    private static final Set<String> URL_SHORTENERS = new HashSet<>(Arrays.asList(
            "bit.ly", "tinyurl.com", "t.co", "goo.gl", "ow.ly", "is.gd", "buff.ly",
            "adf.ly", "bit.do", "cutt.ly", "rebrand.ly", "shorte.st", "tiny.cc",
            "v.gd", "qr.ae", "rb.gy", "shorturl.at"
    ));

    // High-abuse Top-Level Domains (TLDs) frequently observed in disposable phishing kits
    private static final Set<String> HIGH_RISK_TLDS = new HashSet<>(Arrays.asList(
            "xyz", "top", "work", "icu", "buzz", "rest", "surf", "gq", "cf", "tk", "ml",
            "cc", "quest", "beauty", "hair", "skin", "monster", "cfd", "sbs", "cam", "fit"
    ));

    // High-value brand targets for typosquatting / spoofing
    private static final String[] TARGET_BRANDS = {
            "paypal", "chase", "bankofamerica", "wellsfargo", "citibank", "apple", "netflix",
            "amazon", "microsoft", "google", "binance", "coinbase", "metamask", "whatsapp",
            "telegram", "facebook", "instagram", "usps", "fedex", "ups", "dhl", "irs"
    };

    // Sensitive keyword tokens in query or path
    private static final String[] SENSITIVE_PATH_TOKENS = {
            "login", "signin", "verify", "verification", "secure", "update", "account",
            "confirm", "banking", "wallet", "password", "credential", "auth", "suspended"
    };

    public static List<String> extractUrls(String text) {
        List<String> urls = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) return urls;

        Matcher matcher = URL_EXTRACT_PATTERN.matcher(text);
        while (matcher.find()) {
            String url = matcher.group();
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "http://" + url;
            }
            urls.add(url);
        }
        return urls;
    }

    public static List<DetectedIndicator> analyzeUrl(String urlString) {
        List<DetectedIndicator> indicators = new ArrayList<>();
        if (urlString == null || urlString.trim().isEmpty()) return indicators;

        String rawUrl = urlString.trim();
        if (!rawUrl.startsWith("http://") && !rawUrl.startsWith("https://")) {
            rawUrl = "https://" + rawUrl;
        }

        try {
            URI uri = new URI(rawUrl);
            String scheme = uri.getScheme() != null ? uri.getScheme().toLowerCase(Locale.US) : "";
            String host = uri.getHost() != null ? uri.getHost().toLowerCase(Locale.US) : "";
            String path = uri.getPath() != null ? uri.getPath().toLowerCase(Locale.US) : "";
            String query = uri.getQuery() != null ? uri.getQuery().toLowerCase(Locale.US) : "";

            if (host.isEmpty()) {
                // If URI host parsing failed, attempt extraction via URL
                URL url = new URL(rawUrl);
                host = url.getHost() != null ? url.getHost().toLowerCase(Locale.US) : "";
            }

            // 1. Insecure HTTP Protocol
            if ("http".equals(scheme)) {
                indicators.add(new DetectedIndicator(
                        "URL_INSECURE_HTTP",
                        "Unencrypted HTTP Connection",
                        "The link uses unencrypted HTTP instead of secure HTTPS. Legitimate financial and login portals virtually always require HTTPS.",
                        DetectedIndicator.Severity.WARNING,
                        IndicatorCategory.URL_ANOMALY,
                        scheme + "://" + host
                ));
            }

            // 2. Direct IP Address Host
            if (IP_HOST_PATTERN.matcher(host).matches()) {
                indicators.add(new DetectedIndicator(
                        "URL_IP_ADDRESS",
                        "IP Address Used Instead of Domain",
                        "The URL directs to a raw numerical IP address (" + host + ") rather than a registered domain name. This is a common evasion tactic.",
                        DetectedIndicator.Severity.CRITICAL,
                        IndicatorCategory.URL_ANOMALY,
                        host
                ));
            }

            // 3. Known URL Shortener
            if (URL_SHORTENERS.contains(host)) {
                indicators.add(new DetectedIndicator(
                        "URL_SHORTENER",
                        "Hidden URL Destination (Shortener)",
                        "This link uses a shortening service (" + host + ") that hides the real final destination domain. Phishers use shorteners to mask malicious destinations.",
                        DetectedIndicator.Severity.WARNING,
                        IndicatorCategory.URL_ANOMALY,
                        host
                ));
            }

            // 4. Punycode or Homograph Lookalike Domain
            if (HomographDetector.containsHomographOrPunycode(host)) {
                indicators.add(new DetectedIndicator(
                        "URL_HOMOGRAPH",
                        "Punycode / Homograph Lookalike Domain",
                        "The domain contains internationalized lookalike characters (IDN/Punycode). Attackers use visual duplicates (e.g. Cyrillic letters) to spoof legitimate websites.",
                        DetectedIndicator.Severity.CRITICAL,
                        IndicatorCategory.URL_ANOMALY,
                        host
                ));
            }

            // 5. Suspicious or High-Abuse TLD
            String tld = extractTld(host);
            if (HIGH_RISK_TLDS.contains(tld)) {
                indicators.add(new DetectedIndicator(
                        "URL_HIGH_RISK_TLD",
                        "Unusual High-Risk Top-Level Domain (." + tld + ")",
                        "The domain ends in '." + tld + "', a top-level domain frequently associated with low-cost, disposable phishing operations.",
                        DetectedIndicator.Severity.WARNING,
                        IndicatorCategory.URL_ANOMALY,
                        "." + tld
                ));
            }

            // 6. Excessive Subdomains (Deep Subdomain Nesting)
            String[] domainParts = host.split("\\.");
            if (domainParts.length >= 4) {
                indicators.add(new DetectedIndicator(
                        "URL_EXCESSIVE_SUBDOMAINS",
                        "Excessive Subdomain Stacking",
                        "The domain has " + domainParts.length + " nested components. Attackers often prefix reputable brand names as subdomains (e.g., paypal.com.attacker-domain.com).",
                        DetectedIndicator.Severity.WARNING,
                        IndicatorCategory.URL_ANOMALY,
                        host
                ));
            }

            // 7. Brand Typosquatting / Impersonation in Domain or Subdomain
            for (String brand : TARGET_BRANDS) {
                if (host.contains(brand)) {
                    // Check if it's the genuine root domain (e.g., paypal.com vs paypal-security-update.com)
                    boolean isLegitimateRoot = host.equals(brand + ".com") ||
                            host.endsWith("." + brand + ".com") ||
                            host.equals(brand + ".org") ||
                            host.equals(brand + ".gov");

                    if (!isLegitimateRoot) {
                        indicators.add(new DetectedIndicator(
                                "URL_BRAND_TYPOSQUAT",
                                "Target Brand Spoofing (" + brand.toUpperCase(Locale.US) + ")",
                                "The domain contains the brand name '" + brand + "' but does not appear to be the official domain. This strongly suggests brand impersonation.",
                                DetectedIndicator.Severity.CRITICAL,
                                IndicatorCategory.IMPERSONATION,
                                host
                        ));
                        break;
                    }
                }
            }

            // 8. Sensitive Keyword Tokens in Path / Query
            String fullPath = path + (query.isEmpty() ? "" : "?" + query);
            for (String token : SENSITIVE_PATH_TOKENS) {
                if (fullPath.contains(token)) {
                    indicators.add(new DetectedIndicator(
                            "URL_SENSITIVE_KEYWORD",
                            "Login / Account Verification Keywords in URL",
                            "The link contains the sensitive security keyword '" + token + "'. Ensure you are on the verified official domain before typing any credentials.",
                            DetectedIndicator.Severity.INFO,
                            IndicatorCategory.CREDENTIAL_THEFT,
                            token
                    ));
                    break;
                }
            }

        } catch (Exception e) {
            indicators.add(new DetectedIndicator(
                    "URL_MALFORMED",
                    "Malformed or Obfuscated URL Structure",
                    "The provided URL could not be parsed according to standard RFC URI specifications. It may contain obfuscated characters.",
                    DetectedIndicator.Severity.WARNING,
                    IndicatorCategory.URL_ANOMALY,
                    rawUrl
            ));
        }

        return indicators;
    }

    private static String extractTld(String host) {
        if (host == null || host.isEmpty()) return "";
        int lastDot = host.lastIndexOf('.');
        if (lastDot >= 0 && lastDot < host.length() - 1) {
            return host.substring(lastDot + 1);
        }
        return "";
    }
}
