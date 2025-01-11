package yangFenTuoZi.shamikox;

import android.app.Application;

import com.google.android.material.color.DynamicColors;
import com.mai.packageviewer.GlobalContext;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

public class App extends Application {
    private Socket server;
    private BufferedWriter output;
    private BufferedReader input;
    public boolean serverIsClosed = true;
    public boolean isWhitelist = false;
    public MainActivity main;
    private Thread thread;

    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
        }
    }

    public boolean tryConnectServer() {
        try {
            server = new Socket("localhost", 11451);
            server.setKeepAlive(true);
            output = new BufferedWriter(new OutputStreamWriter(server.getOutputStream()));
            input = new BufferedReader(new InputStreamReader(server.getInputStream()));
            return serverIsClosed = false;
        } catch (IOException ignored) {
            return serverIsClosed = true;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        GlobalContext.context = this;
        DynamicColors.applyToActivitiesIfAvailable(this);
        thread = new Thread(() -> {
            while (!thread.isInterrupted()) {
                if (serverIsClosed) {
                    tryConnectServer();
                }
                if (!serverIsClosed) {
                    try {
                        output.write("-4\n");
                        output.flush();
                        String result = input.readLine();
                        if (result == null) isWhitelist = false;
                        else isWhitelist = result.equals("1");
                    } catch (Exception e) {
                        serverIsClosed = true;
                        isWhitelist = false;
                    }
                }

                if (main != null && main.isForeground) {
                    if (serverIsClosed) {
                        main.runOnUiThread(() -> {
                            main.switchWhitelist.setChecked(false);
                            main.switchWhitelist.setEnabled(false);
                        });
                    } else {
                        main.runOnUiThread(() -> {
                            main.switchWhitelist.setEnabled(true);
                            main.switchWhitelist.setChecked(isWhitelist);
                        });
                    }
                }

                sleep(1000);
            }
        });
        thread.start();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        thread.interrupt();
        if (!serverIsClosed) {
            try {
                server.close();
                output.close();
                input.close();
            } catch (Exception ignored) {
                serverIsClosed = true;
                isWhitelist = false;
            }
        }
    }

    public boolean changeWhitelist(boolean whitelist) {
        try {
            if (serverIsClosed)
                if (!tryConnectServer()) throw new IOException();
            output.write((whitelist ? -2 : -3) + "\n");
            output.flush();
            String string = input.readLine();
            isWhitelist = string.equals("1");
        } catch (Exception e) {
            serverIsClosed = true;
            isWhitelist = false;
        }
        if (main != null && main.isForeground) {
            new Thread(() -> main.runOnUiThread(() -> main.switchWhitelist.setChecked(isWhitelist))).start();
        }
        return isWhitelist;
    }

    public void requestRoot(int uid) {
        try {
            if (serverIsClosed)
                if (!tryConnectServer()) throw new IOException();
            output.write(uid + "\n");
            output.flush();
        } catch (Exception e) {
            serverIsClosed = true;
            isWhitelist = false;
        }
    }

    public void stopServer() {
        try {
            if (serverIsClosed)
                if (!tryConnectServer()) throw new IOException();
            output.write("-1\n");
            output.flush();
        } catch (Exception ignored) {
        }
        isWhitelist = false;
        serverIsClosed = true;
    }
}
