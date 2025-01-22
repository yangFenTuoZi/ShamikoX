package yangFenTuoZi.shamikox.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import yangFenTuoZi.shamikox.App;
import yangFenTuoZi.shamikox.server.Server;

public class ServerReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("ServerReceiver", "onReceive: " + intent.getAction());
        if (Server.ACTION_SERVER_RUNNING.equals(intent.getAction())) {
            Bundle data = intent.getBundleExtra("data");
            IBinder binder = data.getBinder("binder");
            if (!binder.pingBinder()) return;
            ((App) context.getApplicationContext()).onReceive(binder);
        } else if (Server.ACTION_SERVER_STOPPED.equals(intent.getAction())) {
            ((App) context.getApplicationContext()).onReceive(null);
        }
    }
}
