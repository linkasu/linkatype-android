package ru.ibakaidov.distypepro;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.session.MediaController;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ListView;

import ru.ibakaidov.distypepro.util.YandexMetricaHelper;


public class MainActivity extends AppCompatActivity {

    TTS tts;
    private DB db;
    private ListView wordsLV;
    private ListView categoriesLV;
    private AutoCompleteTextView si;
    private IsOnlineVoiceController iovc;
    private SpeechController sc;
    private SayButtonController sbc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        YandexMetricaHelper.activate(this);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        tts=new TTS(getApplicationContext(), getString(R.string.apiKey));


        db = new DB(MainActivity.this, getString(R.string.withoutCategory));
        wordsLV = (ListView) findViewById(R.id.wordsListView);
        categoriesLV = (ListView) findViewById(R.id.categoriesListView);

        final CategoryController cc=new CategoryController(categoriesLV, this, db, getString(R.string.add_category), getString(R.string.edit_category), getString(R.string.delete));
        final WordsController wc = new WordsController(this, db, getString(R.string.delete), getString(R.string.edit_statement), getString(R.string.add_statement), cc, wordsLV, tts);

        cc.setWC(wc);
        iovc = new IsOnlineVoiceController(tts);

        si = (AutoCompleteTextView) findViewById(R.id.sayEditText);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_dropdown_item_1line, db.getStatements());
        si.setAdapter(adapter);

        Button sb = (Button) findViewById(R.id.sayButton);
        sbc = new SayButtonController(si, db, cc, wc, tts);

        sb.setOnClickListener(sbc);

        categoriesLV.setOnItemClickListener(cc);
        categoriesLV.setOnItemLongClickListener(cc);
        wordsLV.setOnItemClickListener(wc);
        wordsLV.setOnItemLongClickListener(wc);

        cc.loadCategories();
        wc.loadStatements();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        sc = new SpeechController(this,  getString(R.string.new_speech), si);
        sbc.setSC(sc);
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
            final String[] voiceNames = new String[]{"ALYSS","ERMIL","JANE","OMAZH","ZAHAR"};


            SharedPreferences sharedPref = MainActivity.this.getPreferences(Context.MODE_PRIVATE);
            final String sCurrentVoice=getString(R.string.current_voice);
            int idCurrentVoice = sharedPref.getInt(sCurrentVoice, 4);

            final SharedPreferences.Editor editor = sharedPref.edit();

            AlertDialog.Builder adb = new AlertDialog.Builder(this);

            adb.setSingleChoiceItems(voices, idCurrentVoice, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface d, int n) {
                    d.cancel();
                    tts.voice=voiceNames[n];
                    editor.putInt(sCurrentVoice, n);
                    editor.apply();

                }

            });
            adb.setNegativeButton(R.string.cancel, null);
            adb.setTitle(R.string.choose_voice);
            adb.show();
            return true;
        }

        if (id==R.id.clear){
            si.setText("");
            return true;
        }

        if (id==R.id.is_online_voice||id==R.id.say_after_word_input){
            iovc.onMenuItemClick(item);
            return true;
        }

        if(id==R.id.selectSpeech){
            sc.openDialog();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        tts.update();
        super.onResume();
    }
}
