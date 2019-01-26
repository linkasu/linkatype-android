package ru.ibakaidov.distypepro;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import java.util.List;

import ru.ibakaidov.distypepro.util.YandexMetricaHelper;

/**
 * Created by aacidov on 29.05.16.
 */
public class SayButtonController implements View.OnClickListener {

    private AutoCompleteTextView mAutoCompleteTextView;
    private DatabaseManager mDatabaseManager;
    private CategoryController mCategoryController;
    private WordsController mWordsController;


    public SayButtonController(AutoCompleteTextView speechInput, CategoryController categoryController, WordsController wordsController) {
        this.mAutoCompleteTextView = speechInput;
        this.mDatabaseManager = DatabaseManager.getInstance();
        this.mCategoryController = categoryController;
        this.mWordsController = wordsController;

        speechInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!TTS.getInstance().isSayAfterWordInput || before > count) return;
                if (s.charAt(s.length() - 1) == ' ') {

                    String text = mAutoCompleteTextView.getText().toString();

                    String[] b = text.split("\\s+");
                    String word = b[b.length - 1];
                    TTS.getInstance().speak(word, true);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    @Override
    public void onClick(View v) {
        String tfs = mAutoCompleteTextView.getText().toString();

        //Do not react on empty input in AutoCompleteTextView
        if (tfs.length() == 0) {
            return;
        }


        //YandexMetricaHelper.pronouncedTextEvent(tfs);
        TTS.getInstance().speak(tfs);
    }
}
