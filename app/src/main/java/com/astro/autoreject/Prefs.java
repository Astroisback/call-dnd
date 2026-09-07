package com.astro.autoreject;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Single source of truth shared by both tiles, the settings screen and the screening
 * service, so they can never disagree about the active mode.
 */
final class Prefs {

    /** Let everything ring normally. */
    static final int MODE_OFF = 0;
    /** Silence unknown callers; the carrier no-reply rule takes them to voicemail. */
    static final int MODE_UNKNOWN = 1;
    /** Silence every caller. */
    static final int MODE_ALL = 2;
    /** Hard-reject every caller. Needs the busy rule armed to reach voicemail. */
    static final int MODE_REJECT_ALL = 3;

    private static final int MODE_COUNT = 4;

    private static final String FILE = "autoreject";
    private static final String KEY_MODE = "mode";
    private static final String KEY_FORWARDING = "forwarding_on";
    private static final String KEY_VM_NUMBER = "vm_number";
    private static final String KEY_TIMER = "ring_timer";

    private Prefs() {
    }

    private static SharedPreferences prefs(Context ctx) {
        // Device-protected storage: readable before first unlock, so screening
        // still works if the phone reboots and sits at the lock screen.
        Context safe = ctx.createDeviceProtectedStorageContext();
        return safe.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    static int getMode(Context ctx) {
        return prefs(ctx).getInt(KEY_MODE, MODE_OFF);
    }

    static void setMode(Context ctx, int mode) {
        prefs(ctx).edit().putInt(KEY_MODE, mode).apply();
    }

    static int nextMode(Context ctx) {
        return (getMode(ctx) + 1) % MODE_COUNT;
    }

    /** True for any mode that suppresses incoming calls. */
    static boolean isActive(int mode) {
        return mode != MODE_OFF;
    }

    static String label(int mode) {
        switch (mode) {
            case MODE_UNKNOWN:
                return "Unknown callers";
            case MODE_ALL:
                return "All callers";
            case MODE_REJECT_ALL:
                return "Reject all";
            default:
                return "Off";
        }
    }

    /**
     * Tracked locally: reading the real network forwarding state needs privileged
     * telephony access. Long-press the tile to query the network instead.
     */
    static boolean isForwardingOn(Context ctx) {
        return prefs(ctx).getBoolean(KEY_FORWARDING, false);
    }

    static void setForwardingOn(Context ctx, boolean on) {
        prefs(ctx).edit().putBoolean(KEY_FORWARDING, on).apply();
    }

    static String getVoicemailNumber(Context ctx) {
        return prefs(ctx).getString(KEY_VM_NUMBER, Forwarding.DEFAULT_VOICEMAIL);
    }

    static void setVoicemailNumber(Context ctx, String number) {
        prefs(ctx).edit().putString(KEY_VM_NUMBER, number).apply();
    }

    static int getRingTimer(Context ctx) {
        return prefs(ctx).getInt(KEY_TIMER, Forwarding.MAX_TIMER);
    }

    static void setRingTimer(Context ctx, int seconds) {
        prefs(ctx).edit().putInt(KEY_TIMER, Forwarding.clampTimer(seconds)).apply();
    }
}
