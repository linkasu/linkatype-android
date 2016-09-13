package ru.ibakaidov.distypepro.util;

import android.app.Activity;

import com.yandex.metrica.YandexMetrica;

import ru.ibakaidov.distypepro.R;

/**
 * Created by kolenyov on 31/08/16.
 */
public class YandexMetricaHelper {

    public static void activate(Activity activity) {
        YandexMetrica.activate(activity.getApplicationContext(), activity.getString(R.string.metrikaKey));
        YandexMetrica.enableActivityAutoTracking(activity.getApplication());
    }

    public static void pronouncedTextEvent(String text) {
        YandexMetrica.reportEvent("said", "{\"text\":\"" + text + "\"}");
    }

    public static void categoryEvent(String label) {
        YandexMetrica.reportEvent("create category", "{\"text\":\"" + label + "\"}");
    }

    public static void statementEvent(String statement) {
        YandexMetrica.reportEvent("create statement", "{\"text\":\"" + statement + "\"}");
    }

    public static void changeCategoryEvent(String statement) {
        YandexMetrica.reportEvent("change category", "{\"text\":\"" + statement + "\"}");
    }

    public static void changeOnlineValueEvent(boolean isOnline) {
        YandexMetrica.reportEvent("change online voice status", "{\"on\":"+isOnline+"}");
    }

    public static void changeSayingAfterWordValueEvent(boolean value) {
        YandexMetrica.reportEvent("say after word status", "{\"on\":"+value+"}");
    }
}
