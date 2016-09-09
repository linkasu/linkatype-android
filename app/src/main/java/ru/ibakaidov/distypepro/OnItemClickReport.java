package ru.ibakaidov.distypepro;

import android.view.MenuItem;

import ru.ibakaidov.distypepro.util.YandexMetricaHelper;

/**
 * Created by aacidov on 06.06.16.
 */
public class OnItemClickReport implements MenuItem.OnMenuItemClickListener {
    private final SpeechProvider speechProvider;

    public OnItemClickReport(SpeechProvider speechProvider) {
        this.speechProvider = speechProvider;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.is_online_voice:
                speechProvider.isOnline = !speechProvider.isOnline;
                YandexMetricaHelper.changeOnlineValueEvent(speechProvider.isOnline);
                item.setChecked(speechProvider.isOnline);
                break;
            case R.id.say_after_word_input:
                this.speechProvider.isSayAfterWordInput = !speechProvider.isSayAfterWordInput;
                YandexMetricaHelper.changeOnlineValueEvent(speechProvider.isOnline);
                item.setChecked(speechProvider.isSayAfterWordInput);
                break;
        }
        return false;
    }
}
