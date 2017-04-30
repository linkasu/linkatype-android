package ru.ibakaidov.distypepro;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.io.File;
import java.util.Locale;

import ru.ibakaidov.distypepro.ui.MainActivity;
import ru.yandex.speechkit.Error;
import ru.yandex.speechkit.Synthesis;
import ru.yandex.speechkit.Vocalizer;
import ru.yandex.speechkit.VocalizerListener;

/**
 * Created by aacidov on 27.05.16.
 */
public class TTS {
    public static String[] VOICES = new String[]{Vocalizer.Voice.ALYSS, Vocalizer.Voice.ERMIL, Vocalizer.Voice.JANE, Vocalizer
            .Voice.OMAZH, Vocalizer.Voice.ZAHAR};

    private TextToSpeech tts;
    private String mCurrentVoice;
    private String[] mAvailableVoices;
    static int TIMEOUT = 1500;



    public boolean isOnline = true;
    public boolean isSayAfterWordInput = false;

    private static volatile TTS instance;
    private FileStorage mfs;

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
        mfs = new FileStorage(MainActivity.activity);

        tts = new TextToSpeech(DisTypePro.getAppContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {

            }
        });
        tts.setLanguage(Locale.getDefault());

    }


    public void speak(final String text, boolean primaryOffline) {

        if (this.isOnline && ! primaryOffline ) {
            final boolean[] started = new boolean[]{false};

            final Vocalizer vocalizer = Vocalizer.createVocalizer(Vocalizer.Language.RUSSIAN, text, true, mCurrentVoice.toLowerCase());



            vocalizer.setListener(new VocalizerListener() {
                @Override
                public void onSynthesisBegin(Vocalizer vocalizer) {

                }

                @Override
                public void onSynthesisDone(Vocalizer vocalizer, Synthesis synthesis) {

                }

                @Override
                public void onPlayingBegin(Vocalizer vocalizer) {
                    started[0]=true;

                }

                @Override
                public void onPlayingDone(Vocalizer vocalizer) {

                }

                @Override
                public void onVocalizerError(Vocalizer vocalizer, Error error) {

                }
            });
            vocalizer.start();

            new Thread() {
                @Override
                public void run() {
                    try {
                        this.sleep(TIMEOUT);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    if(!started[0]){
                        vocalizer.cancel();
                        instance.speak(text, true);

                    }

                }
            }.start();


            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        } else {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
        }
    }
    public void speak(String text){
        speak(text, false);
    }
    public File speakToFile(String text){
        File file = mfs.getAudioFile();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tts.synthesizeToFile(text, null, file,null);

        }else {
            tts.synthesizeToFile(text, null, file.getAbsolutePath());
        }
        return file;
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
