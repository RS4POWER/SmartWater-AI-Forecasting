package com.example.citirecontoare;

import android.content.Context;
import android.content.SharedPreferences;

public class RouteTracker {
    private static final String PREF_NAME = "RoutePrefs";

    // Salvăm dacă tracking-ul e activ
    public static void startRoute(Context context, String startHouse) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putLong("startTime", System.currentTimeMillis())
                .putString("startHouse", startHouse)
                .putBoolean("isTracking", true)
                .apply();
    }

    public static boolean isTracking(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getBoolean("isTracking", false);
    }

    public static long getStartTime(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getLong("startTime", 0);
    }

    public static String getStartHouse(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getString("startHouse", "");
    }

    public static void stopTracking(Context context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit()
                .putBoolean("isTracking", false)
                .apply();
    }

    public static void clearAll(Context context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit().clear().apply();
    }

    // Salvăm ultima casă vizitată
    public static void updateLastHouse(Context context, String houseName) {
        context.getSharedPreferences("RoutePrefs", Context.MODE_PRIVATE).edit()
                .putString("lastHouse", houseName)
                .apply();
    }

    public static String getLastHouse(Context context) {
        return context.getSharedPreferences("RoutePrefs", Context.MODE_PRIVATE)
                .getString("lastHouse", "N/A");
    }
}