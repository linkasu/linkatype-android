package ru.ibakaidov.distypepro.components;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.ArrayAdapter;

import ru.ibakaidov.distypepro.R;

public class EditMenu {

    public static void showCategpryDialog(Context context, final OnCategoryDialogListener listener){
        showDialog(context, R.string.edit_category, R.array.edit_category_menu, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0: //rename
                        listener.onRename();
                        break;
                    case 1: //delete
                        listener.onRemove();
                        break;
                }
            }
        });
    }

    public static void showStatementDialog(Context context, final OnStatementDialogListener listener){
        showDialog(context, R.string.edit_statement, R.array.edit_statement_menu, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case 0:
                        listener.onPaste();
                        break;
                    case 1: //rename
                        listener.onRename();
                        break;
                    case 2:
                        listener.onChangeCategory();
                        break;
                    case 3:
                        listener.onRemove();
                        break;
                }
            }
        });
        }


    private static AlertDialog showDialog(final Context context, int titleId, int menuId, DialogInterface.OnClickListener listener){

        AlertDialog.Builder builderSingle = new AlertDialog.Builder(context);
        builderSingle.setTitle(titleId);
        builderSingle.setAdapter(new ArrayAdapter<String>(context, R.layout.support_simple_spinner_dropdown_item, context.getResources().getStringArray(menuId)), listener);
        return builderSingle.show();
    }

    private static abstract class OnDialogListener {
        public abstract void onRename();


        public abstract void onRemove();
    }

    public static abstract class OnCategoryDialogListener extends OnDialogListener {
    }

    public static abstract class OnStatementDialogListener extends OnDialogListener {
        public abstract void onChangeCategory();

        public abstract void onPaste();
    }
}
