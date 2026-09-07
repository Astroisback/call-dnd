package com.astro.autoreject;

import android.Manifest;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.telecom.Call;
import android.telecom.CallScreeningService;
import android.util.Log;

/**
 * Rejects incoming calls outright (not "silence") according to the current mode.
 *
 * Telecom binds this only while we hold the CALL_SCREENING role, and calls
 * onScreenCall() before the phone rings. setDisallowCall(true) is a real
 * disconnect, unlike setSilenceCall(true) which just mutes the ringer.
 */
public class RejectScreeningService extends CallScreeningService {

    private static final String TAG = "AutoReject";

    @Override
    public void onScreenCall(Call.Details details) {
        // Never screen outgoing calls.
        if (details.getCallDirection() != Call.Details.DIRECTION_INCOMING) {
            respondAllow(details);
            return;
        }

        int mode = Prefs.getMode(this);
        if (mode == Prefs.MODE_OFF) {
            respondAllow(details);
            return;
        }

        String number = extractNumber(details);

        // Safety: never block emergency callbacks.
        if (details.hasProperty(Call.Details.PROPERTY_EMERGENCY_CALLBACK_MODE)) {
            respondAllow(details);
            return;
        }

        // MODE_UNKNOWN spares saved contacts. Withheld numbers have no digits, so
        // they count as unknown.
        boolean targeted = (mode != Prefs.MODE_UNKNOWN) || !isKnownContact(number);

        Log.i(TAG, "mode=" + mode + " targeted=" + targeted);

        if (!targeted) {
            respondAllow(details);
        } else if (mode == Prefs.MODE_REJECT_ALL) {
            respondReject(details);
        } else {
            // Default: silence rather than disconnect, so the carrier's no-reply timer
            // keeps running and the caller still reaches voicemail.
            respondSilence(details);
        }
    }

    /**
     * Silence instead of disconnect, so the carrier's no-reply timer keeps running and
     * the call still lands on voicemail. Disconnecting would kill the call before the
     * timer elapses and the caller would never hear the greeting.
     */
    private void respondSilence(Call.Details details) {
        CallResponse response = new CallResponse.Builder()
                .setSilenceCall(true)    // mute the ringer, let the network time out
                .setSkipCallLog(false)   // keep it visible so nothing is missed silently
                .setSkipNotification(false)
                .build();
        respondToCall(details, response);
    }

    /** Hard reject: disconnects immediately. Only reaches voicemail if a busy-forward
     *  rule is armed AND the reject signals "user busy" to the network. */
    private void respondReject(Call.Details details) {
        CallResponse response = new CallResponse.Builder()
                .setDisallowCall(true)
                .setRejectCall(true)
                .setSkipCallLog(false)
                .setSkipNotification(false)
                .build();
        respondToCall(details, response);
    }

    private void respondAllow(Call.Details details) {
        respondToCall(details, new CallResponse.Builder().build());
    }

    /** @return bare number, or null when withheld. */
    private String extractNumber(Call.Details details) {
        Uri handle = details.getHandle();
        if (handle == null) {
            return null;
        }
        String number = handle.getSchemeSpecificPart();
        return (number == null || number.isEmpty()) ? null : number;
    }

    private boolean isKnownContact(String number) {
        if (number == null) {
            return false;
        }
        if (checkSelfPermission(Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            // Without contacts access we cannot tell known from unknown.
            // Fail SAFE: treat as known so we never reject everything by accident.
            Log.w(TAG, "READ_CONTACTS not granted; allowing call");
            return true;
        }

        Uri uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
        try (Cursor c = getContentResolver().query(
                uri, new String[]{ContactsContract.PhoneLookup._ID}, null, null, null)) {
            return c != null && c.moveToFirst();
        } catch (Exception e) {
            Log.w(TAG, "contact lookup failed; allowing call", e);
            return true;
        }
    }
}
