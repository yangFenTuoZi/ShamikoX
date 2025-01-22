package yangFenTuoZi.shamikox;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.color.DynamicColors;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Objects;

import yangFenTuoZi.shamikox.databinding.ActivityMainBinding;
import yangFenTuoZi.shamikox.databinding.DialogRequestRootBinding;
import yangFenTuoZi.shamikox.server.Server;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    public App mApp;
    public boolean isDialogShow = false;

    private DialogRequestRootBinding dialogBinding;
    private final ActivityResultLauncher<Intent> launcherActivity = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == RESULT_OK) {
            if (result.getData() != null && dialogBinding != null) {
                dialogBinding.uid.setText(String.valueOf(result.getData().getIntExtra("uid", -1)));
            }
        }
    });


    @SuppressLint("NonConstantResourceId")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mApp = (App) getApplication();

        DynamicColors.applyToActivityIfAvailable(this);
        EdgeToEdge.enable(this, SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT), SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT));

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Fragment fragment = Objects.requireNonNull(getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_activity_main));
        NavController navController = ((NavHostFragment) fragment).getNavController();
        NavigationUI.setupWithNavController(binding.navView, navController);

//        binding.requestRoot.setOnClickListener(v -> {
//            dialogBinding = DialogRequestRootBinding.inflate(getLayoutInflater());
//            dialogBinding.chooseApp.setOnClickListener(v1 -> launcherActivity.launch(new Intent(this, PackageViewerActivity.class).putExtra("choose", true)));
//            try {
//                new BaseDialogBuilder(this)
//                        .setTitle(R.string.request_root)
//                        .setView(dialogBinding.getRoot())
//                        .setNegativeButton(R.string.request_root, (dialog, which) -> new Thread(() -> mApp.requestRoot(Integer.parseInt(String.valueOf(dialogBinding.uid.getText())))).start())
//                        .setOnDismissListener(dialog -> dialogBinding = null)
//                        .show();
//            } catch (BaseDialogBuilder.DialogShowException ignored) {
//            }
//        });

        mApp.main = this;

        writeStarter();
    }

    private void writeStarter() {
        // server_starter
        try {
            String starter_content = String.format("""
                    #!/system/bin/sh
                    log() {
                        echo "[$(date "%s")] [server_starter] [$1] $2"
                    }
                    log I "Begin"
                    
                    # check uid
                    uid=$(id -u)
                    if [ ! $uid -eq 0 ] && [ ! $uid -eq 2000 ]; then
                        log E "Insufficient permission! Need to be launched by root or shell, but your uid is $uid." >&2
                        exit 255
                    fi
                    
                    # start server
                    log I "Start server"
                    pkill -f %3$s 2>&1 >/dev/null
                    app_process -Djava.class.path="$(pm path %2$s | sed 's/package://')" /system/bin --nice-name=%3$s %4$s
                    exitValue=$?
                    while [ $exitValue -eq 10 ]; do
                        pkill -f %3$s 2>&1 >/dev/null
                        app_process -Djava.class.path="$(pm path %2$s | sed 's/package://')" /system/bin --nice-name=%3$s %4$s restart
                        exitValue=$?
                    done
                    exit $exitValue
                    """, "+%H:%M:%S", BuildConfig.APPLICATION_ID, Server.TAG, Server.class.getName());
            File starter = new File(getExternalFilesDir(""), "server_starter.sh");
            if (starter.exists()) {
                starter.delete();
                starter.createNewFile();
            }
            FileWriter fileWriter = new FileWriter(starter);
            fileWriter.write(starter_content);
            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e) {
            Log.e(getClass().getSimpleName(), Log.getStackTraceString(e));
        }
    }

}
