package ru.ibakaidov.distypepro;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.speech.tts.TextToSpeech;

import java.util.Locale;

import ru.yandex.speechkit.*;

/**
 * Created by aacidov on 27.05.16.
 */
public class TTS  {

    private Context cxt;
    private boolean isConnected;
    private TextToSpeech tts;
    private ConnectivityManager cm;
    public String voice="ZAHAR";
    public boolean isOnline=true;
    public boolean isSayAfterWordInput=false;

    public TTS(Context cxt, String apiKey){
        this.cxt=cxt;
        SpeechKit.getInstance().configure(cxt, apiKey);
        tts= new TextToSpeech(cxt, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {

            }
        });
        tts.setLanguage(Locale.getDefault());
        cm = (ConnectivityManager)this.cxt.getSystemService(Context.CONNECTIVITY_SERVICE);
        final TTS self = this;

        this.update();
    }
    public void speak(String text){

        if (this.isOnline&&this.isConnected && (Locale.getDefault().getCountry()=="RU")){

            Vocalizer vocalizer = Vocalizer.createVocalizer(Vocalizer.Language.RUSSIAN, text, true, voice.toLowerCase());

            vocalizer.start();

            return;
        }
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
    }
    public void update (){
        NetworkInfo ni = cm.getActiveNetworkInfo();
        if (ni == null) {
            this.isConnected=false;
            return;
        }
        ;
        this.isConnected = ni.isConnected();
    }


}
