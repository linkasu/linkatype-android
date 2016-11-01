package ru.ibakaidov.distypepro;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.media.ToneGenerator;
import android.net.Uri;

import java.io.IOException;

/**
 * Created by aacidov on 21.10.16.
 */
public class BellButtonController {
    final ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, ToneGenerator.MAX_VOLUME);

    public BellButtonController(){

    }
    public void play () throws IOException {
        tg.startTone(ToneGenerator.TONE_CDMA_DIAL_TONE_LITE);
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        tg.stopTone();
//        if (audioManager.getStreamVolume(AudioManager.STREAM_ALARM) != 0) {
//            mMediaPlayer.reset();
//
//            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
//            mMediaPlayer.prepare();
//            mMediaPlayer.setLooping(false);
//            mMediaPlayer.start();
//        }
    }
}
