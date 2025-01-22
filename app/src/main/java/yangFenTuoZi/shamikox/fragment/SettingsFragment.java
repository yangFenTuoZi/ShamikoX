package yangFenTuoZi.shamikox.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import yangFenTuoZi.shamikox.R;
import yangFenTuoZi.shamikox.databinding.FragmentSettingsBinding;
import yangFenTuoZi.shamikox.databinding.FragmentSuperuserBinding;

public class SettingsFragment extends BaseFragment {
    public boolean isForeground = false;
    private FragmentSettingsBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        setupToolbar(binding.toolbar, null, R.string.settings);

        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();
        isForeground = true;
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
    }
}
