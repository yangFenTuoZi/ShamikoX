package yangFenTuoZi.shamikox.server;

import static yangFenTuoZi.shamikox.server.Logger.getStackTraceString;
import static yangFenTuoZi.shamikox.server.magisk.SuPolicy.POLICY_ALLOW;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.IActivityManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.ddm.DdmHandleAppName;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.system.Os;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.lang.reflect.Method;
import java.util.List;

import yangFenTuoZi.shamikox.BuildConfig;
import yangFenTuoZi.shamikox.MainActivity;
import yangFenTuoZi.shamikox.server.fakecontext.FakeContext;
import yangFenTuoZi.shamikox.server.fakecontext.Workarounds;
import yangFenTuoZi.shamikox.server.magisk.MagiskDB;
import yangFenTuoZi.shamikox.server.magisk.SuPolicy;

public class Server {
    public static final String TAG = "shamikox_server";
    public static final String ACTION_SERVER_RUNNING = "shamikox.intent.action.SERVER_RUNNING";
    public static final String ACTION_SERVER_STOPPED = "shamikox.intent.action.SERVER_STOPPED";
    public static final String SEND_BINDER_FILE = "send_binder";
    Handler mHandler;
    MagiskDB magiskDB;
    PackageManager packageManager;
    IPackageManager iPackageManager;
    IActivityManager iActivityManager;
    Logger Log;
    boolean isStop = false;
    String appPath;
    ContextWrapper mContext;
    Context appContext;
    File whiteListFile = new File("/data/adb/shamiko/whitelist");
    IBinder binder;

    public static void main(String[] args) {
        DdmHandleAppName.setAppName(TAG, 0);
        int uid = Os.getuid();
        if (uid != 0) {
            System.err.printf("Insufficient permission! Need to be launched by root, but your uid is %d.\n", uid);
            System.exit(255);
        }

        File shamiko = new File("/data/adb/modules/zygisk_shamiko");
        if (!shamiko.exists() || shamiko.isFile()) {
            System.err.print("Do not install 'Shamiko' module.\n");
            System.exit(255);
        }

        File shamikoEnable = new File("/data/adb/shamiko");
        if (!shamikoEnable.exists() || shamikoEnable.isFile()) {
            System.err.printf("'%s' not found, try to reboot your device.\n", shamikoEnable.getPath());
            System.exit(255);
        }

        new Server(args);
    }

