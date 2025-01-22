package yangFenTuoZi.shamikox.service;

import android.os.RemoteException;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;

import java.util.concurrent.atomic.AtomicInteger;

import yangFenTuoZi.shamikox.App;

public class SwitchWhitelist extends TileService {
    private App mApp;

    private boolean waitForIsWhitelist() {
        if (!mApp.isServerRunning) return false;
        try {
            return mApp.iService.isWhitelist();
        } catch (RemoteException e) {
            Log.e(getClass().getSimpleName(), Log.getStackTraceString(e));
            return false;
        }
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
            if (!mApp.isServerRunning) return;
            try {
                getQsTile().setState(mApp.iService.change(state == Tile.STATE_INACTIVE) ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
            } catch (RemoteException e) {
                Log.e(getClass().getSimpleName(), Log.getStackTraceString(e));
            }
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
