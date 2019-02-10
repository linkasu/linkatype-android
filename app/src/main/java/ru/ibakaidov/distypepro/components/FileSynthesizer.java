package ru.ibakaidov.distypepro.components;

import android.content.Context;

import com.yarolegovich.lovelydialog.LovelyStandardDialog;
import com.yarolegovich.lovelydialog.LovelyTextInputDialog;

import ru.ibakaidov.distypepro.R;
import ru.ibakaidov.distypepro.screens.MainActivity;
import ru.ibakaidov.distypepro.utils.TTS;

public class FileSynthesizer {
    public static void show(Context context) {
        final TTS tts = new TTS(context);
        new LovelyTextInputDialog(context)
                .setTitle(R.string.synth_to_file)
                .setNegativeButton(R.string.cancel, null)
                .setConfirmButton(R.string.say, new LovelyTextInputDialog.OnTextInputConfirmListener() {
                    @Override
                    public void onTextInputConfirmed(String text) {
                        tts.speakToFile(text);
                    }
                });
    }
}