    @SuppressLint("WrongConstant")
    private Server(String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (!isStop)
                onStop();
        }));

        mContext = new FakeContext();
        try {
            appContext = mContext.createPackageContext(BuildConfig.APPLICATION_ID, Context.CONTEXT_IGNORE_SECURITY);
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }

        try {
            onStart(args);
        } catch (Throwable e) {
            Log.e(getStackTraceString(e));
        }
    }

    public void onStart(String[] args) {

        Log = new Logger(TAG, new File(appContext.getCacheDir(), "logs"));

        if (args.length != 0 && args[0].equals("restart")) {
            Log.i("Server Restart.");
        } else {
            Log.i("Server Start.");
        }

        packageManager = mContext.getPackageManager();
        iPackageManager = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
        iActivityManager = IActivityManager.Stub.asInterface(ServiceManager.getService("activity"));

        try {
            appPath = getApplicationInfo(BuildConfig.APPLICATION_ID).sourceDir;
        } catch (Throwable e) {
            Log.e("Unable to get app path!\n" + getStackTraceString(e));
            exit(1);
        }

        magiskDB = new MagiskDB();

        new Thread(() -> {
            File file = new File(appPath);
            long lastModified = file.lastModified();
            final Object lock = new Object();
            synchronized (lock) {
                while (!isStop) {
                    if (file.lastModified() != lastModified || !file.exists()) {
                        try {
                            appPath = getApplicationInfo(BuildConfig.APPLICATION_ID).sourceDir;
                            if (appPath == null || appPath.isEmpty()) {
                                Log.w("App is uninstalled!");
                                exit(1);
                            } else {
                                exit(10);
                            }
                        } catch (Exception e) {
                            Log.w("App is uninstalled!");
                            exit(1);
                        }
                        break;
                    }
                    try {
                        lock.wait(1000);
                    } catch (InterruptedException e) {
                        Log.e(getStackTraceString(e));
                    }
                }
            }
        }).start();

        new Thread(() -> {
            final Object lock = new Object();
            synchronized (lock) {
                while (!isStop) {
                    System.gc();
                    try {
                        lock.wait(1000 * 60);
                    } catch (InterruptedException e) {
                        Log.e(getStackTraceString(e));
                    }
                }
            }
        }).start();

        new Thread(() -> {
            File file = new File(appContext.getCacheDir(), SEND_BINDER_FILE);
            final Object lock = new Object();
            synchronized (lock) {
                while (!isStop) {
                    if (file.exists()) {
                        file.delete();
                        if (isAppRunning(BuildConfig.APPLICATION_ID))
                            mHandler.post(this::sendBinder);
                    }
                    try {
                        lock.wait(1000);
                    } catch (InterruptedException e) {
                        Log.e(getStackTraceString(e));
                    }
                }
            }
        }).start();

        if (Looper.getMainLooper() == null) {
            Looper.prepareMainLooper();
        }
        mHandler = new Handler();
        Looper.loop();
    }

    private void collapsePanels() {
        try {
            Class<?> statusBarManagerClass = Class.forName("android.app.StatusBarManager");
            Object statusBarManager = mContext.getSystemService(statusBarManagerClass);
            Method collapsePanelsMethod = statusBarManagerClass.getMethod("collapsePanels");
            collapsePanelsMethod.invoke(statusBarManager);
        } catch (Exception e) {
            Log.e("collapsePanels error: %s", getStackTraceString(e));
        }
    }

    private ApplicationInfo getApplicationInfo(String packageName) throws RemoteException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return iPackageManager.getApplicationInfo(packageName, 0L, 0);
        } else {
            return iPackageManager.getApplicationInfo(packageName, 0, 0);
        }
    }

    public void onStop() {
        isStop = true;

        magiskDB.close();
        mContext.sendBroadcast(new Intent(Server.ACTION_SERVER_STOPPED)
                .setPackage(BuildConfig.APPLICATION_ID)
                .addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES));

        Log.i("Server Stop.\n");
        Log.close();
    }

    public void exit(int status) {
        onStop();
        System.exit(status);
    }

    private void sendBinder() {
        if (binder == null || !binder.isBinderAlive() || !binder.pingBinder())
            binder = createBinder();

        try {
            Bundle data = new Bundle();
            data.putBinder("binder", binder);

            @SuppressLint("WrongConstant")
            Intent intent = new Intent(Server.ACTION_SERVER_RUNNING)
                    .setPackage(BuildConfig.APPLICATION_ID)
                    .addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                    .putExtra("data", data);

            if (iActivityManager == null || !iActivityManager.asBinder().pingBinder())
                iActivityManager = IActivityManager.Stub.asInterface(ServiceManager.getService("activity"));
            iActivityManager.broadcastIntent(null, intent, null, null, 0, null, null,
                    null, -1, null, true, false, 0);
        } catch (Throwable e) {
            Log.e("sendBinder error: %s", getStackTraceString(e));
        }
    }

    private IBinder createBinder() {
        //生成binder
        return new IService.Stub() {
            @Override
            public void stopServer() {
                isStop = true;
                exit(0);
            }

            @Override
            public int testConnect() {
                return 114514;
            }

            @Override
            public String getVersionName() {
                return "1.0";
            }

            @Override
            public int getVersionCode() {
                return 1;
            }

            @Override
            public void delete(SuPolicy suPolicy) {
                magiskDB.delete(suPolicy);
            }

            @Override
            public void update(SuPolicy suPolicy) {
                magiskDB.update(suPolicy);
            }

            @Override
            public void insert(SuPolicy suPolicy) {
                magiskDB.insert(suPolicy);
            }

            @Override
            public SuPolicy queryById(int uid) {
                return magiskDB.query(uid);
            }

            @Override
            public SuPolicy[] queryAll() {
                return magiskDB.query();
            }

            @Override
            public boolean exist(int uid) {
                return magiskDB.exist(uid);
            }

            @Override
            public void insertOrUpdate(SuPolicy suPolicy) {
                magiskDB.insertOrUpdate(suPolicy);
            }

            @Override
            public boolean isWhitelist() {
                return whiteListFile.exists();
            }

            @Override
            public boolean change(boolean whitelist) throws RemoteException {
                try {
                    if (whitelist) whiteListFile.createNewFile();
                    else whiteListFile.delete();
                } catch (Throwable e) {
                    Log.e("IService_change(boolean): %s", getStackTraceString(e));
                    throw new RemoteException(getStackTraceString(e));
                }
                return whiteListFile.exists();
            }

            @Override
            public void authorize(int uid) {
                if (magiskDB.exist(uid)) {
                    SuPolicy suPolicy = queryById(uid);
                    if (suPolicy.policy != POLICY_ALLOW) {
                        suPolicy.policy = POLICY_ALLOW;
                        magiskDB.update(suPolicy);
                    }
                } else {
                    SuPolicy suPolicy = new SuPolicy();
                    suPolicy.uid = uid;
                    suPolicy.policy = POLICY_ALLOW;
                    magiskDB.insert(suPolicy);
                }
            }

            @Override
            public void authorizeForegroundApp() {
                collapsePanels();
                String packageName = getTopFullscreenOpaqueWindowPackageName();
                int uid = -1;
                try {
                    uid = getApplicationInfo(packageName).uid;
                } catch (RemoteException e) {
                    Log.e("getApplicationInfo error: %s", getStackTraceString(e));
                }
                if (uid == -1) return;
                authorize(uid);
                try {
                    String appName = packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString();
                    String msgText = appName + " has been authorized";
                    SpannableString msg = new SpannableString(msgText);
                    msg.setSpan(new StyleSpan(Typeface.BOLD), 0, appName.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                    mHandler.post(() -> Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show());
                } catch (PackageManager.NameNotFoundException e) {
                    Log.e(getStackTraceString(e));
                }
            }
        };
    }

    public String getTopFullscreenOpaqueWindowPackageName() {
        try {
            ActivityManager activityManager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningAppProcessInfo> tasks = activityManager.getRunningAppProcesses();
            if (tasks != null && !tasks.isEmpty()) {
                ActivityManager.RunningAppProcessInfo appTask = tasks.get(0);
                return appTask.pkgList[0];
            }
        } catch (Exception e) {
            Log.e("getTopFullscreenOpaqueWindowPackageName error: %s", getStackTraceString(e));
        }
        return null;
    }

    public boolean isAppRunning(String packageName) {
        ActivityManager activityManager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> processes = activityManager.getRunningAppProcesses();
        for (ActivityManager.RunningAppProcessInfo processInfo : processes) {
            if (processInfo.processName.equals(packageName)) {
                return true;
            }
        }
        return false;
    }
}
