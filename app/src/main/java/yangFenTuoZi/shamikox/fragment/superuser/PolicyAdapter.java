package yangFenTuoZi.shamikox.fragment.superuser;

import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Arrays;
import java.util.List;

import yangFenTuoZi.shamikox.App;
import yangFenTuoZi.shamikox.databinding.ItemPolicyBinding;
import yangFenTuoZi.shamikox.server.magisk.SuPolicy;

public class PolicyAdapter extends RecyclerView.Adapter<PolicyAdapter.ViewHolder> {

    private List<SuPolicy> policyList;
    private final PackageManager pm;
    private boolean[] expandList;
    private final Activity mContext;
    private final App mApp;

    public PolicyAdapter(List<SuPolicy> list, PackageManager pm, Activity mContext, App mApp) {
        policyList = list;
        this.mContext = mContext;
        this.mApp = mApp;
        expandList = new boolean[policyList.size()];
        this.pm = pm;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemPolicyBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SuPolicy policy = policyList.get(position);
        holder.uid = policy.uid;
        ItemPolicyBinding binding = holder.binding;
        binding.policy.setOnClickListener(v -> holder.setExpand(!holder.isExpand()));
        binding.indicator.setChecked(policy.policy == SuPolicy.POLICY_ALLOW);
        binding.indicator.jumpDrawablesToCurrentState();
        binding.delete.setOnClickListener(v -> {
            try {
                mApp.iService.delete(policy);
                notifyItemRemoved(position);
            } catch (Exception ignored) {
            }
        });
        new Thread(() -> {
            String[] packageNames = pm.getPackagesForUid(policy.uid);
            PackageInfo pi;
            try {
                pi = pm.getPackageInfo(packageNames[0], 0);
            } catch (PackageManager.NameNotFoundException e) {
                return;
            }
            ApplicationInfo ai = pi.applicationInfo;
            if (ai == null) return;

            Drawable appIcon = ai.loadIcon(pm);
            String packageName = pi.packageName;
            String appName = ((pi.sharedUserId != null && !pi.sharedUserId.isEmpty()) ? "[SharedUID] " : "") + ai.loadLabel(pm);

            mContext.runOnUiThread(() -> {
                binding.appName.setText(appName);
                binding.packageName.setText(packageName);
                binding.appIcon.setImageDrawable(appIcon);
            });
        }).start();
    }

    @Override
    public int getItemCount() {
        return policyList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        int uid, position;
        ItemPolicyBinding binding;
        private boolean expand = true;

        public ViewHolder(ItemPolicyBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public boolean isExpand() {
            return expand;
        }

        public void setExpand(boolean expand) {
            if (!expand) {
                new Thread(() -> {
                    try {
                        SuPolicy policy = mApp.iService.queryById(uid);
                        policyList.set(position, policy);
                        mApp.runOnUiThread(() -> {
                            binding.indicator.setChecked(policy.policy == SuPolicy.POLICY_ALLOW);
                            binding.indicator.jumpDrawablesToCurrentState();

                            binding.notify.setChecked(policy.notification);
                            binding.notify.jumpDrawablesToCurrentState();

                            binding.log.setChecked(policy.logging);
                            binding.log.jumpDrawablesToCurrentState();
                        });
                    } catch (Exception ignored) {
                    }
                }).start();
            }
            binding.expandContainer.setVisibility(expand ? View.GONE : View.VISIBLE);
            this.expand = expand;
        }
    }
}