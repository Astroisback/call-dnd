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
            // Only voice_call_reject_mode lives in Settings.System and is writable
            // with WRITE_SETTINGS. The other two flags (oplus_games_not_disturb_switch_key
            // and disturb_for_game_space_mode_flag) are in secure/global and need
            // WRITE_SECURE_SETTINGS — set them once via adb, they persist across reboots.
            Settings.System.putInt(getContentResolver(), "voice_call_reject_mode", on ? 1 : 0);
        } catch (Exception ignored) {
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
