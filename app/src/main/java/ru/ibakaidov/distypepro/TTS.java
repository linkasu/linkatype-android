
package ru.ibakaidov.distypepro;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Debug;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Locale;

import ru.ibakaidov.distypepro.ui.MainActivity;
import ru.ibakaidov.distypepro.util.YandexMetricaHelper;


/**
 * Created by aacidov on 27.05.16.
 */
public class TTS {
    //public static String[] VOICES = new String[]{Vocalizer.Voice.ALYSS, Vocalizer.Voice.ERMIL, Vocalizer.Voice.JANE, Vocalizer.Voice.OMAZH, Vocalizer.Voice.ZAHAR};

    private TextToSpeech tts;
    private String mCurrentVoice;
    private String[] mAvailableVoices;
    static int TIMEOUT = 1500;



    public boolean isOnline = true;
    public boolean isSayAfterWordInput = false;

    private static volatile TTS instance;
    private FileStorage mfs;
    private MediaPlayer mp;

    public static TTS getInstance() {
        TTS localInstance = instance;
        if (localInstance == null) {
            synchronized (TTS.class) {
                localInstance = instance;
                if (localInstance == null) {
                    instance = localInstance = new TTS();
                }
            }
        }
        return localInstance;
    }

    private TTS() {
        mp = new MediaPlayer();
        mfs = FileStorage.getInstance();

        tts = new TextToSpeech(DisTypePro.getAppContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    if(!tts.getAvailableLanguages().contains(Locale.getDefault())) {
                        AlertDialog.Builder dlgAlert  = new AlertDialog.Builder(MainActivity.activity);
                        dlgAlert.setMessage(R.string.install_tts);
                        dlgAlert.setTitle(R.string.lang_doesnt_support);
                        dlgAlert.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                       //         YandexMetricaHelper.openTTSInstall();
                                final String appPackageName = "com.google.android.tts";
                                try {
                                    MainActivity.activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
                                } catch (android.content.ActivityNotFoundException anfe) {
                                    MainActivity.activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
                                }
                            }
                        });
                        dlgAlert.create().show();

                    }

                }
            }
        });
        tts.setLanguage(Locale.getDefault());

    }


    public void speak(final String text, boolean primaryOffline) {
        File file = null;
        try {
           file = File.createTempFile(System.currentTimeMillis()+"", ".wav");

            file.setReadable(true);
            file.setWritable(true);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                tts.synthesizeToFile(text, null, file, null);
            } else {
                tts.synthesizeToFile(text, null, file.getAbsolutePath());
            }

        } catch (IOException e) {
            Log.e("synth to file error", e.getMessage());
            e.printStackTrace();
        }
            Uri alarmSound = null;
            Uri ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);


            try{
                assert file != null;
                Log.d("file path", file.getAbsolutePath());
                alarmSound = Uri.fromFile(file);
            }catch(Exception e){
                alarmSound = ringtoneUri;
            }
            finally{
                if(alarmSound == null){
                    alarmSound = ringtoneUri;
                }
            }

            try {
                Log.d("alarm sound uri", "path: "+alarmSound.getPath());
                if(!mp.isPlaying()){
                    FileInputStream fisAudio = new FileInputStream(file);
                    mp.setDataSource(fisAudio.getFD());
                    mp.setAudioStreamType(AudioManager.STREAM_ALARM);
                    mp.setLooping(true);
                    mp.prepare();
                    mp.start();
                }


            } catch (IOException e) {
                Log.e("tts", e.getLocalizedMessage());
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    tts.speak(text, TextToSpeech.QUEUE_FLUSH,null, null);
                } else {
                    tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
                }
            }


    }
    public void speak(String text){
        speak(text, false);
    }
    public void stop(){

    }
    public void speakToFile(final String text){
        mfs.getAudioFile(new FileStorage.OnAudioFile(){
            @Override
            public void onCreate(File file) {
                super.onCreate(file);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    tts.synthesizeToFile(text, null, file,null);

                }else {
                    tts.synthesizeToFile(text, null, file.getAbsolutePath());
                }

                Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file:///"+file.getAbsolutePath()));
                shareIntent.setType("audio/*");
                MainActivity.activity.startActivity(Intent.createChooser(shareIntent, MainActivity.activity.getResources().getText(R.string.send_to)));

            }

            @Override
            public void onFail() {
                super.onFail();
                Toast.makeText(MainActivity.activity, R.string.fail, Toast.LENGTH_LONG).show();
            }
        });
    }

    public String[] getAvailableVoices() {
        return mAvailableVoices;
    }

    public String getCurrentVoice() {
        return mCurrentVoice;
    }

    public void getCurrentVoice(String voice) {
        this.mCurrentVoice = voice;
    }
}
