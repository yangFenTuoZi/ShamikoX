package yangFenTuoZi.shamikox.service;

import android.service.quicksettings.TileService;
import android.util.Log;

import yangFenTuoZi.shamikox.App;

public class AuthorizeRoot extends TileService {
    private App mApp;

    @Override
    public void onCreate() {
        super.onCreate();
        mApp = (App) getApplication();
    }

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
        new Thread(() -> {
            if (!mApp.isServerRunning) return;
            try {
                mApp.iService.authorizeForegroundApp();
            } catch (Exception e) {
                Log.e(getClass().getSimpleName(), Log.getStackTraceString(e));
            }
        }).start();
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
