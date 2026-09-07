package com.astro.autoreject;

import android.graphics.drawable.Icon;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

/**
 * "Do Not Disturb (calls)" tile.
 *
 * Taps cycle: Off -> Unknown callers -> All callers -> Reject all -> Off.
 *
 * The first three SILENCE the call, which is deliberate: the carrier's no-reply rule
 * still times out and hands the caller to voicemail. "Reject all" disconnects instantly,
 * which only reaches voicemail if the busy rule is armed and the network treats the
 * reject as "user busy".
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
        Prefs.setMode(this, Prefs.nextMode(this));
        render();
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
