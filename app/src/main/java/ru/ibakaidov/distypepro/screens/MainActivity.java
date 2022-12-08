package ru.ibakaidov.distypepro.screens;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Spinner;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.database.FirebaseDatabase;

import ru.ibakaidov.distypepro.R;
import ru.ibakaidov.distypepro.components.BankGroup;
import ru.ibakaidov.distypepro.components.InputGroup;
import ru.ibakaidov.distypepro.utils.TTS;

public class MainActivity extends AppCompatActivity {


    private InputGroup inputGroup;
    private BankGroup bankGroup;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseDatabase
                .getInstance()
                .setPersistenceEnabled(true);
        FirebaseAnalytics
                .getInstance(this);
        setContentView(R.layout.activity_main);
        TTS tts = new TTS(this);
        inputGroup = findViewById(R.id.input_group);
        inputGroup.setTts(tts);
        bankGroup = findViewById(R.id.bank_group);
        bankGroup.setTts(tts);


    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        Spinner spinner = (Spinner) menu.findItem(R.id.chats_spinner).getActionView();
        inputGroup.setChatSpinner(spinner);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.clear_menu_item:
                inputGroup.clear();
                break;
            case R.id.settings_menu_item:
                Intent intent = new Intent();
                intent.setAction("com.android.settings.TTS_SETTINGS");
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);

                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        inputGroup.back();
        bankGroup.back();
    }
}