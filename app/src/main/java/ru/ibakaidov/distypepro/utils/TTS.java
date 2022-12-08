package ru.ibakaidov.distypepro.utils;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;

import java.io.File;
import java.util.Set;
import java.util.UUID;

public class TTS {

    private final Context context;
    private final TextToSpeech tts;
    private Callback<Integer> onInitCallback;
    private Callback<ProgressState> onPlayCallback;

    public TTS(Context context){
        this.context = context;
        tts = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(onInitCallback!=null){
                    onInitCallback.onDone(status);
                }
            }
        });
    }

    public void setOnPlayCallback(Callback<ProgressState> onPlayCallback) {

        this.onPlayCallback = onPlayCallback;

        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String s) {
                onPlayCallback.onDone(ProgressState.START);
            }

            @Override
            public void onDone(String s) {
                onPlayCallback.onDone(ProgressState.STOP);
            }

            @Override
            public void onError(String s) {

            }
        });
    }

    public void setOnInitCallback(Callback<Integer> onInitCallback) {
        this.onInitCallback = onInitCallback;
    }

    public Set<Voice> getVoices(){
        return tts.getVoices();
    }
    public void speak(String text){
        if(tts.isSpeaking()){
            tts.stop();
            onPlayCallback.onDone(ProgressState.STOP);
            return;
        }

        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "1");
    }

    public File speakToBuffer(String text) throws Exception {
        File file = new File(context.getCacheDir(), UUID.randomUUID().toString()+".wav");

        int res = tts.synthesizeToFile(text, null, file,null);
        if(res!= TextToSpeech.SUCCESS) throw new Exception("synth error");
        return file;

    }
}