package ru.ibakaidov.distypepro;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ListView;

import com.yandex.metrica.YandexMetrica;


public class MainActivity extends AppCompatActivity {
    SpeechProvider speechProvider;
    private AutoCompleteTextView speechInput;
    private MenuItem.OnMenuItemClickListener onItemClickReport;
    private SpeechController speechController;
    private SayButtonController sayButtonController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        YandexMetrica.activate(getApplicationContext(), getString(R.string.metrikaKey));
        YandexMetrica.enableActivityAutoTracking(this.getApplication());

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        speechProvider = new SpeechProvider(getApplicationContext(), getString(R.string.apiKey));

        DB db = new DB(MainActivity.this, getString(R.string.withoutCategory));
        ListView wordsList = (ListView) findViewById(R.id.wordsListView);
        ListView categoriesList = (ListView) findViewById(R.id.categoriesListView);

        CategoryController categoryController = new CategoryController(categoriesList, db);
        WordsController wordsController = new WordsController(
                db, categoryController, wordsList, speechProvider
        );

        categoryController.setWordsController(wordsController);
        onItemClickReport = new OnItemClickReport(speechProvider);

        speechInput = (AutoCompleteTextView) findViewById(R.id.sayEditText);
        speechInput.setAdapter(new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, db.getStatements()
        ));

        Button sayButton = (Button) findViewById(R.id.sayButton);
        sayButtonController = new SayButtonController(
                speechInput, db, categoryController, wordsController, speechProvider
        );

        sayButton.setOnClickListener(sayButtonController);

        categoriesList.setOnItemClickListener(categoryController);
        categoriesList.setOnItemLongClickListener(categoryController);
        wordsList.setOnItemClickListener(wordsController);
        wordsList.setOnItemLongClickListener(wordsController);

        categoryController.loadCategories(this);
        wordsController.loadStatements(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        speechController = new SpeechController(getString(R.string.new_speech), speechInput);
        sayButtonController.setSpeechController(speechController);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.choose_voice) {
            final String[] voices = getResources().getStringArray(R.array.voices);

            SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
            final String sCurrentVoice = getString(R.string.current_voice);
            // TODO: 8/30/16 replace "magic number" with a constant
            int idCurrentVoice = sharedPref.getInt(sCurrentVoice, 4);

            final SharedPreferences.Editor editor = sharedPref.edit();

            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            dialogBuilder.setSingleChoiceItems(voices, idCurrentVoice, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface d, int n) {
                    d.cancel();
                    speechProvider.voice = voices[n];
                    editor.putInt(sCurrentVoice, n);
                    editor.apply();

                }
            });
            dialogBuilder.setNegativeButton(R.string.cancel, null);
            dialogBuilder.setTitle(R.string.choose_voice);
            dialogBuilder.show();
            return true;
        }

        if (id == R.id.clear) {
            speechInput.setText("");
            return true;
        }

        if (id == R.id.is_online_voice || id == R.id.say_after_word_input) {
            onItemClickReport.onMenuItemClick(item);
            return true;
        }

        if (id == R.id.selectSpeech) {
            speechController.openDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        speechProvider.update();
        super.onResume();
    }
}
