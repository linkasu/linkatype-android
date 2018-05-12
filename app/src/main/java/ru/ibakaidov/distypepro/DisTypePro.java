package ru.ibakaidov.distypepro;

import android.app.Application;
import android.content.Context;
import android.support.annotation.NonNull;

import ru.ibakaidov.distypepro.util.YandexMetricaHelper;

/**
 * Created by v.tanakov on 9/13/16.
 */
public class DisTypePro extends Application {

    private static Context appContext;

    @Override
    public void onCreate() {
        super.onCreate();

        appContext = this;

        YandexMetricaHelper.activate(this, getString(R.string.metrikaKey));
    }

    @NonNull
    public static Context getAppContext() {
        return appContext;
    }
}
