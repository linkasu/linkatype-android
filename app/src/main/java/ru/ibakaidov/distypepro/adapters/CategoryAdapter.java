package ru.ibakaidov.distypepro.adapters;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import ru.ibakaidov.distypepro.R;
import ru.ibakaidov.distypepro.structures.Category;

public class CategoryAdapter extends ArrayAdapter<Category> {


    private  final int BOLD_COLOR;
    private int mSelected;

    public CategoryAdapter(Context context, int resourceId, Category[] categories, int selected) {

        super(context, resourceId, categories);
        mSelected = selected;
        BOLD_COLOR = context.getResources().getColor(R.color.colorAccent);
    }

    public CategoryAdapter(Context context, int resourceId) {
        super(context, resourceId);
        BOLD_COLOR = context.getResources().getColor(R.color.colorAccent);
    }

    @Override
    public View getView(int position, View convertView,  ViewGroup parent) {
        convertView = super.getView(position, convertView, parent);

        Category category = getItem(position);
        TextView textView = (TextView) convertView.findViewById(android.R.id.text1);
        assert category != null;
        textView.setText(category.label);
        return  convertView;
    }
}
