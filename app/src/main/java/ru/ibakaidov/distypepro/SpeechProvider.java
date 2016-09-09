package ru.ibakaidov.distypepro;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.speech.tts.TextToSpeech;

import java.util.Locale;

import ru.yandex.speechkit.SpeechKit;
import ru.yandex.speechkit.Vocalizer;

/**
 * Created by aacidov on 27.05.16.
 */
public class SpeechProvider {
    public String voice = "ZAHAR";
    public boolean isOnline = true;
    public boolean isSayAfterWordInput = false;
    private boolean isConnected;
    private TextToSpeech textToSpeech;
    private ConnectivityManager cm;

    public SpeechProvider(Context context, String apiKey) {
        SpeechKit.getInstance().configure(context, apiKey);
        textToSpeech = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {

            }
        });
        textToSpeech.setLanguage(Locale.getDefault());
        cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        update();
    }

    public void speak(String text) {
        if (isOnline && isConnected) {
            Vocalizer vocalizer = Vocalizer.createVocalizer(
                    Vocalizer.Language.RUSSIAN, text, true, voice.toLowerCase()
            );
            vocalizer.start();
            return;
        }
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null);
    }

    public void update() {
        NetworkInfo ni = cm.getActiveNetworkInfo();
        if (ni == null) {
            isConnected = false;
            return;
        }
        isConnected = ni.isConnected();
    }
}
