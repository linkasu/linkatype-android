package ru.ibakaidov.distypepro.components;

import android.content.res.Resources;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import com.yarolegovich.lovelydialog.LovelyChoiceDialog;
import com.yarolegovich.lovelydialog.LovelyStandardDialog;
import com.yarolegovich.lovelydialog.LovelyTextInputDialog;

import java.util.ArrayList;

import ru.ibakaidov.distypepro.R;
import ru.ibakaidov.distypepro.adapters.StatementsAdapter;
import ru.ibakaidov.distypepro.controllers.StatementsController;
import ru.ibakaidov.distypepro.structures.Category;
import ru.ibakaidov.distypepro.structures.Statement;
import ru.ibakaidov.distypepro.structures.Structure;

public class StatementsList extends Component {
    private static final Statement[] EMPTY_ARRAY = new Statement[0];
    private final CategoriesList mCategoriesList;
    private final SayBox mSayBox;
    private final ListView mList;
    private final StatementsController mStatementsController;
    private final Button mCreateButton;
    private Category mCategory;
    private StatementsAdapter mAdapter;

    public StatementsList(final View rootView, final SayBox sayBox, CategoriesList categoriesList) {
        super(rootView);

        mSayBox = sayBox;
        mCategoriesList = categoriesList;


        mStatementsController = new StatementsController(mContext, null);

        mList = (ListView) rootView.findViewById(R.id.statements_list);
        mCreateButton = (Button) rootView.findViewById(R.id.create_statement_button);


        mCategoriesList.setOnCategoryUpdateListener(new CategoriesList.OnCategoryUpdateListener() {
            @Override
            public void update(Category category) {
                mCategory = category;
                mStatementsController.setmCategory(category);

                    Statement[] statements = (category.statements == null) ? EMPTY_ARRAY : category.statements.values().toArray(new Statement[0]);
                    mAdapter = new StatementsAdapter(rootView.getContext(), R.layout.support_simple_spinner_dropdown_item, statements);
                    mList.setAdapter(mAdapter);


            }
        });

        mList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Statement statement = mAdapter.getItem(position);
                sayBox.say(statement.text);
            }
        });

        mList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, final View view, int position, long id) {

                final Statement statement = mAdapter.getItem(position);

                EditMenu.showStatementDialog(view.getContext(), new EditMenu.OnStatementDialogListener() {
                    @Override
                    public void onChangeCategory() {
                        final Category[] categories = mCategoriesList.getCategories();
                        String[] titles = new String[categories.length];
                        for (int i = 0; i < categories.length; i++) {
                            titles[i] = categories[i].label;
                        }
                        new LovelyChoiceDialog(view.getContext())
                                .setTitle(R.string.change_category)
                                .setItems(titles, new LovelyChoiceDialog.OnItemSelectedListener<String>() {
                                    @Override
                                    public void onItemSelected(int position, String title) {
                                        mStatementsController.removeItem(statement);
                                        statement.categoryId = categories[position].id;
                                        mStatementsController.pushToTable(statement);
                                    }
                                })
                                .show();
                    }

                    @Override
                    public void onPaste() {
                        sayBox.pasteText(statement.text);
                    }

                    @Override
                    public void onRename() {

                        new LovelyTextInputDialog(view.getContext())
                                .setTitle(R.string.edit_statement)
                                .setInitialInput(statement.text)
                                .setConfirmButton(R.string.ok, new LovelyTextInputDialog.OnTextInputConfirmListener() {
                                    @Override
                                    public void onTextInputConfirmed(String text) {
                                        statement.text = text;
                                        mStatementsController.changeText(statement);
                                    }
                                })
                                .show();
                    }

                    @Override
                    public void onRemove() {
                        Resources resources = view.getContext().getResources();
                        int color = resources.getColor(android.R.color.holo_red_dark);
                        new LovelyStandardDialog(view.getContext())
                                .setTitle(R.string.delete)
                                .setMessage(resources.getString(R.string.delete) + " \"" + statement.text + "\"?")
                                .setTopColor(color)
                                .setPositiveButtonColor(color)
                                .setNegativeButton(R.string.cancel, null)
                                .setPositiveButton(R.string.ok, new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        mStatementsController.removeItem(statement);
                                    }
                                })
                                .show();

                    }
                });

                return true;
            }
        });

        mCreateButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                new LovelyTextInputDialog(v.getContext())
                        .setTitle(R.string.add_statement)
                        .setConfirmButton(R.string.ok, new LovelyTextInputDialog.OnTextInputConfirmListener() {
                            @Override
                            public void onTextInputConfirmed(String text) {
                                    createStatement(text);
                                     }
                        })
                        .show();

            }
        });

    }

    private void createStatement(String text) {
        Statement statement = new Statement(text, mCategory.id);
        mStatementsController.pushToTable(statement);
    }


    public void saveCurrentStatement() {
        createStatement( mSayBox.getText());
    }

    private abstract class OnStatementsListener extends ru.ibakaidov.distypepro.controllers.Controller.OnDataListener<Statement> {
        @Override
        public void onData(ArrayList<Statement> list) {
            onData(list.toArray(new Statement[list.size()]));

        }

        public abstract void onData(Statement[] statements);
    }
}
