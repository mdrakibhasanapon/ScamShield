package com.scamshield.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.scamshield.app.data.model.DetectedIndicator;
import com.scamshield.app.domain.detector.UrlIndicatorDetector;

import org.junit.Test;

import java.util.List;

public class UrlIndicatorDetectorTest {

    @Test
    public void testDirectIpHostDetection() {
        String ipUrl = "http://192.168.1.50/banking/login";
        List<DetectedIndicator> indicators = UrlIndicatorDetector.analyzeUrl(ipUrl);

        boolean foundIp = indicators.stream().anyMatch(i -> "URL_IP_ADDRESS".equals(i.getId()));
        boolean foundInsecure = indicators.stream().anyMatch(i -> "URL_INSECURE_HTTP".equals(i.getId()));

        assertTrue(foundIp);
        assertTrue(foundInsecure);
    }

    @Test
    public void testUrlShortenerDetection() {
        String shortUrl = "https://bit.ly/3xY8abc";
        List<DetectedIndicator> indicators = UrlIndicatorDetector.analyzeUrl(shortUrl);

        boolean foundShortener = indicators.stream().anyMatch(i -> "URL_SHORTENER".equals(i.getId()));
        assertTrue(foundShortener);
    }

    @Test
    public void testHighRiskTldDetection() {
        String badTldUrl = "https://secure-portal-update.xyz/login";
        List<DetectedIndicator> indicators = UrlIndicatorDetector.analyzeUrl(badTldUrl);

        boolean foundTld = indicators.stream().anyMatch(i -> "URL_HIGH_RISK_TLD".equals(i.getId()));
        assertTrue(foundTld);
    }

    @Test
    public void testBrandTyposquattingDetection() {
        String spoofUrl = "https://paypal-security-verification-center.com/signin";
        List<DetectedIndicator> indicators = UrlIndicatorDetector.analyzeUrl(spoofUrl);

        boolean foundTyposquat = indicators.stream().anyMatch(i -> "URL_BRAND_TYPOSQUAT".equals(i.getId()));
        assertTrue(foundTyposquat);
    }

    @Test
    public void testGenuineBrandUrlIsNotFlaggedAsTyposquat() {
        String genuineUrl = "https://paypal.com/signin";
        List<DetectedIndicator> indicators = UrlIndicatorDetector.analyzeUrl(genuineUrl);

        boolean foundTyposquat = indicators.stream().anyMatch(i -> "URL_BRAND_TYPOSQUAT".equals(i.getId()));
        assertFalse(foundTyposquat);
    }

    @Test
    public void testExtractMultipleUrlsFromMessage() {
        String msg = "Check this out https://bit.ly/deal and then see http://192.168.1.1/info";
        List<String> urls = UrlIndicatorDetector.extractUrls(msg);

        assertEquals(2, urls.size());
        assertTrue(urls.contains("https://bit.ly/deal"));
    }
}
