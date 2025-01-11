package yangFenTuoZi.shamikox.dialog;

import android.content.DialogInterface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import yangFenTuoZi.shamikox.MainActivity;

public class BaseDialogBuilder extends MaterialAlertDialogBuilder {
    private final MainActivity mMainActivity;
    private AlertDialog mAlertDialog;
    private DialogInterface.OnDismissListener mOnDismissListener;

    public BaseDialogBuilder(@NonNull MainActivity context) throws DialogShowException {
        super(context);
        mMainActivity = context;
        if (mMainActivity.isDialogShow) throw new DialogShowException();
        mMainActivity.isDialogShow = true;
        super.setOnDismissListener(dialogInterface -> {
            mMainActivity.isDialogShow = false;
            if (mOnDismissListener != null)
                mOnDismissListener.onDismiss(dialogInterface);
        });
    }

    public AlertDialog getAlertDialog() {
        return mAlertDialog;
    }

    @NonNull
    @Override
    public AlertDialog create() {
        return mAlertDialog = super.create();
    }

    @NonNull
    @Override
    public BaseDialogBuilder setOnDismissListener(@Nullable DialogInterface.OnDismissListener onDismissListener) {
        mOnDismissListener = onDismissListener;
        return this;
    }

    public void runOnUiThread(Runnable action) {
        mMainActivity.runOnUiThread(action);
    }

    public static class DialogShowException extends Exception {
        private DialogShowException() {
        }
    }
}
