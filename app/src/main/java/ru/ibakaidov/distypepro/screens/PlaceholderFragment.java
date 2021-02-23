package ru.ibakaidov.distypepro.screens;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import ru.ibakaidov.distypepro.R;
import ru.ibakaidov.distypepro.components.CategoriesList;
import ru.ibakaidov.distypepro.components.SayBox;
import ru.ibakaidov.distypepro.components.StatementsList;
import ru.ibakaidov.distypepro.controllers.CategoriesController;

public class PlaceholderFragment extends Fragment {
    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private static final String ARG_SECTION_NUMBER = "section_number";
    private static PlaceholderFragment[] fragments = new PlaceholderFragment[3];
    private SayBox mSayBox;
    private CategoriesList mCategoriesList;
    private StatementsList mStatementsList;


    public PlaceholderFragment() {
    }

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static PlaceholderFragment getInstance(int sectionNumber) {
        if (fragments[sectionNumber] == null) {
            fragments[sectionNumber] = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragments[sectionNumber].setArguments(args);
        }

        return fragments[sectionNumber];
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        mSayBox = new SayBox(rootView);

        Log.d(this.getClass().getName(), "onCreateView: create categorylist");
        mCategoriesList = new CategoriesList(rootView);
        mStatementsList = new StatementsList(rootView, mSayBox, mCategoriesList);
        return rootView;
    }

    public SayBox getSayBox() {
        return mSayBox;
    }

    public CategoriesList getCategoriesList() {
        return mCategoriesList;
    }

    public StatementsList getStatementsList() {
        return mStatementsList;
    }
}

