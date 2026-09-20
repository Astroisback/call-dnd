package com.astro.autoreject;

import android.graphics.drawable.Icon;
import android.provider.Settings;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

/**
 * Quick Settings tile: "Voicemail". Single on/off toggle.
 *
 * ON  -> flips voice_call_reject_mode=1 (Game Space call reject) and brings the app
 *        to the foreground so the OS sees a "game" running. All calls get rejected.
 * OFF -> flips voice_call_reject_mode=0. Calls ring normally.
 *
 * The secure/global Game Space flags must be armed once via adb (they persist):
 *   settings put secure oplus_games_not_disturb_switch_key 3
 *   settings put global disturb_for_game_space_mode_flag 3
 *   settings put global disturb_for_game_space_mode 0
 */
public class ModeTileService extends TileService {

    @Override
    public void onStartListening() {
        super.onStartListening();
        render();
    }

    @Override
    public void onClick() {
        super.onClick();

        final boolean turningOn = !Prefs.isForwardingOn(this);

        unlockAndRun(() -> {
            // Flip the Game Space reject flag.
            setGameSpaceReject(turningOn);

            if (turningOn) {
                // Bring the app to the foreground so Game Space sees a "game".
                android.content.Intent launch = new android.content.Intent(this, MainActivity.class);
                launch.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                        | android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(launch);
            }

            Prefs.setForwardingOn(this, turningOn);
            render();
        });
    }

    private void setGameSpaceReject(boolean on) {
        // voice_call_reject_mode is on OEM's protected list — Settings.System.putInt
        // throws "You cannot keep your settings in the secure settings" for normal apps.
        // Use raw ContentResolver insert on the settings URI as a workaround.
        try {
            android.content.ContentValues cv = new android.content.ContentValues(2);
            cv.put("name", "voice_call_reject_mode");
            cv.put("value", on ? "1" : "0");
            getContentResolver().insert(
                    android.net.Uri.parse("content://settings/system"), cv);
            android.util.Log.i("CallDND", "insert voice_call_reject_mode=" + (on ? 1 : 0));
        } catch (Exception e) {
            android.util.Log.e("CallDND", "insert failed", e);
            android.widget.Toast.makeText(this,
                    "Cannot write setting: " + e.getMessage(),
                    android.widget.Toast.LENGTH_LONG).show();
        }
    }

    private void render() {
        Tile tile = getQsTile();
        if (tile == null) {
            return;
        }

        boolean on = Prefs.isForwardingOn(this);
        tile.setState(on ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.setLabel("Voicemail");
        tile.setSubtitle(on ? "Rejecting calls" : "Off");
        tile.setIcon(Icon.createWithResource(this, on
                ? android.R.drawable.ic_lock_silent_mode
                : android.R.drawable.ic_menu_call));
        tile.updateTile();
    }
}
