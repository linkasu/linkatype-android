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

import ru.ibakaidov.distypepro.ui.MainActivity;
import ru.ibakaidov.distypepro.ui.SpotlightActivity;

/**
 * Created by aacidov on 29.05.16.
 */
public class WordsController implements AdapterView.OnItemLongClickListener, AdapterView.OnItemClickListener {

    private final String paste;
    private Context mContext;
    private DatabaseManager mDatabaseManager;
    private String delete;
    private String editStatement;
    private CategoryController categoryController;
    private ListView wordsLV;
    private String addStatement;
    private String show;

    public WordsController(Context context, ListView wordsLV, CategoryController categoryController) {
        this.mContext = context;
        this.mDatabaseManager = DatabaseManager.getInstance();
        this.categoryController = categoryController;
        this.wordsLV = wordsLV;
        this.addStatement = context.getString(R.string.add_statement);
        this.editStatement = context.getString(R.string.edit_statement);
        this.delete = context.getString(R.string.delete);
        this.show =  context.getString(R.string.show);
        this.paste = context.getString(R.string.paste);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        if (position == parent.getAdapter().getCount() - 1) return true;
        final String selected = ((TextView) view).getText().toString();
        final AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        final TextView titleBox = new TextView(mContext);

        titleBox.setText(R.string.change_category);
        final TextView titleEditBox = new TextView(mContext);
        titleEditBox.setText(R.string.edit_statement);

        final LinearLayout ll = new LinearLayout(mContext);
        ll.setOrientation(LinearLayout.VERTICAL);
        final ListView lv = new ListView(mContext);
        lv.setAdapter(new ArrayAdapter<>(mContext, R.layout.support_simple_spinner_dropdown_item, mDatabaseManager.getCategories().toArray(new String[]{})));

        String[] managerStrings = new String[]{show, paste, editStatement, delete};
        final ListView managerLV = new ListView(mContext);

        managerLV.setAdapter(new ArrayAdapter<>(mContext, R.layout.support_simple_spinner_dropdown_item, managerStrings));



        ll.addView(titleBox);
        ll.addView(lv);
        ll.addView(titleEditBox);
        ll.addView(managerLV);

        builder.setView(ll);


        final AlertDialog mainDialog = builder.create();
        mainDialog.show();
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mDatabaseManager.setCategory(position, selected);
                loadStatements();
                mainDialog.hide();
            }


        });
        managerLV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                switch (position) {
                    case 0:
                        //show
                        SpotlightActivity.show( selected);
                        break;
                    case 1:
                        //paste
                        ((MainActivity) MainActivity.activity).getCurrentSpeechFragment().pasteText(" "+selected+" ");
                        mainDialog.hide();
                        break;
                    case 2:
                        //edit statement
                        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                        builder.setTitle(R.string.edit_statement);
                        final EditText input = new EditText(mContext);
                        input.setText(selected);

                        builder.setView(input);


                        builder.setPositiveButton(R.string.edit_statement, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {

                                mDatabaseManager.editStatement(selected, input.getText().toString());
                                mainDialog.hide();
                                loadStatements();

                            }
                        });
                        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {

                            }
                        });
                        final AlertDialog editDialog = builder.create();
                        editDialog.show();

                        break;
                    case 3:
                        //delete statement

                        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dDialog, int which) {
                                switch (which) {
                                    case DialogInterface.BUTTON_POSITIVE:

                                        mDatabaseManager.deleteStatement(selected);
                                        mainDialog.hide();
                                        loadStatements();
                                        break;

                                    case DialogInterface.BUTTON_NEGATIVE:

                                        break;
                                }
                            }
                        };

                        builder = new AlertDialog.Builder(mContext);
                        builder.setMessage(R.string.delete).setPositiveButton(R.string.yes, dialogClickListener)
                                .setNegativeButton(R.string.no, dialogClickListener).show();
                        break;
                }
            }
        });
        return true;
    }

    public void loadStatements() {
        ArrayList<String> statements = mDatabaseManager.getStatements(categoryController.currentCategory);
        statements.add(addStatement);
        wordsLV.setAdapter(new ArrayAdapter<String>(mContext, R.layout.support_simple_spinner_dropdown_item, statements.toArray(new String[statements.size()])));
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (wordsLV.getAdapter().getCount() - 1 == position) {
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            builder.setTitle(R.string.add_statement);
            final EditText input = new EditText(mContext);
            builder.setView(input);

            builder.setPositiveButton(R.string.add_statement, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    mDatabaseManager.createStatement(input.getText().toString(), categoryController.currentCategory);
                    loadStatements();
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
        String tfs = ((TextView) view).getText().toString();
        TTS.getInstance().speak(tfs);
        mDatabaseManager.updateRating(tfs);
        loadStatements();
    }
}
