package com.astro.autoreject;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Settings screen for the network-side rules. These are MMI codes: the values live in
 * the carrier's registers, not in this app, so they persist across reboots and apply
 * even when the phone is off or out of coverage.
 */
public class ForwardingActivity extends Activity {

    private TextView timerLabel;
    private EditText numberField;

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        setTitle("Voicemail rules");

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(24);
        root.setPadding(pad, pad, pad, pad);

        root.addView(caption("Voicemail number"));
        numberField = new EditText(this);
        numberField.setHint("+914042180038");
        numberField.setText(Prefs.getVoicemailNumber(this));
        root.addView(numberField);
        root.addView(button("Save number", v -> saveNumber()));

        root.addView(spacer());
        timerLabel = new TextView(this);
        timerLabel.setTextSize(15f);
        root.addView(timerLabel);

        // GSM allows 5..30 in steps of 5, so the bar has 6 stops.
        SeekBar bar = new SeekBar(this);
        bar.setMax((Forwarding.MAX_TIMER - Forwarding.MIN_TIMER) / Forwarding.STEP_TIMER);
        bar.setProgress(
                (Prefs.getRingTimer(this) - Forwarding.MIN_TIMER) / Forwarding.STEP_TIMER);
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                Prefs.setRingTimer(ForwardingActivity.this,
                        Forwarding.MIN_TIMER + progress * Forwarding.STEP_TIMER);
                renderTimer();
            }

            @Override
            public void onStartTrackingTouch(SeekBar sb) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar sb) {
            }
        });
        root.addView(bar);
        root.addView(button("Apply ring timer rule", v ->
                Forwarding.setNoReplyTimer(this, number(), Prefs.getRingTimer(this))));

        root.addView(spacer());
        root.addView(caption("Busy rule"));
        TextView busyWarning = new TextView(this);
        busyWarning.setTextSize(13f);
        busyWarning.setTextColor(Color.RED);
        busyWarning.setText("Not recommended. GSM uses ONE \"busy\" condition for both "
                + "\"you rejected the call\" and \"you are already on a call\", so arming "
                + "this replaces call waiting: a second caller is diverted to voicemail "
                + "with no beep and no on-screen prompt. Confirmed on this device.");
        root.addView(busyWarning);
        root.addView(button("Cancel busy rule (restore call waiting)",
                v -> Forwarding.disableBusy(this)));

        root.addView(spacer());
        root.addView(caption("Rule: send everything to voicemail"));
        root.addView(button("Enable (phone never rings)", v -> {
            Forwarding.enableAll(this, number());
            Prefs.setForwardingOn(this, true);
        }));
        root.addView(button("Disable", v -> {
            Forwarding.disableAll(this);
            Prefs.setForwardingOn(this, false);
        }));

        root.addView(spacer());
        root.addView(caption("Check what the network has stored"));
        root.addView(button("Query no-reply rule", v -> Forwarding.queryNoReply(this)));
        root.addView(button("Query all-calls rule", v -> Forwarding.queryAll(this)));

        TextView note = new TextView(this);
        note.setTextSize(13f);
        note.setTextColor(Color.GRAY);
        note.setText("These rules are stored by your carrier, so they survive reboots "
                + "and work even when the phone is off.\n\n"
                + "The busy rule only catches rejected calls if your network reports "
                + "the reject as \"user busy\". Test it with a second phone.");
        root.addView(spacer());
        root.addView(note);

        android.widget.ScrollView scroll = new android.widget.ScrollView(this);
        scroll.addView(root);
        setContentView(scroll);

        renderTimer();
    }

    private void renderTimer() {
        timerLabel.setText("Ring for " + Prefs.getRingTimer(this)
                + "s before voicemail picks up");
    }

    private String number() {
        return Prefs.getVoicemailNumber(this);
    }

    private void saveNumber() {
        String value = numberField.getText().toString().trim();
        if (value.isEmpty()) {
            Toast.makeText(this, "Enter a number first", Toast.LENGTH_SHORT).show();
            return;
        }
        Prefs.setVoicemailNumber(this, value);
        Toast.makeText(this, "Saved " + value, Toast.LENGTH_SHORT).show();
    }

    private TextView caption(String text) {
        TextView t = new TextView(this);
        t.setTextSize(16f);
        t.setPadding(0, dp(8), 0, dp(4));
        t.setText(text);
        return t;
    }

    private Button button(String text, View.OnClickListener onClick) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setOnClickListener(onClick);
        return b;
    }

    private View spacer() {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(16)));
        return v;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
