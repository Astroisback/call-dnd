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
        int next = Prefs.nextMode(this);
        Prefs.setMode(this, next);
        // Flip the Game Space reject flag so the OS rejects calls while our app is fg.
        setGameSpaceReject(Prefs.isActive(next));
        render();
    }

    private void setGameSpaceReject(boolean on) {
        try {
            Settings.System.putInt(getContentResolver(), "voice_call_reject_mode", on ? 1 : 0);
            Settings.Secure.putInt(getContentResolver(), "oplus_games_not_disturb_switch_key", on ? 3 : 0);
            Settings.Global.putInt(getContentResolver(), "disturb_for_game_space_mode_flag", on ? 3 : 0);
            Settings.Global.putInt(getContentResolver(), "disturb_for_game_space_mode", 0);
        } catch (Exception ignored) {
            // WRITE_SETTINGS or WRITE_SECURE_SETTINGS may not be granted.
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
