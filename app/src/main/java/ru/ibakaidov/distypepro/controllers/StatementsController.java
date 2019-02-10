package ru.ibakaidov.distypepro.controllers;

import android.content.Context;

import com.google.firebase.database.FirebaseDatabase;

import ru.ibakaidov.distypepro.structures.Category;
import ru.ibakaidov.distypepro.structures.Statement;

public class StatementsController extends Controller<Statement> {
    private Category mCategory;

    public StatementsController(Context context, Category category) {
        super(context, Statement.class);
        if(category!=null) {
            setmCategory(category);
        }
    }


    @Override
    public void pushToTable(Statement data) {
        getmRootTableReference().child(Category.class.getSimpleName()).child(data.categoryId).child("statements").child(data.id).setValue(data);
    }

    public void setmCategory(Category category){
        mCategory = category;
        setmTableReference(getmRootTableReference().child(Category.class.getSimpleName()).child(category.id).child("statements"));
    }

    public void changeText(Statement statement) {
        getmTableReference().child(statement.id).child("text").setValue(statement.text);
    }
}
