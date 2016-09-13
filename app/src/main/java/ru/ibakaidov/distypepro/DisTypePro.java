package ru.ibakaidov.distypepro;

import android.app.Application;
import android.content.Context;
import android.support.annotation.NonNull;

import ru.ibakaidov.distypepro.util.YandexMetricaHelper;
import ru.yandex.speechkit.SpeechKit;

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
        SpeechKit.getInstance().configure(this, getString(R.string.speechKitApiKey));
    }

    @NonNull
    public static Context getAppContext() {
        return appContext;
    }
}