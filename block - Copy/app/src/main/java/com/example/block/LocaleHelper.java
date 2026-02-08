package com.example.block;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;

import java.util.Locale;

public class LocaleHelper {

    private static final String PREFS_NAME = "app_prefs";
    private static final String KEY_LANGUAGE = "language";

    // 🔹 Apply the chosen locale
    public static Context setLocale(Context context, String languageCode) {
        saveLanguage(context, languageCode); // save language
        return updateResources(context, languageCode);
    }

    // 🔹 Toggle between English and Amharic
    public static void toggleLanguage(Context context) {
        String currentLang = getLanguage(context);
        String newLang = currentLang.equals("en") ? "am" : "en";
        setLocale(context, newLang);
    }

    // 🔹 Get the saved language (default English)
    public static String getLanguage(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_LANGUAGE, "en");
    }

    // 🔹 Save the selected language
    private static void saveLanguage(Context context, String languageCode) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_LANGUAGE, languageCode).apply();
    }

    // 🔹 Load and apply saved language (call in BaseActivity)
    public static Context loadLocale(Context context) {
        String lang = getLanguage(context);
        return updateResources(context, lang);
    }

    // 🔹 Update app resources
    private static Context updateResources(Context context, String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        Configuration config = new Configuration(context.getResources().getConfiguration());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale);
            context = context.createConfigurationContext(config);
        } else {
            config.locale = locale;
            context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
        }
        return context;
    }
}
