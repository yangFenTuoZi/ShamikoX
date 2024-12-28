package yangFenTuoZi.shamikox.service;

import android.service.quicksettings.TileService;

import yangFenTuoZi.shamikox.App;

public class RequestRoot extends TileService {

    @Override
    public void onTileAdded() {
        super.onTileAdded();
    }

    @Override
    public void onTileRemoved() {
        super.onTileRemoved();
    }

    @Override
    public void onClick() {
        super.onClick();
        new Thread(() -> ((App) getApplication()).requestRoot(-5)).start();
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
    }

    @Override
    public void onStopListening() {
        super.onStopListening();
    }
}
