package ru.ibakaidov.distypepro.adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import ru.ibakaidov.distypepro.structures.Statement;

public class StatementsAdapter extends ArrayAdapter<Statement> {
    public StatementsAdapter(Context context, int resource, Statement[] statements) {
        super(context, resource, statements);

    }

    @Override
    public View getView(int position, View convertView,  ViewGroup parent) {
        convertView= super.getView(position, convertView, parent);
        TextView textView = (TextView) convertView.findViewById(android.R.id.text1);
        Statement statement = getItem(position);
        textView.setText(statement.text);
        return convertView;
    }
}
