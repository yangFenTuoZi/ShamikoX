package yangFenTuoZi.shamikox;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.MenuProvider;

import com.google.android.material.color.DynamicColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;

import yangFenTuoZi.shamikox.databinding.ActivityMainBinding;
import yangFenTuoZi.shamikox.databinding.DialogRequestRootBinding;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private App mApp;
    public MaterialSwitch switchWhitelist;
    public boolean isForeground = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DynamicColors.applyToActivityIfAvailable(this);
        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setupToolbar(binding.toolbar, getString(R.string.app_name), -1);
        mApp = (App) getApplication();

        switchWhitelist = binding.switchWhitelist;
        switchWhitelist.setOnClickListener(v -> {
            new Thread(() -> {
                boolean isChecked = switchWhitelist.isChecked();
                mApp.changeWhitelist(isChecked);
//                if (!result) runOnUiThread(() -> switchWhitelist.setChecked(!isChecked));
            }).start();
        });

        binding.requestRoot.setOnClickListener(v -> {
            DialogRequestRootBinding dialogBinding = DialogRequestRootBinding.inflate(getLayoutInflater());
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.request_root)
                    .setView(dialogBinding.getRoot())
                    .setNegativeButton(R.string.request_root, (dialog, which) -> new Thread(() -> mApp.requestRoot(Integer.parseInt(String.valueOf(dialogBinding.uid.getText())))).start())
                    .show();
        });

        binding.startServer.setOnClickListener(v -> {
            if (mApp.serverIsClosed)
                startServer();
        });
        binding.stopServer.setOnClickListener(v -> {
            if (mApp.serverIsClosed) return;
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.stop_server)
                    .setNegativeButton("Yes", (dialog, which) -> new Thread(() -> mApp.stopServer()).start())
                    .show();
        });
        mApp.main = this;
    }

    public void startServer() {
        AlertDialog alertDialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.start_server)
                .setMessage("Waiting..")
                .setCancelable(false)
                .show();
        new Thread(() -> {
            try {
                String serverFilePath = getApplicationInfo().nativeLibraryDir + "/libserver.so";
                Process process = Runtime.getRuntime().exec("/system/bin/sh");
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
                writer.write("""
                        su
                        if [ ! $? -eq 0 ]; then
                            exit 1
                        fi
                        """);
                writer.flush();
                writer.write(String.format("nohup %s 2>&1 >/dev/null &\nexit\n", serverFilePath));
                writer.flush();
                writer.close();
                alertDialog.setMessage("Exit code: " + process.waitFor());
                alertDialog.setCancelable(true);
            } catch (Throwable e) {
                runOnUiThread(() -> {
                    alertDialog.setMessage(Log.getStackTraceString(e));
                    alertDialog.setCancelable(true);
                });
            }
        }).start();
    }

    @Override
    protected void onStart() {
        super.onStart();
        isForeground = true;
        new Thread(() -> {
            runOnUiThread(() -> {
                switchWhitelist.setChecked(mApp.isWhitelist);
                if (mApp.serverIsClosed) switchWhitelist.setEnabled(false);
            });
        }).start();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    public void setupToolbar(Toolbar toolbar, String title, int menu) {
        toolbar.setTitle(title);
        toolbar.setTooltipText(title);
        if (menu != -1) {
            toolbar.inflateMenu(menu);
            if (this instanceof MenuProvider self) {
                self.onPrepareMenu(toolbar.getMenu());
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }

}
