package ru.ibakaidov.distypepro.controllers;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;

import ru.ibakaidov.distypepro.structures.Category;
import ru.ibakaidov.distypepro.structures.Structure;

public class CategoriesController extends Controller<Category> {
    public CategoriesController(Context context) {
        super(context, Category.class);
        setmTableReference(
                getmRootTableReference().child(Category.class.getSimpleName()));

    }

    public void changeLabel(Category category) {
        getmTableReference().child(category.id).child("label").setValue(category.label);
    }

    public abstract static class OnCategoriesListener extends OnDataListener<Category> {
        @Override
        public void onData(ArrayList<Category> list) {
            onData( list.toArray( new Category[list.size()]));
        }

        public abstract void onData( Category[] categories);
    }

}
