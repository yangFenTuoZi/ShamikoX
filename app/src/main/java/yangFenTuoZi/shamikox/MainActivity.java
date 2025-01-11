package yangFenTuoZi.shamikox;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.MenuProvider;

import com.google.android.material.color.DynamicColors;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.mai.packageviewer.activity.PackageViewerActivity;
import com.mai.packageviewer.setting.MainSettings;

import yangFenTuoZi.shamikox.databinding.ActivityMainBinding;
import yangFenTuoZi.shamikox.databinding.DialogRequestRootBinding;
import yangFenTuoZi.shamikox.dialog.BaseDialogBuilder;
import yangFenTuoZi.shamikox.dialog.StartServerDialogBuilder;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private App mApp;
    public MaterialSwitch switchWhitelist;
    public boolean isForeground = false;
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
        DynamicColors.applyToActivityIfAvailable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setupToolbar(binding.toolbar, getString(R.string.app_name), R.menu.menu_main);
        mApp = (App) getApplication();

        switchWhitelist = binding.switchWhitelist;
        switchWhitelist.setOnClickListener(v -> new Thread(() -> {
            boolean isChecked = switchWhitelist.isChecked();
            mApp.changeWhitelist(isChecked);
        }).start());

        binding.toolbar.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.start_server) {
                if (mApp.serverIsClosed) {
                    if (MainSettings.INSTANCE.getBool("do_not_start_server_in_app", true)) {
                        try {
                            new BaseDialogBuilder(this)
                                    .setTitle(R.string.start_server)
                                    .setMessage(R.string.server_magisk_runner_magisk_server)
                                    .setPositiveButton(R.string.allow_once, (dialog, which) -> {
                                        dialog.dismiss();
                                        isDialogShow = false;
                                        try {
                                            new StartServerDialogBuilder(this).show();
                                        } catch (BaseDialogBuilder.DialogShowException ignored) {
                                        }
                                    })
                                    .setNegativeButton(R.string.cancel, null)
                                    .setNeutralButton(R.string.no_more_prompts, (dialogInterface, i) -> {
                                        dialogInterface.dismiss();
                                        isDialogShow = false;
                                        MainSettings.INSTANCE.setBool("do_not_start_server_in_app", false);
                                        try {
                                            new StartServerDialogBuilder(this).show();
                                        } catch (BaseDialogBuilder.DialogShowException ignored) {
                                        }
                                    })
                                    .show();
                        } catch (BaseDialogBuilder.DialogShowException ignored) {
                        }
                    } else {
                        try {
                            new StartServerDialogBuilder(this).show();
                        } catch (BaseDialogBuilder.DialogShowException ignored) {
                        }
                    }
                }
            } else if (itemId == R.id.stop_server) {
                if (!mApp.serverIsClosed) {
                    try {
                        new BaseDialogBuilder(this)
                                .setTitle(R.string.stop_server)
                                .setNegativeButton("Yes", (dialog, which) -> new Thread(() -> mApp.stopServer()).start())
                                .show();
                    } catch (BaseDialogBuilder.DialogShowException ignored) {
                    }
                }
            }
            return false;
        });

        binding.requestRoot.setOnClickListener(v -> {
            dialogBinding = DialogRequestRootBinding.inflate(getLayoutInflater());
            dialogBinding.chooseApp.setOnClickListener(v1 -> launcherActivity.launch(new Intent(this, PackageViewerActivity.class).putExtra("choose", true)));
            try {
                new BaseDialogBuilder(this)
                        .setTitle(R.string.request_root)
                        .setView(dialogBinding.getRoot())
                        .setNegativeButton(R.string.request_root, (dialog, which) -> new Thread(() -> mApp.requestRoot(Integer.parseInt(String.valueOf(dialogBinding.uid.getText())))).start())
                        .setOnDismissListener(dialog -> dialogBinding = null)
                        .show();
            } catch (BaseDialogBuilder.DialogShowException ignored) {
            }
        });

        mApp.main = this;

    }

    @Override
    protected void onStart() {
        super.onStart();
        isForeground = true;
        new Thread(() -> runOnUiThread(() -> {
            switchWhitelist.setChecked(mApp.isWhitelist);
            if (mApp.serverIsClosed) switchWhitelist.setEnabled(false);
        })).start();
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
