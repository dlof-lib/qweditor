package com.qw.editor;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * نقطة واحدة للوصول إلى إعدادات التطبيق (الثيم، حجم الخط، التفاف الأسطر...).
 */
public final class AppPreferences {

    private static final String PREFS_NAME = "qw_editor_prefs";

    private static final String KEY_THEME_MODE = "theme_mode";
    private static final String KEY_FONT_SIZE = "font_size";
    private static final String KEY_WORD_WRAP = "word_wrap";
    private static final String KEY_TAB_SIZE = "tab_size";
    private static final String KEY_AUTO_SAVE = "auto_save";
    private static final String KEY_SORT_MODE = "sort_mode";

    public static final String THEME_LIGHT = "light";
    public static final String THEME_DARK = "dark";
    public static final String THEME_SYSTEM = "system";

    public static final int SORT_NAME = 0;
    public static final int SORT_DATE = 1;
    public static final int SORT_TYPE = 2;

    private AppPreferences() {}

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static String getThemeMode(Context ctx) {
        return prefs(ctx).getString(KEY_THEME_MODE, THEME_DARK);
    }

    public static void setThemeMode(Context ctx, String mode) {
        prefs(ctx).edit().putString(KEY_THEME_MODE, mode).apply();
    }

    public static float getFontSize(Context ctx) {
        return prefs(ctx).getFloat(KEY_FONT_SIZE, 14f);
    }

    public static void setFontSize(Context ctx, float size) {
        prefs(ctx).edit().putFloat(KEY_FONT_SIZE, size).apply();
    }

    public static boolean isWordWrapEnabled(Context ctx) {
        return prefs(ctx).getBoolean(KEY_WORD_WRAP, false);
    }

    public static void setWordWrapEnabled(Context ctx, boolean enabled) {
        prefs(ctx).edit().putBoolean(KEY_WORD_WRAP, enabled).apply();
    }

    public static int getTabSize(Context ctx) {
        return prefs(ctx).getInt(KEY_TAB_SIZE, 4);
    }

    public static void setTabSize(Context ctx, int size) {
        prefs(ctx).edit().putInt(KEY_TAB_SIZE, size).apply();
    }

    public static boolean isAutoSaveEnabled(Context ctx) {
        return prefs(ctx).getBoolean(KEY_AUTO_SAVE, true);
    }

    public static void setAutoSaveEnabled(Context ctx, boolean enabled) {
        prefs(ctx).edit().putBoolean(KEY_AUTO_SAVE, enabled).apply();
    }

    public static int getSortMode(Context ctx) {
        return prefs(ctx).getInt(KEY_SORT_MODE, SORT_NAME);
    }

    public static void setSortMode(Context ctx, int mode) {
        prefs(ctx).edit().putInt(KEY_SORT_MODE, mode).apply();
    }
}
