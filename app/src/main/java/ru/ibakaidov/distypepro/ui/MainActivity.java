 package ru.ibakaidov.distypepro.ui;

 import android.app.Activity;
 import android.content.Context;
 import android.content.DialogInterface;
 import android.content.Intent;
 import android.content.SharedPreferences;
 import android.media.AudioManager;
 import android.net.Uri;
 import android.os.Bundle;
 import android.support.annotation.NonNull;
 import android.support.design.widget.TabLayout;
 import android.support.v4.view.ViewPager;
 import android.support.v7.app.AlertDialog;
 import android.support.v7.app.AppCompatActivity;
 import android.support.v7.widget.Toolbar;
 import android.view.Menu;
 import android.view.MenuItem;
 import android.widget.EditText;

 import java.io.File;
 import java.io.IOException;

 import ru.aacidov.disfeedback.FeedBack;
 import ru.ibakaidov.distypepro.BellButtonController;
 import ru.ibakaidov.distypepro.FileStorage;
 import ru.ibakaidov.distypepro.IsOnlineVoiceController;
 import ru.ibakaidov.distypepro.R;
 import ru.ibakaidov.distypepro.TTS;
 import ru.ibakaidov.distypepro.util.YandexMetricaHelper;


 public class MainActivity extends AppCompatActivity {

    public static final int DEFAULT_TABS_COUNT = 3;
    public static Activity activity;

    private static final String PREFS_VOICE_INDEX = "current_voice";


    private IsOnlineVoiceController iovc;
    private ViewPager mViewPager;
    private SharedPreferences mSharedPreferences;
    private BellButtonController mBellButtonController;
    private FeedBack fb;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        activity=this;

        fb = new FeedBack(this);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mViewPager = (ViewPager) findViewById(R.id.view_pager);
        setupViewPager();

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tab_layout);
        tabLayout.setupWithViewPager(mViewPager);

        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        iovc = new IsOnlineVoiceController();
        mBellButtonController = new BellButtonController();
        mSharedPreferences = getPreferences(Context.MODE_PRIVATE);

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

     @Override
     public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
         super.onRequestPermissionsResult(requestCode, permissions, grantResults);
         FileStorage.getInstance().onRequestPermissionsResult(requestCode, permissions, grantResults);
     }

     @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.clear) {
            clearTextPressed();
            return true;
        }

        if ( id == R.id.say_after_word_input) {
            iovc.onMenuItemClick(item);
            return true;
        }

        if(id==R.id.action_save){
            getCurrentSpeechFragment().save();
            return true;
        }

        if (id==R.id.bell){
            try {
                mBellButtonController.play();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return true;
        }

        if(id==R.id.synth_to_file){
            String text="";

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.input_text_for_synth);
            final EditText input = new EditText(this);

            builder.setView(input);


            builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    String text = input.getText().toString();
                    TTS.getInstance().speakToFile(text);
                    }
            });
            builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {

                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();


            return true;
        }
        if (id==R.id.action_feedback){
            fb.openFeedbackForm();
            return true;
        }
        if(id==R.id.action_show){
            SpotlightActivity.show( getInputText());
            return true;
        }
        if(id==R.id.tts_settings){
            Intent intent = new Intent();
            intent.setAction("com.android.settings.TTS_SETTINGS");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            this.startActivity(intent);
            //YandexMetricaHelper.openTTSSettings();
            return true;
        }
        return super.onOptionsItemSelected(item);


    }
    @Override
    protected void onResume() {
        super.onResume();
    }


    private void setupViewPager() {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        for (int i = 0; i < DEFAULT_TABS_COUNT; i++) {
            String chatTitle = getString(R.string.chat) + " " + (i + 1);
            adapter.addFrag(SpeechFragment.newInstance(), chatTitle);
        }
        mViewPager.setAdapter(adapter);
        //always keep all pages in memory
        mViewPager.setOffscreenPageLimit(99);
    }

    private void clearTextPressed() {
        SpeechFragment speechFragment = getCurrentSpeechFragment();
        speechFragment.clearText();
    }
    private String getInputText() {
        int index = mViewPager.getCurrentItem();
        SpeechFragment speechFragment = (SpeechFragment) ((ViewPagerAdapter) mViewPager.getAdapter()).getItem(index);
        return speechFragment.getText();
    }

     public SpeechFragment getCurrentSpeechFragment() {
         int index = mViewPager.getCurrentItem();
         return (SpeechFragment) ((ViewPagerAdapter) mViewPager.getAdapter()).getItem(index);
     }
 }
