package ru.ibakaidov.distypepro.components;

import android.content.res.Resources;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.yarolegovich.lovelydialog.LovelyStandardDialog;
import com.yarolegovich.lovelydialog.LovelyTextInputDialog;

import ru.ibakaidov.distypepro.R;
import ru.ibakaidov.distypepro.adapters.CategoryAdapter;
import ru.ibakaidov.distypepro.controllers.CategoriesController;
import ru.ibakaidov.distypepro.structures.Category;

public class CategoriesList extends Component {
    private final int BOLD_COLOR;
    private final ListView mList;
    private final CategoriesController mCategoriesController;
    private final static boolean[] init = new boolean[]{false};
    private final int DEFAULT_COLOR;
    private Category mCategory;
    private OnCategoryUpdateListener onCategoryUpdateListener;
    private CategoryAdapter mCategoryAdapter;
    private int mPreviousPosition;
    private Category[] mCategories;
    public static int lists = 0;
    private int mListNumber;

    public CategoriesList(final View rootView) {
        super(rootView);

        mListNumber = lists;
        lists++;

        DEFAULT_COLOR = mContext.getResources().getColor(android.R.color.black);
        BOLD_COLOR = mContext.getResources().getColor(R.color.colorAccent);


        mList = (ListView) rootView.findViewById(R.id.categories_list);
        Button mCreateButton = (Button) rootView.findViewById(R.id.create_category_button);

        mCategoryAdapter = new CategoryAdapter(mContext, R.layout.support_simple_spinner_dropdown_item);
        mList.setAdapter(mCategoryAdapter);

        mCategoriesController = new CategoriesController(mContext);
        mCategoriesController.setmTableReference(mCategoriesController.getmRootTableReference().child(Category.class.getSimpleName()));

        mCategoriesController.setOnDataListener(new CategoriesController.OnCategoriesListener() {
            @Override
            public void onData(Category[] categories) {
                if (categories.length == 0) {
                    if (!init[0]) {
                        init[0] = true;
                        mCategoriesController.pushToTable(new Category(mContext.getString(R.string.withoutCategory)));
                    }
                    return;
                }

                init[0] = false;
                mCategories = categories;
                int oldCategoryIndex = 0;
                if (mCategory != null) {

                    for (int i = 0; i < mCategories.length; i++) {
                        if (mCategories[i].id.equals(mCategory.id)) {
                            oldCategoryIndex = i;
                        }
                    }
                }

                Log.d(this.getClass().getName(), "onData: " + oldCategoryIndex);
                Category category = categories[oldCategoryIndex];

                mCategoryAdapter.clear();
                mCategoryAdapter.addAll(categories);
                mList.setAdapter(mCategoryAdapter);

                setmCategory(category);
            }
        });

        mList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                setmCategory(mCategoryAdapter.getItem(position));
            }
        });

        mList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, final View view, int position, long id) {
                final Category category = mCategoryAdapter.getItem(position);

                EditMenu.showCategpryDialog(mContext, new EditMenu.OnCategoryDialogListener() {
                    @Override
                    public void onRename() {
                        new LovelyTextInputDialog(mContext)
                                .setTitle(R.string.edit_category)
                                .setInitialInput(category.label)
                                .setConfirmButton(R.string.ok, new LovelyTextInputDialog.OnTextInputConfirmListener() {
                                    @Override
                                    public void onTextInputConfirmed(String text) {
                                        mCategoriesController.changeLabel(category);
                                    }
                                })
                                .show();

                    }

                    @Override
                    public void onRemove() {
                        Resources resources = mContext.getResources();
                        int color = resources.getColor(android.R.color.holo_red_dark);
                        new LovelyStandardDialog(mContext)
                                .setTitle(R.string.delete)
                                .setMessage(resources.getString(R.string.delete) + " \"" + category.label + "\"?")
                                .setTopColor(color)
                                .setPositiveButtonColor(color)
                                .setNegativeButton(R.string.cancel, null)
                                .setPositiveButton(R.string.ok, new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        mCategory = null;
                                        mCategoriesController.removeItem(category);
                                    }
                                })
                                .show();
                    }
                });

                return false;
            }
        });

        mCreateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new LovelyTextInputDialog(v.getContext())
                        .setTitle(R.string.add_category)
                        .setConfirmButton(R.string.ok, new LovelyTextInputDialog.OnTextInputConfirmListener() {
                            @Override
                            public void onTextInputConfirmed(String text) {
                                Category category = new Category(text);
                                mCategoriesController.pushToTable(category);
                                setmCategory(category);
                            }
                        })
                        .show();

            }
        });

    }

    private void setmCategory(Category category) {
        mCategory = category;
        if (onCategoryUpdateListener != null) {
            onCategoryUpdateListener.update(mCategory);
        }
    }


    public void setOnCategoryUpdateListener(OnCategoryUpdateListener onCategoryUpdateListener) {
        this.onCategoryUpdateListener = onCategoryUpdateListener;
    }


    public Category[] getCategories() {
        return mCategories;
    }

    public abstract static class OnCategoryUpdateListener {
        public abstract void update(Category mCategory);
    }
}
