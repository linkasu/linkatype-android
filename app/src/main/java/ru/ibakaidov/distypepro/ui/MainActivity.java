 package ru.ibakaidov.distypepro.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import java.io.IOException;

import ru.ibakaidov.distypepro.BellButtonController;
import ru.ibakaidov.distypepro.IsOnlineVoiceController;
import ru.ibakaidov.distypepro.R;
import ru.ibakaidov.distypepro.TTS;


public class MainActivity extends AppCompatActivity {

    public static final int DEFAULT_TABS_COUNT = 3;
    private static final String PREFS_VOICE_INDEX = "current_voice";

    private IsOnlineVoiceController iovc;
    private ViewPager mViewPager;
    private SharedPreferences mSharedPreferences;
    private BellButtonController mBellButtonController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
        restoreVoiceSettings();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.choose_voice) {
            int idCurrentVoice = mSharedPreferences.getInt(PREFS_VOICE_INDEX, 4);

            AlertDialog.Builder adb = new AlertDialog.Builder(this);
            adb.setSingleChoiceItems(TTS.getInstance().getAvailableVoices(), idCurrentVoice, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface d, int n) {
                    TTS.getInstance().getCurrentVoice(TTS.VOICES[n]);

                    SharedPreferences.Editor editor = mSharedPreferences.edit();
                    editor.putInt(PREFS_VOICE_INDEX, n);
                    editor.apply();

                    d.dismiss();
                }
            });
            adb.setNegativeButton(R.string.cancel, null);
            adb.setTitle(R.string.choose_voice);
            adb.show();
            return true;
        }

        if (id == R.id.clear) {
            clearTextPressed();
            return true;
        }

        if (id == R.id.is_online_voice || id == R.id.say_after_word_input) {
            iovc.onMenuItemClick(item);
            return true;
        }

        if (id == R.id.addSpeech) {
            int number = mViewPager.getAdapter().getCount() + 1;
            String chatTitle = getString(R.string.chat) + " " + number;
            ((ViewPagerAdapter) mViewPager.getAdapter()).addFrag(SpeechFragment.newInstance(), chatTitle);
            return true;
        }

        if (id == R.id.removeSpeech) {
            ((ViewPagerAdapter) mViewPager.getAdapter()).removeAddedFrag();
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

        return super.onOptionsItemSelected(item);


    }
    @Override
    protected void onResume() {
        super.onResume();
        TTS.getInstance().update();
    }

    private void restoreVoiceSettings() {
        int idCurrentVoice = mSharedPreferences.getInt(PREFS_VOICE_INDEX, 4);
        TTS.getInstance().getCurrentVoice(TTS.VOICES[idCurrentVoice]);
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
        int index = mViewPager.getCurrentItem();
        SpeechFragment speechFragment = (SpeechFragment) ((ViewPagerAdapter) mViewPager.getAdapter()).getItem(index);
        speechFragment.clearText();
    }

}
