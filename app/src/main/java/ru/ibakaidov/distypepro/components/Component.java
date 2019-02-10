package ru.ibakaidov.distypepro.components;

import android.content.Context;
import android.view.View;

public abstract class Component {

    public final Context mContext;
    private final View mView;

    Component(View view){
        this.mView = view;
        this.mContext = view.getContext();

    }
}
