package com.app.bridge;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

/**
 * Created by lazy on 2019-09-30
 */
public class PermissionDialog extends DialogFragment {
    private CallBack callBack;

    interface CallBack {
        void onReTry();
    }

    public void setCallback(CallBack callBack) {
        this.callBack = callBack;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("温馨提示");
        builder.setMessage("您拒绝了应用所需要的权限，会导致部分功能不可使用，请点击[确定]重新允许");
        builder.setNegativeButton("退出", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (null != getActivity())
                    getActivity().finish();
            }
        });

        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (null != callBack)
                    callBack.onReTry();
            }
        });

        return builder.create();
    }
}
