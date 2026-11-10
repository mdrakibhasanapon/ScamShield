package com.scamshield.app.domain.scanner;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;

import com.scamshield.app.util.PermissionHelper;

import java.util.Locale;

public class LocationScamRadar {

    public static boolean isLocationPermitted(Context context) {
        return PermissionHelper.hasLocationPermission(context);
    }

    public static String getCoarseLocationSummary(Context context) {
        if (!isLocationPermitted(context)) {
            return "Location Permission Required for Regional Threat Radar";
        }

        try {
            LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            if (lm != null) {
                Location lastKnown = null;
                try {
                    lastKnown = lm.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
                } catch (SecurityException ignored) {}

                if (lastKnown == null) {
                    try {
                        lastKnown = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    } catch (SecurityException ignored) {}
                }

                if (lastKnown != null) {
                    return String.format(Locale.getDefault(), "Regional Zone Active (Lat: %.2f, Lon: %.2f) • Shield Active",
                            lastKnown.getLatitude(), lastKnown.getLongitude());
                }
            }
        } catch (Exception ignored) {}

        return "Regional Scam Radar Active • GPS Protection Ready";
    }

    public static boolean isConnectedToPublicUnsecuredWifi(Context context) {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                NetworkCapabilities caps = cm.getNetworkCapabilities(cm.getActiveNetwork());
                if (caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    // Check if network is captive portal or unvalidated
                    return !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
                }
            }
        } catch (Exception ignored) {}
        return false;
    }
}
