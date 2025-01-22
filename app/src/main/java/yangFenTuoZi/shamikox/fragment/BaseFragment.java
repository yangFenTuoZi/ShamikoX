package yangFenTuoZi.shamikox.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;

import yangFenTuoZi.shamikox.App;
import yangFenTuoZi.shamikox.MainActivity;

public class BaseFragment extends Fragment {
    public MainActivity mContext;
    public App mApp;

    public void setupToolbar(Toolbar toolbar, View tipsView, int title) {
        setupToolbar(toolbar, tipsView, getString(title), -1);
    }

    public void setupToolbar(Toolbar toolbar, View tipsView, int title, int menu) {
        setupToolbar(toolbar, tipsView, getString(title), menu);
    }

    public void setupToolbar(Toolbar toolbar, View tipsView, String title, int menu) {
        toolbar.setTitle(title);
        toolbar.setTooltipText(title);
        if (tipsView != null) tipsView.setTooltipText(title);
        if (menu != -1) {
            toolbar.inflateMenu(menu);
            if (this instanceof MenuProvider self) {
                self.onPrepareMenu(toolbar.getMenu());
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = (MainActivity) requireActivity();
        mApp = mContext.mApp;
    }

    public void runOnUiThread(Runnable action) {
        mContext.runOnUiThread(action);
    }
}