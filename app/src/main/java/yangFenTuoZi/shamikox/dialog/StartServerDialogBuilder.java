package yangFenTuoZi.shamikox.dialog;

import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;

import yangFenTuoZi.shamikox.App;
import yangFenTuoZi.shamikox.MainActivity;
import yangFenTuoZi.shamikox.R;

public class StartServerDialogBuilder extends BaseDialogBuilder {

    boolean br = false;
    TextView t1;
    TextView t2;
    MainActivity mContext;
    Thread h1, serverListener = new Thread(() -> {
        App mApp = (App) mContext.getApplication();
        while (!br) {
            if (!mApp.serverIsClosed) {
                runOnUiThread(() -> {
                    getAlertDialog().setCancelable(true);
                    getAlertDialog().setTitle(R.string.exec_finish);
                    Toast.makeText(mContext, "Server starts successfully and the window will close after 5 seconds.", Toast.LENGTH_LONG).show();
                });
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ignored) {
                }
                runOnUiThread(getAlertDialog()::cancel);
            }
        }
    });

    public StartServerDialogBuilder(@NonNull MainActivity context) throws DialogShowException {
        super(context);
        mContext = context;
        setView(R.layout.dialog_exec);
        setTitle(mContext.getString(R.string.exec_running));
        setOnDismissListener(dialog -> onDestroy());
    }

    @Override
    public AlertDialog show() {
        AlertDialog alertDialog = super.show();
        getAlertDialog().setCancelable(false);
        t1 = getAlertDialog().findViewById(R.id.exec_title);
        if (t1 != null) {
            t1.setVisibility(View.GONE);
        }
        t2 = getAlertDialog().findViewById(R.id.exec_msg);
        serverListener.start();
        h1 = new Thread(() -> {
            br = false;
            int exitValue;

            try {
                Process p = Runtime.getRuntime().exec("su");
                OutputStream out = p.getOutputStream();
                out.write(("\"" + mContext.getApplicationInfo().nativeLibraryDir + "/libserver.so\" &\nsleep 3s\nexit\n").getBytes());
                out.flush();
                out.close();
                try {
                    BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
                    String inline;
                    while ((inline = br.readLine()) != null && !StartServerDialogBuilder.this.br) {
                        if (inline.contains("服务启动"))
                            ((App) mContext.getApplication()).tryConnectServer();
                        String finalInline = inline;
                        runOnUiThread(() -> t2.append(finalInline + "\n"));
                    }
                    br.close();
                } catch (Exception ignored) {
                }
                p.waitFor();
                exitValue = p.exitValue();
            } catch (Exception e) {
                exitValue = -1;
                runOnUiThread(() -> t2.append(e.getMessage()));
            }
            if (t1 != null) {
                runOnUiThread(() -> t1.setVisibility(View.VISIBLE));
            }
            int finalExitValue = exitValue;
            runOnUiThread(() -> {
                runOnUiThread(() -> t1.append(mContext.getString(R.string.exec_return, finalExitValue, mContext.getString(switch (finalExitValue) {
                    case 0 -> R.string.exec_normal;
                    case 127 -> R.string.exec_command_not_found;
                    case 130 -> R.string.exec_ctrl_c_error;
                    case 139 -> R.string.exec_segmentation_error;
                    default -> R.string.exec_other_error;
                }))));
                getAlertDialog().setTitle(mContext.getString(R.string.exec_finish));
            });
            br = true;
            getAlertDialog().setCancelable(true);
        });
        h1.start();
        return alertDialog;
    }

    public void onDestroy() {
        br = true;
        h1.interrupt();
        serverListener.interrupt();
    }
}