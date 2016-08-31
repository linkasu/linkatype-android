package ru.ibakaidov.distypepro;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;

import java.lang.reflect.Array;
import java.util.List;

import ru.ibakaidov.distypepro.util.YandexMetricaHelper;

/**
 * Created by aacidov on 29.05.16.
 */
public class SayButtonController implements View.OnClickListener {
    private AutoCompleteTextView si;
    private DB db;
    private CategoryController cc;
    private WordsController wc;
    private TTS tts;
    private SpeechController sc;


    public SayButtonController(AutoCompleteTextView speechInput, DB dateBase, CategoryController categoryController, WordsController wordsController, TTS textToSpeech) {
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
                if (!tts.isSayAfterWordInput || before > count) return;
                if (s.charAt(s.length() - 1) == ' ') {

                    String text = si.getText().toString();

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

    public void setSC(SpeechController sc) {
        this.sc = sc;
    }

    @Override
    public void onClick(View v) {
        String tfs = si.getText().toString();

        //Do not react on empty input in AutoCompleteTextView
        if (tfs.length() == 0) {
            return;
        }

        db.createStatement(tfs, 0);

        //Updating statements containing in adapter
        ArrayAdapter<String> adapter = (ArrayAdapter<String>) si.getAdapter();
        adapter.clear();
        List<String> updatedStatements = db.getStatements();
        for (String statement : updatedStatements) {
            adapter.add(statement);
        }

        if (cc.currentCategory == 0) {
            wc.loadStatements();
        }

        YandexMetricaHelper.pronouncedTextEvent(tfs);
        tts.speak(tfs);
        sc.onSay();
    }
}
