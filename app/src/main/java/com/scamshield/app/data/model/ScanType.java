package com.scamshield.app.data.model;

public enum ScanType {
    MESSAGE("Message / SMS / Email"),
    URL("Link / URL"),
    SCREENSHOT("Screenshot OCR"),
    QR_CODE("QR Code"),
    FILE_DOWNLOAD("Downloaded File / APK"),
    NETWORK_TRAFFIC("App Network & Data Traffic"),
    CHAT_MESSAGE("Chat App Message (WhatsApp/Telegram/Messenger)"),
    FINANCIAL_SMS("Mobile Financial / Payment SMS"),
    CLIPBOARD("Clipboard Copied Text");

    private final String label;

    ScanType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
