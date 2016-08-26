package ru.ibakaidov.distypepro;

import android.view.MenuItem;

import com.yandex.metrica.YandexMetrica;

/**
 * Created by aacidov on 06.06.16.
 */
public class IsOnlineVoiceController implements MenuItem.OnMenuItemClickListener {
    private final TTS tts;

    public IsOnlineVoiceController(TTS ttobj){
        this.tts = ttobj;
    }
    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId())
        {
            case R.id.is_online_voice:
                this.tts.isOnline=!this.tts.isOnline;

                YandexMetrica.reportEvent("change online voice status", "{\"on\":"+this.tts.isOnline+"}");
                item.setChecked(this.tts.isOnline);
                break;
            case R.id.say_after_word_input:
                this.tts.isSayAfterWordInput=!this.tts.isSayAfterWordInput;
                YandexMetrica.reportEvent("say after word status", "{\"on\":"+this.tts.isSayAfterWordInput+"}");

                item.setChecked(this.tts.isSayAfterWordInput);

                break;
        }
        return false;
    }


}
