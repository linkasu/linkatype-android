package ru.ibakaidov.distypepro.components;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import ru.ibakaidov.distypepro.R;

public abstract class Component extends LinearLayout {

    protected int layoutId;

    public Component(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        inflate(context);
    }

    private void inflate(Context context) {
        LayoutInflater layoutInflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        layoutInflater.inflate(getLayoutId(), this, true);
        initUI();
    }

    protected abstract int getLayoutId();

    protected abstract void initUI();
}
