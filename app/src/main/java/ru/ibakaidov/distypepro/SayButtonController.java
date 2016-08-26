package ru.ibakaidov.distypepro;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.yandex.metrica.YandexMetrica;

/**
 * Created by aacidov on 29.05.16.
 */
public class SayButtonController implements View.OnClickListener {
    private EditText si;
    private DB db;
    private CategoryController cc;
    private WordsController wc;
    private TTS tts;
    private SpeechController sc;


    public SayButtonController(EditText speechInput, DB dateBase, CategoryController categoryController, WordsController wordsController, TTS textToSpeech){
        this.si = speechInput;
        this.db = dateBase;
        this.cc = categoryController;
        this.wc = wordsController;
        this.tts = textToSpeech;

        speechInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(!tts.isSayAfterWordInput||before>count) return;
                if (s.charAt(s.length()-1)==' '){

                    String text = si.getText().toString();
;
                    String[] b = text.split("\\s+");
                    String word = b[b.length - 1];
                    tts.speak(word);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }
    public void setSC(SpeechController sc){
       this.sc = sc;
    }
    @Override
    public void onClick(View v) {
        String tfs = si.getText().toString();
        db.createStatement(tfs, 0);

        if (cc.currentCategory == 0) {
            wc.loadStatements();
        }

        YandexMetrica.reportEvent("said", "{\"text\":\""+tfs+"\"}");
        tts.speak(tfs);
        sc.onSay();
    }
}
