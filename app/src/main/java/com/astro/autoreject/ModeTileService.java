package com.astro.autoreject;

import android.graphics.drawable.Icon;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

/**
 * Quick Settings tile: "Voicemail". Sits next to the DND tile.
 *
 * ON  -> unconditional forwarding, so the network answers before the phone rings and
 *        the caller hears the greeting and can leave a message.
 * OFF -> cancels unconditional forwarding, restoring the normal 30s no-reply rule.
 *
 * A screening-service reject is deliberately NOT used: it disconnects instantly, so the
 * carrier's 30s no-reply timer never elapses and the caller never reaches voicemail.
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
        final String number = Prefs.getVoicemailNumber(this);

        // Dialling an MMI code starts an activity, which needs the shade collapsed.
        unlockAndRun(() -> {
            if (turningOn) {
                Forwarding.enableAll(this, number);
            } else {
                Forwarding.disableAll(this);
            }
            // Optimistic; the network shows its own confirmation dialog.
            Prefs.setForwardingOn(this, turningOn);
            render();
        });
    }

    private void render() {
        Tile tile = getQsTile();
        if (tile == null) {
            return;
        }

        boolean on = Prefs.isForwardingOn(this);
        tile.setState(on ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.setLabel("Voicemail");
        tile.setSubtitle(on ? "All calls" : "Off");
        tile.setIcon(Icon.createWithResource(this, on
                ? android.R.drawable.ic_lock_silent_mode
                : android.R.drawable.ic_menu_call));
        tile.updateTile();
    }
}
