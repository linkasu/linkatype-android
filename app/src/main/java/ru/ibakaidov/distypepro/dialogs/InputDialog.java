package ru.ibakaidov.distypepro.dialogs;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import ru.ibakaidov.distypepro.R;
import ru.ibakaidov.distypepro.utils.Callback;

public class InputDialog {
    public static void showDialog(Context context, @StringRes int title, int type, Callback<String> listener) {
        showDialog(context, title, type, null, listener);
    }
    public static void showDialog(Context context, @StringRes int title, Callback<String> listener) {
        showDialog(context, title, null, listener);
    }

    public static void showDialog(Context context, @StringRes int title, @Nullable String currentValue, Callback<String> listener) {
        showDialog(context, title, InputType.TYPE_CLASS_TEXT, currentValue, listener);
    }

    public static void showDialog(Context context, @StringRes int title, int type, @Nullable String currentValue, Callback<String> listener) {
        LayoutInflater li = LayoutInflater.from(context);
        View promptsView = li.inflate(R.layout.input_prompt, null);
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
        alertDialogBuilder.setView(promptsView);

        final EditText userInput = (EditText) promptsView
                .findViewById(R.id.input_prompt);
        if (currentValue != null) {
            userInput.setText(currentValue);
        }
        userInput.setInputType(type);
        alertDialogBuilder
                .setTitle(title)
                .setPositiveButton(R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                /** DO THE METHOD HERE WHEN PROCEED IS CLICKED*/
                                String user_text = (userInput.getText()).toString().trim();
                                if (!user_text.equals("")) {
                                    listener.onDone(user_text);
                                } else {
                                    listener.onError(null);
                                }
                                dialog.dismiss();
                            }

                        })
                .setNegativeButton(R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                                listener.onError(null);
                            }

                        }

                ).setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        listener.onError(null);
                    }
                });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                userInput.requestFocus();
            }
        });
        // show it
        alertDialog.show();

    }
}