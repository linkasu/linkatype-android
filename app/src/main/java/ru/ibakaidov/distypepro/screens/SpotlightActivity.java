package ru.ibakaidov.distypepro.screens;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import ru.ibakaidov.distypepro.R;
/**
 * Created by aacidov on 01.04.2018.
 */

public class SpotlightActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.spotlight);


        TextView textView = (TextView) findViewById(R.id.textView);

        Bundle bundle = getIntent().getExtras();

        String text = bundle.getString("text");
        textView.setText(text);
        textView.setOnClickListener(this);
    }



    public static void show(Context cxt, String text) {
        Intent intent = new Intent(cxt, SpotlightActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString("text", text);
        intent.putExtras(bundle);
        cxt.startActivity(intent);
    }

    @Override
    public void onClick(View view) {
        finish();
    }
}