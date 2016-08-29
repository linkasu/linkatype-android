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
public class WordsController implements AdapterView.OnItemLongClickListener, AdapterView.OnItemClickListener {
    private Context cxt;
    private DB db;
    private String delete;
    private String editStatement;
    private CategoryController categoryController;
    private ListView wordsLV;
    private String addStatement;
    private TTS tts;

    public WordsController(Context cxt, DB db, String delete, String editStatement, String addStatement, CategoryController categoryController, ListView wordsLV, TTS tts){
        this.cxt = cxt;
        this.db = db;
        this.delete = delete;
        this.editStatement = editStatement;
        this.categoryController = categoryController;
        this.wordsLV = wordsLV;
        this.addStatement = addStatement;
        this.tts = tts;
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        if (position==parent.getAdapter().getCount()-1) return true;
        final String selected = ((TextView) view).getText().toString();
        final AlertDialog.Builder builder = new AlertDialog.Builder(cxt);
        final TextView titleBox = new TextView(cxt);

        titleBox.setText(R.string.change_category);
        final TextView titleEditBox = new TextView(cxt);
        titleEditBox.setText(R.string.edit_statement);

       final LinearLayout ll = new LinearLayout(cxt);
        ll.setOrientation(LinearLayout.VERTICAL);
        final ListView lv = new ListView(cxt);
        lv.setAdapter(new ArrayAdapter<String>(cxt, R.layout.support_simple_spinner_dropdown_item, db.getCategories().toArray(new String[]{})));
        
        String[] managerStrings = new String[]{editStatement, delete};
        final ListView managerLV = new ListView(cxt);

        managerLV.setAdapter(new ArrayAdapter<String>(cxt, R.layout.support_simple_spinner_dropdown_item, managerStrings));

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
                db.setCategory(position, selected);
                loadStatements();
                mainDialog.hide();
            }


        });
        managerLV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                switch (position){
                    case 0:
                        //edit statement
                        AlertDialog.Builder builder = new AlertDialog.Builder(cxt);
                        builder.setTitle(R.string.edit_statement);
                        final EditText input = new EditText(cxt);
                        input.setText(selected);

                        builder.setView(input);


                        builder.setPositiveButton(R.string.edit_statement, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {

                                db.editStatement(selected, input.getText().toString());
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
                    case 1:
                        //delete statement
                        
                        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dDialog, int which) {
                                switch (which){
                                    case DialogInterface.BUTTON_POSITIVE:

                                        db.deleteStatement(selected);
                                        mainDialog.hide();
                                        loadStatements();
                                        break;

                                    case DialogInterface.BUTTON_NEGATIVE:

                                        break;
                                }
                            }
                        };

                        builder = new AlertDialog.Builder(cxt);
                        builder.setMessage(R.string.delete).setPositiveButton(R.string.yes, dialogClickListener)
                                .setNegativeButton(R.string.no, dialogClickListener).show();
                        break;
                }
            }
        });
        return true;
    }
    public void loadStatements() {
        ArrayList< String > statements = db.getStatements(categoryController.currentCategory);
        statements.add(addStatement);
        wordsLV.setAdapter(new ArrayAdapter<String>(cxt, R.layout.support_simple_spinner_dropdown_item, statements.toArray(new String[statements.size()])));
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if( wordsLV.getAdapter().getCount()-1==position){
            AlertDialog.Builder builder = new AlertDialog.Builder(cxt);
            builder.setTitle(R.string.add_statement);
            final EditText input = new EditText(cxt);
            builder.setView(input);

            builder.setPositiveButton(R.string.add_statement, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    db.createStatement(input.getText().toString(), categoryController.currentCategory);
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
        String tfs=((TextView) view).getText().toString();
        tts.speak(tfs);
        db.updateRating(tfs);
        loadStatements();
    }
}
