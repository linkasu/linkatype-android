package ru.ibakaidov.distypepro;

import android.view.MenuItem;

import com.yandex.metrica.YandexMetrica;

/**
 * Created by aacidov on 06.06.16.
 */
public class OnItemClickReport implements MenuItem.OnMenuItemClickListener {
    private final SpeechProvider speechProvider;

    public OnItemClickReport(SpeechProvider ttObj) {
        speechProvider = ttObj;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.is_online_voice:
                speechProvider.isOnline = !speechProvider.isOnline;
                YandexMetrica.reportEvent(
                        "change online voice status", "{\"on\":" + speechProvider.isOnline + "}"
                );
                item.setChecked(speechProvider.isOnline);
                break;
            case R.id.say_after_word_input:
                this.speechProvider.isSayAfterWordInput = !this.speechProvider.isSayAfterWordInput;
                YandexMetrica.reportEvent(
                        "say after word status", "{\"on\":" + speechProvider.isSayAfterWordInput + "}"
                );
                item.setChecked(speechProvider.isSayAfterWordInput);
                break;
        }
        return false;
    }
}
