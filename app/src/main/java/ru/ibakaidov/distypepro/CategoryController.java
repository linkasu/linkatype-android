package ru.ibakaidov.distypepro;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by aacidov on 29.05.16.
 */
public class CategoryController implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {

    public int currentCategory = 0;
    private ListView categories;
    private DB db;
    private WordsController wc;

    public CategoryController(ListView categories, DB db) {
        this.categories = categories;
        this.db = db;
    }

    public void setWordsController(WordsController wc) {
        this.wc = wc;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
        if (categories.getAdapter().getCount() - 1 == position) {
            AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
            builder.setTitle(R.string.add_category);
            final EditText input = new EditText(view.getContext());

            builder.setView(input);

            builder.setPositiveButton(R.string.add_category, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    db.createCategory(input.getText().toString());
                    loadCategories(view.getContext());

                }
            });
            builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {

                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();
            return;
        }
        currentCategory = position;
        wc.loadStatements(view.getContext());
    }

    public void loadCategories(Context context) {
        ArrayList<String> categoriesList = db.getCategories();
        categoriesList.add(context.getString(R.string.add_category));
        String[] categories = categoriesList.toArray(new String[categoriesList.size()]);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                context, R.layout.support_simple_spinner_dropdown_item, categories
        );
        this.categories.setAdapter(adapter);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        if (position == parent.getAdapter().getCount() - 1 || position == 0) return true;
        final String selected = ((TextView) view).getText().toString();
        final AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());


        final LinearLayout ll = new LinearLayout(view.getContext());
        ll.setOrientation(LinearLayout.VERTICAL);

        String editCategory = view.getContext().getString(R.string.edit_category);
        String delete = view.getContext().getString(R.string.delete);
        String[] managerStrings = new String[]{editCategory, delete};
        final ListView managerLV = new ListView(view.getContext());

        managerLV.setAdapter(new ArrayAdapter<>(
                view.getContext(), R.layout.support_simple_spinner_dropdown_item, managerStrings
        ));


        ll.addView(managerLV);

        builder.setView(ll);

        final AlertDialog mainDialog = builder.create();
        mainDialog.setTitle(R.string.edit_category);
        mainDialog.show();

        managerLV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
                switch (position) {
                    case 0:
                        //edit category
                        AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
                        builder.setTitle(R.string.edit_category);
                        final EditText input = new EditText(view.getContext());
                        input.setText(selected);
                        builder.setView(input);

                        builder.setPositiveButton(R.string.edit_category, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {

                                db.editCategory(selected, input.getText().toString());
                                mainDialog.hide();
                                loadCategories(view.getContext());

                            }
                        });
                        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {

                            }
                        });
                        final AlertDialog editDialog = builder.create();
                        editDialog.show();

                        break;
                    case 1:
                        //delete statement
                        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dDialog, int which) {
                                switch (which) {
                                    case DialogInterface.BUTTON_POSITIVE:

                                        db.deleteCategory(selected);
                                        mainDialog.hide();
                                        loadCategories(view.getContext());
                                        break;

                                    case DialogInterface.BUTTON_NEGATIVE:

                                        break;
                                }
                            }
                        };
                        builder = new AlertDialog.Builder(view.getContext());
                        builder.setMessage(R.string.delete)
                                .setPositiveButton(R.string.yes, dialogClickListener)
                                .setNegativeButton(R.string.no, dialogClickListener)
                                .show();
                        break;
                }
            }
        });
        return true;
    }
}
