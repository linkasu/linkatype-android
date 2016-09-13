package ru.ibakaidov.distypepro;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.speech.tts.TextToSpeech;

import java.util.Locale;

import ru.yandex.speechkit.Vocalizer;

/**
 * Created by aacidov on 27.05.16.
 */
public class TTS {

    public static String[] VOICES = new String[]{Vocalizer.Voice.ALYSS, Vocalizer.Voice.ERMIL, Vocalizer.Voice.JANE, Vocalizer
            .Voice.OMAZH, Vocalizer.Voice.ZAHAR};

    private boolean isConnected;
    private TextToSpeech tts;
    private ConnectivityManager cm;
    private String mCurrentVoice;
    private String[] mAvailableVoices;

    public boolean isOnline = true;
    public boolean isSayAfterWordInput = false;

    private static volatile TTS instance;

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
        mCurrentVoice = Vocalizer.Voice.ZAHAR;
        mAvailableVoices = DisTypePro.getAppContext().getResources().getStringArray(R.array.voices);

        tts = new TextToSpeech(DisTypePro.getAppContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {

            }
        });
        tts.setLanguage(Locale.getDefault());
        cm = (ConnectivityManager) DisTypePro.getAppContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        update();
    }

    public void speak(String text) {

        if (this.isOnline && this.isConnected) {
            Vocalizer vocalizer = Vocalizer.createVocalizer(Vocalizer.Language.RUSSIAN, text, true, mCurrentVoice.toLowerCase());
            vocalizer.start();
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        } else {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
        }
    }

    public void update() {
        NetworkInfo ni = cm.getActiveNetworkInfo();
        if (ni == null) {
            this.isConnected = false;
            return;
        }
        ;
        this.isConnected = ni.isConnected();
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
