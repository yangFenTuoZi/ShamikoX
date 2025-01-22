package yangFenTuoZi.shamikox.fragment;

import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.mai.packageviewer.setting.MainSettings;

import yangFenTuoZi.shamikox.R;
import yangFenTuoZi.shamikox.databinding.FragmentHomeBinding;
import yangFenTuoZi.shamikox.dialog.BaseDialogBuilder;
import yangFenTuoZi.shamikox.dialog.StartServerDialogBuilder;
import yangFenTuoZi.shamikox.receiver.OnServerChangeListener;
import yangFenTuoZi.shamikox.server.IService;

public class HomeFragment extends BaseFragment {
    public MaterialSwitch switchWhitelist;
    public boolean isForeground = false;
    private FragmentHomeBinding binding;

    private OnServerChangeListener listener = new OnServerChangeListener() {
        @Override
        public void serverRun(IService iService) {
            if (isForeground) {
                runOnUiThread(() -> {
                    try {
                        switchWhitelist.setEnabled(true);
                        switchWhitelist.setChecked(iService.isWhitelist());
                    } catch (RemoteException e) {
                        Log.e(getClass().getSimpleName(), Log.getStackTraceString(e));
                    }
                });
            }
        }

        @Override
        public void serverStop() {
            if (isForeground)
                runOnUiThread(() -> switchWhitelist.setEnabled(false));
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        setupToolbar(binding.toolbar, null, R.string.app_name, R.menu.menu_main);
        mApp.addOnServiceConnectListener(listener);

        switchWhitelist = binding.switchWhitelist;
        switchWhitelist.setOnClickListener(v -> new Thread(() -> {
            if (!mApp.isServerRunning) {
                switchWhitelist.setChecked(false);
                switchWhitelist.setEnabled(false);
                return;
            }
            boolean isChecked = switchWhitelist.isChecked();
            try {
                mApp.iService.change(isChecked);
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        }).start());

        binding.toolbar.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.start_server) {
                if (MainSettings.Companion.getINSTANCE().getBool("do_not_start_server_in_app", true)) {
                    try {
                        new BaseDialogBuilder(mContext)
                                .setTitle(R.string.start_server)
                                .setMessage(R.string.server_magisk_runner_magisk_server)
                                .setPositiveButton(R.string.allow_once, (dialog, which) -> {
                                    dialog.dismiss();
                                    mContext.isDialogShow = false;
                                    try {
                                        new StartServerDialogBuilder(mContext).show();
                                    } catch (BaseDialogBuilder.DialogShowException ignored) {
                                    }
                                })
                                .setNegativeButton(R.string.cancel, null)
                                .setNeutralButton(R.string.no_more_prompts, (dialogInterface, i) -> {
                                    dialogInterface.dismiss();
                                    mContext.isDialogShow = false;
                                    MainSettings.Companion.getINSTANCE().setBool("do_not_start_server_in_app", false);
                                    try {
                                        new StartServerDialogBuilder(mContext).show();
                                    } catch (BaseDialogBuilder.DialogShowException ignored) {
                                    }
                                })
                                .show();
                    } catch (BaseDialogBuilder.DialogShowException ignored) {
                    }
                } else {
                    try {
                        new StartServerDialogBuilder(mContext).show();
                    } catch (BaseDialogBuilder.DialogShowException ignored) {
                    }
                }
            } else if (itemId == R.id.stop_server) {
                if (mApp.isServerRunning) {
                    try {
                        new BaseDialogBuilder(mContext)
                                .setTitle(R.string.stop_server)
                                .setNegativeButton("Yes", (dialog, which) -> new Thread(() -> {
                                    try {
                                        mApp.iService.stopServer();
                                    } catch (RemoteException e) {
                                        Log.e(getClass().getSimpleName(), Log.getStackTraceString(e));
                                    }
                                }).start())
                                .show();
                    } catch (BaseDialogBuilder.DialogShowException ignored) {
                    }
                }
            }
            return false;
        });
        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();
        isForeground = true;
        if (mApp.isServerRunning) listener.serverRun(mApp.iService);
        else listener.serverStop();
    }

    @Override
    public void onStop() {
        super.onStop();
        isForeground = false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        binding = null;
        mApp.removeOnServiceConnectListener(listener);
    }
}
