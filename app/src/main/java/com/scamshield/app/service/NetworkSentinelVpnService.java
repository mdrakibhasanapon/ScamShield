package com.scamshield.app.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;

import androidx.core.app.NotificationCompat;

import com.scamshield.app.R;
import com.scamshield.app.data.model.NetworkTrafficItem;
import com.scamshield.app.data.repository.NetworkTrafficRepository;
import com.scamshield.app.domain.scanner.NetworkPacketParser;
import com.scamshield.app.domain.scanner.NetworkThreatEvaluator;
import com.scamshield.app.ui.network.NetworkTrafficActivity;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;

public class NetworkSentinelVpnService extends VpnService implements Runnable {

    public static final String ACTION_START = "com.scamshield.app.START_VPN";
    public static final String ACTION_STOP = "com.scamshield.app.STOP_VPN";
    private static final String CHANNEL_ID = "scamshield_vpn_sentinel";
    private static final int NOTIF_ID = 9021;

    private static boolean isRunning = false;
    private Thread workerThread;
    private ParcelFileDescriptor vpnInterface;

    public static boolean isServiceRunning() {
        return isRunning;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopVpn();
            stopSelf();
            return START_NOT_STICKY;
        }

        startForeground(NOTIF_ID, createForegroundNotification());
        if (workerThread == null || !workerThread.isAlive()) {
            isRunning = true;
            workerThread = new Thread(this, "ScamShieldVpnThread");
            workerThread.start();
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        stopVpn();
        super.onDestroy();
    }

    private void stopVpn() {
        isRunning = false;
        if (workerThread != null) {
            workerThread.interrupt();
            workerThread = null;
        }
        if (vpnInterface != null) {
            try {
                vpnInterface.close();
            } catch (Exception ignored) {}
            vpnInterface = null;
        }
    }

    private Notification createForegroundNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Network Sentinel Active",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Background on-device network traffic monitor");
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) nm.createNotificationChannel(channel);
        }

        Intent openIntent = new Intent(this, NetworkTrafficActivity.class);
        PendingIntent pi = PendingIntent.getActivity(
                this, 0, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_shield_check)
                .setContentTitle("🛡️ Network Threat Sentinel Active")
                .setContentText("Monitoring live app upload/download destinations on-device.")
                .setContentIntent(pi)
                .setOngoing(true)
                .build();
    }

    @Override
    public void run() {
        try {
            Builder builder = new Builder();
            builder.setSession("ScamShield Sentinel")
                    .addAddress("10.1.10.1", 32)
                    .addRoute("0.0.0.0", 0)
                    .addDnsServer("8.8.8.8");

            vpnInterface = builder.establish();
            if (vpnInterface == null) return;

            FileInputStream in = new FileInputStream(vpnInterface.getFileDescriptor());
            byte[] packetBuffer = new byte[32767];

            NetworkTrafficRepository repo = NetworkTrafficRepository.getInstance(this);

            while (isRunning && !Thread.interrupted()) {
                int read = in.read(packetBuffer);
                if (read > 0) {
                    NetworkPacketParser.PacketInfo info = NetworkPacketParser.parseIpPacket(packetBuffer, read);
                    if (info != null && (info.isTlsClientHello || info.isHttp || info.isDns)) {
                        String host = info.hostName != null ? info.hostName : info.destIp;
                        NetworkTrafficItem item = NetworkThreatEvaluator.evaluateTraffic(
                                "android.app.active",
                                "Active Application",
                                info.destIp,
                                host,
                                info.destPort,
                                info.protocol,
                                NetworkTrafficItem.Direction.UPLOAD,
                                read,
                                0,
                                "Direct packet inspection via on-device Network Sentinel"
                        );
                        repo.logTraffic(item);
                    }
                }
            }
        } catch (Exception ignored) {
        } finally {
            isRunning = false;
        }
    }
}
