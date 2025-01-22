package yangFenTuoZi.shamikox.fragment.superuser;

import android.graphics.Rect;
import android.os.Bundle;
import android.os.RemoteException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import yangFenTuoZi.shamikox.R;
import yangFenTuoZi.shamikox.databinding.FragmentSuperuserBinding;
import yangFenTuoZi.shamikox.fragment.BaseFragment;
import yangFenTuoZi.shamikox.server.magisk.SuPolicy;

public class SuperuserFragment extends BaseFragment {
    public boolean isForeground = false;
    private FragmentSuperuserBinding binding;
    private PolicyAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSuperuserBinding.inflate(inflater, container, false);
        setupToolbar(binding.toolbar, null, R.string.superuser);

        binding.recycler.setLayoutManager(new LinearLayoutManager(mContext));
        binding.recycler.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                super.getItemOffsets(outRect, view, parent, state);
                outRect.bottom = 15;
            }
        });

        List<SuPolicy> policyList;
        try {
            policyList = Arrays.asList(mApp.iService.queryAll());
        } catch (Exception ignored) {
            policyList = new ArrayList<>();
        }
        binding.recycler.setAdapter(adapter = new PolicyAdapter(policyList, mContext.getPackageManager(), mContext, mApp));
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
