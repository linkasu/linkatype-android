package ru.ibakaidov.distypepro.dialogs;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

import androidx.annotation.StringRes;

import ru.ibakaidov.distypepro.R;
import ru.ibakaidov.distypepro.utils.Callback;

public class ConfirmDialog {
    public static void showConfirmDialog(Context context, @StringRes int title, Callback callback){
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder
                .setTitle(title)
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        callback.onError(null);
                    }
                })
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        callback.onDone(null);
                    }
                })
                .show();
    }
}

