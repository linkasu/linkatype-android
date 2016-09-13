package ru.ibakaidov.distypepro.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ListView;

import ru.ibakaidov.distypepro.CategoryController;
import ru.ibakaidov.distypepro.DatabaseManager;
import ru.ibakaidov.distypepro.R;
import ru.ibakaidov.distypepro.SayButtonController;
import ru.ibakaidov.distypepro.WordsController;

/**
 * Created by v.tanakov on 9/13/16.
 */
public class SpeechFragment extends Fragment {

    private Button mButtonSay;
    private ListView mListViewWords;
    private ListView mListViewCategories;
    private AutoCompleteTextView mAutoCompleteTextView;

    public static SpeechFragment newInstance() {

        Bundle args = new Bundle();

        SpeechFragment fragment = new SpeechFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_speech, container, false);

        mButtonSay = (Button) view.findViewById(R.id.sayButton);
        mListViewWords = (ListView) view.findViewById(R.id.wordsListView);
        mListViewCategories = (ListView) view.findViewById(R.id.categoriesListView);
        mAutoCompleteTextView = (AutoCompleteTextView) view.findViewById(R.id.sayEditText);

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_dropdown_item_1line, DatabaseManager.getInstance()
                .getStatements());
        mAutoCompleteTextView.setAdapter(adapter);

        final CategoryController cc = new CategoryController(getContext(), mListViewCategories);
        final WordsController wc = new WordsController(getContext(), mListViewWords, cc);
        cc.setWC(wc);

        mButtonSay.setOnClickListener(new SayButtonController(mAutoCompleteTextView, cc, wc));

        mListViewCategories.setOnItemClickListener(cc);
        mListViewCategories.setOnItemLongClickListener(cc);
        mListViewWords.setOnItemClickListener(wc);
        mListViewWords.setOnItemLongClickListener(wc);

        cc.loadCategories();
        wc.loadStatements();
    }

    public void clearText() {
        mAutoCompleteTextView.setText("");
    }
}
