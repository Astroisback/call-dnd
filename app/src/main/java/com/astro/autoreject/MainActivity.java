package com.astro.autoreject;

import android.Manifest;
import android.app.Activity;
import android.app.role.RoleManager;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Minimal UI: request the CALL_SCREENING role, request contacts access, pick a mode.
 * Everything here is also reachable from the Quick Settings tile.
 */
public class MainActivity extends Activity {

    private static final int REQ_ROLE = 1;
    private static final int REQ_CONTACTS = 2;

    private TextView status;

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(24);
        root.setPadding(pad, pad, pad, pad);

        status = new TextView(this);
        status.setTextSize(15f);
        root.addView(status);

        root.addView(spacer());
        root.addView(button("Grant screening role", v -> requestRole()));
        root.addView(button("Allow contacts access", v -> requestContacts()));
        root.addView(button("Allow phone calls (for MMI codes)", v -> requestCallPhone()));
        root.addView(button("Allow modify settings (for Game Space reject)", v -> requestWriteSettings()));
        root.addView(button("Allow display over other apps", v -> requestOverlay()));

        root.addView(spacer());
        root.addView(button("Mode: Off", v -> setMode(Prefs.MODE_OFF)));
        root.addView(button("Mode: Silence unknown callers", v -> setMode(Prefs.MODE_UNKNOWN)));
        root.addView(button("Mode: Silence all callers", v -> setMode(Prefs.MODE_ALL)));
        root.addView(button("Mode: Hard-reject all (may skip voicemail)",
                v -> setMode(Prefs.MODE_REJECT_ALL)));

        root.addView(spacer());
        root.addView(button("Voicemail rules (number, ring timer, forwarding)",
                v -> startActivity(new Intent(this, ForwardingActivity.class))));

        TextView hint = new TextView(this);
        hint.setTextSize(13f);
        hint.setTextColor(Color.GRAY);
        hint.setText("Two Quick Settings tiles ship with this app:\n"
                + "  \u2022 Call DND \u2014 cycles the screening modes above.\n"
                + "  \u2022 Voicemail \u2014 sends every incoming call straight to voicemail.\n\n"
                + "Silence modes let the carrier ring timer expire, so callers still reach "
                + "voicemail. Hard-reject disconnects instantly; only useful if your busy-"
                + "forwarding rule is armed AND your network treats reject as \"user busy\".\n\n"
                + "This only affects normal phone calls. WhatsApp and similar apps manage "
                + "their own calls and cannot be screened this way.");
        root.addView(spacer());
        root.addView(hint);

        setContentView(root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void setMode(int mode) {
        Prefs.setMode(this, mode);
        refresh();
    }

    private void refresh() {
        RoleManager rm = getSystemService(RoleManager.class);
        boolean held = rm != null && rm.isRoleHeld(RoleManager.ROLE_CALL_SCREENING);
        boolean contacts = checkSelfPermission(Manifest.permission.READ_CONTACTS)
                == android.content.pm.PackageManager.PERMISSION_GRANTED;
        boolean callPhone = checkSelfPermission(Manifest.permission.CALL_PHONE)
                == android.content.pm.PackageManager.PERMISSION_GRANTED;
        boolean writeSettings = Settings.System.canWrite(this);
        boolean overlay = Settings.canDrawOverlays(this);

        status.setText("Screening role: " + (held ? "granted" : "NOT granted")
                + "\nContacts access: " + (contacts ? "granted" : "not granted")
                + "\nPhone calls: " + (callPhone ? "granted" : "not granted")
                + "\nModify settings: " + (writeSettings ? "granted" : "not granted")
                + "\nOverlay: " + (overlay ? "granted" : "not granted")
                + "\nCurrent mode: " + Prefs.label(Prefs.getMode(this))
                + "\n\nAdd this app to Game Space, then when the app is in the "
                + "foreground and a DND mode is active, all incoming calls will be "
                + "rejected at the system level.");
    }

    private void requestRole() {
        RoleManager rm = getSystemService(RoleManager.class);
        if (rm == null || !rm.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
            return;
        }
        startActivityForResult(
                rm.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING), REQ_ROLE);
    }

    private void requestContacts() {
        requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, REQ_CONTACTS);
    }

    private void requestCallPhone() {
        requestPermissions(new String[]{Manifest.permission.CALL_PHONE}, 3);
    }

    private void requestWriteSettings() {
        if (!Settings.System.canWrite(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,
                    Uri.parse("package:" + getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }

    private void requestOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }

    @Override
    protected void onActivityResult(int req, int result, Intent data) {
        super.onActivityResult(req, result, data);
        refresh();
    }

    @Override
    public void onRequestPermissionsResult(int req, String[] perms, int[] granted) {
        super.onRequestPermissionsResult(req, perms, granted);
        refresh();
    }

    private Button button(String text, View.OnClickListener onClick) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setGravity(Gravity.CENTER);
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
