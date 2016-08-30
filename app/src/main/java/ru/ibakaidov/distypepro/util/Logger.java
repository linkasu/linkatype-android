package ru.ibakaidov.distypepro.util;

import android.util.Log;

/**
 * Created by Roman Bugaian on 30/08/16.
 */
public class Logger {


    private static final boolean IS_ENABLED = true;

    public static void log(String log) {
        if (IS_ENABLED) {
            Log.d("DISTYPE", log + " ");
        }
    }
}
