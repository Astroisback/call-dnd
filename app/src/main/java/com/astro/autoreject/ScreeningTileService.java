package com.astro.autoreject;

import android.graphics.drawable.Icon;
import android.provider.Settings;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

/**
 * "Do Not Disturb (calls)" tile.
 *
 * Taps cycle: Off -> Unknown callers -> All callers -> Reject all -> Off.
 *
 * When active, also flips the Game Space voice_call_reject_mode flag so that
 * if this app is in the foreground (declared as appCategory="game"), the OS
 * will hard-reject incoming calls at the system level.
 */
public class ScreeningTileService extends TileService {

    @Override
    public void onStartListening() {
        super.onStartListening();
        render();
    }

    @Override
    public void onClick() {
        super.onClick();
        // Simple on/off toggle instead of cycling through 4 modes.
        boolean wasActive = Prefs.getMode(this) != Prefs.MODE_OFF;
        int next = wasActive ? Prefs.MODE_OFF : Prefs.MODE_ALL;
        Prefs.setMode(this, next);
        setGameSpaceReject(!wasActive);

        if (!wasActive) {
            // Bring the app to the foreground so Game Space sees a "game" running.
            android.content.Intent launch = new android.content.Intent(this, MainActivity.class);
            launch.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                    | android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivityAndCollapse(launch);
        }

        render();
    }

    private void setGameSpaceReject(boolean on) {
        try {
            boolean ok = Settings.System.putInt(getContentResolver(), "voice_call_reject_mode", on ? 1 : 0);
            android.util.Log.e("CallDND", "putInt voice_call_reject_mode=" + (on ? 1 : 0) + " result=" + ok);
            android.widget.Toast.makeText(this,
                    "voice_call_reject_mode → " + (on ? "1" : "0") + " (ok=" + ok + ")",
                    android.widget.Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            android.util.Log.e("CallDND", "setGameSpaceReject failed", e);
            android.widget.Toast.makeText(this,
                    "FAILED: " + e.getMessage(), android.widget.Toast.LENGTH_LONG).show();
        }
    }

    private void render() {
        Tile tile = getQsTile();
        if (tile == null) {
            return;
        }

        int mode = Prefs.getMode(this);
        boolean active = Prefs.isActive(mode);

        tile.setState(active ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.setLabel("Call DND");
        tile.setSubtitle(Prefs.label(mode));
        tile.setIcon(Icon.createWithResource(this, active
                ? android.R.drawable.ic_lock_silent_mode
                : android.R.drawable.ic_lock_silent_mode_off));
        tile.updateTile();
    }
}
