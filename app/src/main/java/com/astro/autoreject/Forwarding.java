package com.astro.autoreject;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

/**
 * Drives carrier call-forwarding via MMI codes.
 *
 * Why MMI and not a reject: voicemail lives on the NETWORK. Truecaller configured
 * conditional forwarding "no reply after 30s" to +914042180038. A screening-service
 * reject disconnects instantly, so that 30s timer never elapses and the caller never
 * hears the greeting. Sending UNCONDITIONAL forwarding instead makes the network
 * intercept the call before the phone rings, so the caller always gets voicemail.
 *
 * `carrier_supports_ss_over_ut_bool = false` on this SIM, so supplementary services
 * go over CS/MMI rather than UT/XCAP. MMI is the correct mechanism here.
 */
final class Forwarding {

    /** Truecaller voicemail entry point (from `*#61#` on this SIM). */
    static final String DEFAULT_VOICEMAIL = "+914042180038";

    /**
     * No-reply ring timer, seconds. GSM allows 5..30 in steps of 5; Truecaller set 30.
     * Dropping to 5 gives the iPhone feel: a moment of ringing, then the greeting.
     */
    static final int MIN_TIMER = 5;
    static final int MAX_TIMER = 30;
    static final int STEP_TIMER = 5;

    private Forwarding() {
    }

    /** Clamp to the nearest legal GSM value; the network rejects anything else. */
    static int clampTimer(int seconds) {
        int rounded = Math.round(seconds / (float) STEP_TIMER) * STEP_TIMER;
        return Math.max(MIN_TIMER, Math.min(MAX_TIMER, rounded));
    }

    /**
     * Re-arm the no-reply rule with a custom ring timer.
     * `11` is the basic-service code for voice telephony.
     */
    static void setNoReplyTimer(Context ctx, String number, int seconds) {
        dial(ctx, "**61*" + number + "*11*" + clampTimer(seconds) + "#");
    }

    /**
     * Forward-when-busy. DO NOT enable this by default.
     *
     * CONFIRMED on this device 2026-09-08: rejecting a call DOES signal "user busy", so
     * CFB does catch rejects. But GSM has only ONE busy condition, shared with "already
     * on a call", so arming CFB also REPLACES CALL WAITING - a second caller is diverted
     * to voicemail with no beep and no on-screen prompt. Calls appear to vanish mid-call.
     * The two behaviours cannot be separated; it is one register in the network.
     *
     * Kept only so the app can offer an explicit, warned opt-in and a cancel button.
     */
    static void enableBusy(Context ctx, String number) {
        dial(ctx, "**67*" + number + "#");
    }

    static void disableBusy(Context ctx) {
        dial(ctx, "##67#");
    }

    /** Ask the network to report the no-reply rule and its timer. */
    static void queryNoReply(Context ctx) {
        dial(ctx, "*#61#");
    }

    /** All incoming calls -> voicemail, before the phone rings. */
    static void enableAll(Context ctx, String number) {
        dial(ctx, "**21*" + number + "#");
    }

    /** Cancel unconditional forwarding; the 30s no-reply rule stays intact. */
    static void disableAll(Context ctx) {
        dial(ctx, "##21#");
    }

    /** Ask the network to report the current unconditional-forwarding state. */
    static void queryAll(Context ctx) {
        dial(ctx, "*#21#");
    }

    /**
     * MMI strings must be dialled, not "called". ACTION_CALL with a tel: URI hands the
     * string to Telecom, which recognises it as an MMI request and sends it to the
     * network. '#' has to be percent-encoded or it is parsed as a URI fragment.
     */
    private static void dial(Context ctx, String mmi) {
        Uri uri = Uri.fromParts("tel", mmi, null);
        // Using ACTION_DIAL so the dialer opens and the user can initiate the code.
        // This avoids crashes if the CALL_PHONE permission wasn't granted at runtime.
        Intent intent = new Intent(Intent.ACTION_DIAL, uri);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ctx.startActivity(intent);
    }
}
