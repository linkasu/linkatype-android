package ru.ibakaidov.distypepro;

import android.view.MenuItem;

import ru.ibakaidov.distypepro.util.YandexMetricaHelper;

/**
 * Created by aacidov on 06.06.16.
 */
public class IsOnlineVoiceController implements MenuItem.OnMenuItemClickListener {
    private final TTS mTTS;

    public IsOnlineVoiceController() {
        mTTS = TTS.getInstance();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.is_online_voice:
                mTTS.isOnline = !mTTS.isOnline;
                YandexMetricaHelper.changeOnlineValueEvent(mTTS.isOnline);
                item.setChecked(mTTS.isOnline);
                break;
            case R.id.say_after_word_input:
                mTTS.isSayAfterWordInput = !mTTS.isSayAfterWordInput;
                YandexMetricaHelper.changeSayingAfterWordValueEvent(mTTS.isSayAfterWordInput);
                item.setChecked(mTTS.isSayAfterWordInput);
                break;
        }
        return false;
    }
}
