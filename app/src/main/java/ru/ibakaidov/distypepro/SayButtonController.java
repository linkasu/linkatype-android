package ru.ibakaidov.distypepro;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import com.yandex.metrica.YandexMetrica;

import java.util.List;

/**
 * Created by aacidov on 29.05.16.
 */
public class SayButtonController implements View.OnClickListener {
    private AutoCompleteTextView si;
    private DB db;
    private CategoryController cc;
    private WordsController wc;
    private SpeechProvider speechProvider;
    private SpeechController sc;


    public SayButtonController(AutoCompleteTextView speechInput, DB dateBase,
                               CategoryController categoryController,
                               WordsController wordsController, SpeechProvider textToSpeech) {
        this.si = speechInput;
        this.db = dateBase;
        this.cc = categoryController;
        this.wc = wordsController;
        this.speechProvider = textToSpeech;

        speechInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!speechProvider.isSayAfterWordInput || before > count) return;
                if (s.charAt(s.length() - 1) == ' ') {
                    String text = si.getText().toString();
                    String[] b = text.split("\\s+");
                    String word = b[b.length - 1];
                    speechProvider.speak(word);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    public void setSpeechController(SpeechController sc) {
        this.sc = sc;
    }

    @Override
    public void onClick(View v) {
        String tfs = si.getText().toString();
        db.createStatement(tfs, 0);

        //Updating statements containing in adapter
        ArrayAdapter<String> adapter = (ArrayAdapter<String>) si.getAdapter();
        adapter.clear();
        List<String> updatedStatements = db.getStatements();
        for (String statement : updatedStatements) {
            adapter.add(statement);
        }


        if (cc.currentCategory == 0) {
            wc.loadStatements(v.getContext());
        }

        YandexMetrica.reportEvent("said", "{\"text\":\"" + tfs + "\"}");
        speechProvider.speak(tfs);
        sc.onSay();
    }
}
