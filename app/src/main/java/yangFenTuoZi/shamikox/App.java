package yangFenTuoZi.shamikox;

import android.app.Application;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.material.color.DynamicColors;
import com.mai.packageviewer.setting.MainSettings;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import yangFenTuoZi.shamikox.receiver.OnServerChangeListener;
import yangFenTuoZi.shamikox.server.IService;
import yangFenTuoZi.shamikox.server.Server;

public class App extends Application {
    public IService iService;
    public boolean isServerRunning = false;
    public MainActivity main;
    public File sendBinderFile;
    private final List<OnServerChangeListener> mListeners = new LinkedList<>();
    private Timer timer;
    private Handler mHandler;
    private Thread mUiThread;

    public void onReceive(IBinder binder) {
        iService = binder == null ? null : IService.Stub.asInterface(binder);

        boolean server = pingServer();

        if (timer != null)
            timer.cancel();
        if (server) {
            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (!pingServer()) {
                        onReceive(null);
                    }
                }
            }, 0L, 500L);
            if (sendBinderFile.exists())
                sendBinderFile.delete();
            for (OnServerChangeListener listener : mListeners)
                new Thread(() -> listener.serverRun(iService)).start();

        } else {
            if (!sendBinderFile.exists()) {
                try {
                    sendBinderFile.createNewFile();
                } catch (IOException e) {
                    Log.e(getClass().getSimpleName(), Log.getStackTraceString(e));
                }
            }
            for (OnServerChangeListener listener : mListeners)
                new Thread(listener::serverStop).start();
        }
    }

    public void addOnServiceConnectListener(@NonNull OnServerChangeListener listener) {
        mListeners.add(listener);
    }

    public void removeOnServiceConnectListener(@NonNull OnServerChangeListener listener) {
        mListeners.remove(listener);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mHandler = new Handler();
        mUiThread = Thread.currentThread();
        sendBinderFile = new File(getCacheDir(), Server.SEND_BINDER_FILE);
        DynamicColors.applyToActivitiesIfAvailable(this);
        MainSettings.Companion.setINSTANCE(new MainSettings(this));
        onReceive(null);
    }

    public boolean pingServer() {
        try {
            return isServerRunning = (iService != null && iService.asBinder().pingBinder() && iService.asBinder().isBinderAlive() && iService.testConnect() == 114514);
        } catch (RemoteException e) {
            return isServerRunning = false;
        }
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        if (timer != null)
            timer.cancel();
        if (sendBinderFile.exists())
            sendBinderFile.delete();
    }

    public final void runOnUiThread(Runnable action) {
        if (Thread.currentThread() != mUiThread) {
            mHandler.post(action);
        } else {
            action.run();
        }
    }
}
