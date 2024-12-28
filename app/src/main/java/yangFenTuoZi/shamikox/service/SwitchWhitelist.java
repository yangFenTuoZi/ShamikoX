package yangFenTuoZi.shamikox.service;

import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import java.util.concurrent.atomic.AtomicInteger;

import yangFenTuoZi.shamikox.App;

public class SwitchWhitelist extends TileService {
    private App mApp;

    private boolean waitForIsWhitelist() {
        AtomicInteger result_ = new AtomicInteger(-1);
        new Thread(() -> {
            result_.set(mApp.isWhitelist ? 1 : 0);
        }).start();
        while (result_.get() == -1) ;
        return result_.get() == 1;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mApp = (App) getApplication();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onTileAdded() {
        super.onTileAdded();
        boolean whitelist = waitForIsWhitelist();
        getQsTile().setState(whitelist ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        getQsTile().updateTile();
    }

    @Override
    public void onTileRemoved() {
        super.onTileRemoved();
    }

    @Override
    public void onClick() {
        var state = getQsTile().getState();
        if (state == Tile.STATE_UNAVAILABLE) return;

        new Thread(() -> {
            getQsTile().setState(mApp.changeWhitelist(state == Tile.STATE_INACTIVE) ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
            getQsTile().updateTile();
        }).start();
        super.onClick();
    }

    @Override
    public void onStartListening() {
        boolean whitelist = waitForIsWhitelist();
        getQsTile().setState(whitelist ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        getQsTile().updateTile();
        super.onStartListening();
    }

    @Override
    public void onStopListening() {
        super.onStopListening();
    }

}
