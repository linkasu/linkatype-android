package ru.ibakaidov.distypepro.dialogs;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

import ru.ibakaidov.distypepro.R;
import ru.ibakaidov.distypepro.utils.Callback;

public class ContextDialog {

    public static void show(Context context, String title, Callback<ContextDialogActions> callback){
        // setup the alert builder
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);

        builder.setItems(R.array.set_context_actions, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        callback.onDone(which==0?ContextDialogActions.edit:ContextDialogActions.remove);
                    }
                })
                .setNegativeButton(R.string.cancel, null);

// create and show the alert dialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public static enum ContextDialogActions{
        edit,
        remove
    }
}
