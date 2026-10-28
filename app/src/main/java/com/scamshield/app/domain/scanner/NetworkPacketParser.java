package com.scamshield.app.domain.scanner;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class NetworkPacketParser {

    public static class PacketInfo {
        public String srcIp;
        public String destIp;
        public int srcPort;
        public int destPort;
        public String protocol;
        public String hostName;
        public int payloadLength;
        public boolean isTlsClientHello;
        public boolean isHttp;
        public boolean isDns;
    }

    public static PacketInfo parseIpPacket(byte[] buffer, int length) {
        if (buffer == null || length < 20) return null;

        PacketInfo info = new PacketInfo();
        ByteBuffer bb = ByteBuffer.wrap(buffer, 0, length);

        int versionAndIhl = bb.get(0) & 0xFF;
        int version = (versionAndIhl >> 4) & 0x0F;
        if (version != 4) {
            // Only IPv4 detailed parsing
            return null;
        }

        int ihl = (versionAndIhl & 0x0F) * 4;
        if (length < ihl) return null;

        int protocolNum = bb.get(9) & 0xFF;

        byte[] srcIpBytes = new byte[4];
        byte[] destIpBytes = new byte[4];
        System.arraycopy(buffer, 12, srcIpBytes, 0, 4);
        System.arraycopy(buffer, 16, destIpBytes, 0, 4);

        info.srcIp = formatIp(srcIpBytes);
        info.destIp = formatIp(destIpBytes);

        int transportOffset = ihl;

        if (protocolNum == 6) { // TCP
            info.protocol = "TCP";
            if (length < transportOffset + 20) return info;

            info.srcPort = ((buffer[transportOffset] & 0xFF) << 8) | (buffer[transportOffset + 1] & 0xFF);
            info.destPort = ((buffer[transportOffset + 2] & 0xFF) << 8) | (buffer[transportOffset + 3] & 0xFF);

            int dataOffset = ((buffer[transportOffset + 12] >> 4) & 0x0F) * 4;
            int payloadOffset = transportOffset + dataOffset;
            info.payloadLength = length - payloadOffset;

            if (info.destPort == 443 || info.srcPort == 443) {
                info.protocol = "HTTPS (TLS)";
                if (payloadOffset < length) {
                    info.hostName = extractTlsSni(buffer, payloadOffset, length);
                    if (info.hostName != null) {
                        info.isTlsClientHello = true;
                    }
                }
            } else if (info.destPort == 80 || info.srcPort == 80) {
                info.protocol = "HTTP (Plaintext)";
                info.isHttp = true;
                if (payloadOffset < length) {
                    info.hostName = extractHttpHost(buffer, payloadOffset, length);
                }
            }
        } else if (protocolNum == 17) { // UDP
            info.protocol = "UDP";
            if (length < transportOffset + 8) return info;

            info.srcPort = ((buffer[transportOffset] & 0xFF) << 8) | (buffer[transportOffset + 1] & 0xFF);
            info.destPort = ((buffer[transportOffset + 2] & 0xFF) << 8) | (buffer[transportOffset + 3] & 0xFF);
            info.payloadLength = length - (transportOffset + 8);

            if (info.destPort == 53 || info.srcPort == 53) {
                info.protocol = "DNS Query";
                info.isDns = true;
                int dnsOffset = transportOffset + 8;
                info.hostName = extractDnsQuestion(buffer, dnsOffset, length);
            }
        } else {
            info.protocol = "IP-Proto-" + protocolNum;
        }

        return info;
    }

    private static String formatIp(byte[] bytes) {
        return (bytes[0] & 0xFF) + "." + (bytes[1] & 0xFF) + "." +
                (bytes[2] & 0xFF) + "." + (bytes[3] & 0xFF);
    }

    /**
     * Extracts TLS SNI (Server Name Indication) from Client Hello payload without decryption.
     */
    public static String extractTlsSni(byte[] buf, int offset, int length) {
        try {
            if (offset + 5 > length) return null;
            // Check Content Type = 22 (Handshake)
            if ((buf[offset] & 0xFF) != 0x16) return null;

            int pos = offset + 5;
            if (pos + 4 > length) return null;
            // Check Handshake Type = 1 (Client Hello)
            if ((buf[pos] & 0xFF) != 0x01) return null;

            pos += 4; // Skip type and length
            pos += 2; // Skip version
            pos += 32; // Skip random bytes

            if (pos >= length) return null;
            int sessionIdLen = buf[pos] & 0xFF;
            pos += 1 + sessionIdLen;

            if (pos + 2 > length) return null;
            int cipherSuitesLen = ((buf[pos] & 0xFF) << 8) | (buf[pos + 1] & 0xFF);
            pos += 2 + cipherSuitesLen;

            if (pos >= length) return null;
            int compMethodsLen = buf[pos] & 0xFF;
            pos += 1 + compMethodsLen;

            if (pos + 2 > length) return null;
            int extensionsLen = ((buf[pos] & 0xFF) << 8) | (buf[pos + 1] & 0xFF);
            pos += 2;

            int endExt = Math.min(length, pos + extensionsLen);
            while (pos + 4 <= endExt) {
                int extType = ((buf[pos] & 0xFF) << 8) | (buf[pos + 1] & 0xFF);
                int extLen = ((buf[pos + 2] & 0xFF) << 8) | (buf[pos + 3] & 0xFF);
                pos += 4;

                if (extType == 0) { // server_name extension
                    if (pos + 2 > endExt) break;
                    int serverNameListLen = ((buf[pos] & 0xFF) << 8) | (buf[pos + 1] & 0xFF);
                    pos += 2;

                    if (pos + 3 > endExt) break;
                    int nameType = buf[pos] & 0xFF;
                    int nameLen = ((buf[pos + 1] & 0xFF) << 8) | (buf[pos + 2] & 0xFF);
                    pos += 3;

                    if (nameType == 0 && pos + nameLen <= endExt) {
                        return new String(buf, pos, nameLen, StandardCharsets.UTF_8);
                    }
                }
                pos += extLen;
            }
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * Extracts Host header from unencrypted HTTP request.
     */
    public static String extractHttpHost(byte[] buf, int offset, int length) {
        try {
            int readLen = Math.min(length - offset, 1024);
            if (readLen <= 0) return null;
            String text = new String(buf, offset, readLen, StandardCharsets.US_ASCII);
            String[] lines = text.split("\r\n");
            for (String line : lines) {
                if (line.toLowerCase().startsWith("host:")) {
                    return line.substring(5).trim();
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * Extracts queried domain name from DNS header.
     */
    public static String extractDnsQuestion(byte[] buf, int offset, int length) {
        try {
            if (offset + 12 > length) return null;
            int qdCount = ((buf[offset + 4] & 0xFF) << 8) | (buf[offset + 5] & 0xFF);
            if (qdCount <= 0) return null;

            int pos = offset + 12;
            StringBuilder sb = new StringBuilder();
            while (pos < length) {
                int len = buf[pos] & 0xFF;
                if (len == 0) break;
                pos++;
                if (pos + len > length) break;
                if (sb.length() > 0) sb.append(".");
                sb.append(new String(buf, pos, len, StandardCharsets.US_ASCII));
                pos += len;
            }
            return sb.length() > 0 ? sb.toString() : null;
        } catch (Exception ignored) {}
        return null;
    }
}
