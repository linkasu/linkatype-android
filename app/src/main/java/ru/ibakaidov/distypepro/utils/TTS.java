package ru.ibakaidov.distypepro.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import androidx.annotation.RequiresApi;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import ru.ibakaidov.distypepro.R;

public class TTS {
    private final Context mContext;
    private final TextToSpeech mTextToSpeech;

    public TTS(Context context) {
        mContext = context;


        mTextToSpeech = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                //TODO: on engine error


            }
        });

    }

    @RequiresApi(api = Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
    public TTS(Context context, UtteranceProgressListener listener) {
        this(context);
        mTextToSpeech.setOnUtteranceProgressListener(listener);
    }

    public void speak(String text) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mTextToSpeech.speak(text, TextToSpeech.QUEUE_ADD, null, "main");
        } else {
            mTextToSpeech.speak(text, TextToSpeech.QUEUE_ADD, null);
        }

    }

    public boolean getSpeaking() {
        return mTextToSpeech.isSpeaking();
    }

    public void stop() {
        mTextToSpeech.stop();
    }

    public void speakToFile(final String text) {
        Dexter.withActivity((Activity) mContext)
                .withPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {

                        SimpleDateFormat simpleDate = new SimpleDateFormat("dd-MM-yyyy-HH-mm-ss");

                        String filename = simpleDate.format(new Date()) + ".wav";
                        File root = new File(Environment.getExternalStorageDirectory(), mContext.getResources().getString(R.string.app_name));
                        File file = new File(root, filename);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            mTextToSpeech.synthesizeToFile(text, null, file, null);

                        } else {
                            mTextToSpeech.synthesizeToFile(text, null, file.getAbsolutePath());
                        }

                        Intent shareIntent = new Intent();
                        shareIntent.setAction(Intent.ACTION_SEND);
                        shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file:///" + file.getAbsolutePath()));
                        shareIntent.setType("audio/*");
                        mContext.startActivity(shareIntent);
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {

                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {

                    }
                })
                .check();
    }
}
