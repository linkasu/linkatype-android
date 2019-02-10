package ru.ibakaidov.distypepro.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class CookiesManager {
    private static final String STORAGE_NAME = "STORAGE";
    private static final String DATA_IMPORTED_ID = "DATA_IMPORTED";
    private final Context mContext;
    private final SharedPreferences mSharedPref;

    public CookiesManager(Context context){
        mContext = context;
        mSharedPref = context.getSharedPreferences(STORAGE_NAME, Context.MODE_PRIVATE);

    }

    public boolean getDataImported() {
        return mSharedPref.getBoolean(DATA_IMPORTED_ID, false);
    }

    public void setDataImported(boolean value){
        SharedPreferences.Editor editor = mSharedPref.edit();
        editor.putBoolean(DATA_IMPORTED_ID, value);
        editor.apply();
    }
}
