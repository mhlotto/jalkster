package com.example.jalkster;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.text.TextUtils;

final class SoundPreferenceManager {

    private static final String PREFS_NAME = "jalk_prefs";
    private static final String KEY_TIMER_SOUND_URI = "timer_sound_uri";

    private SoundPreferenceManager() {
        // Utility class
    }

    static void saveSelectedSoundUri(Context context, Uri uri) {
        if (context == null) {
            return;
        }
        SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        if (uri == null) {
            editor.remove(KEY_TIMER_SOUND_URI);
        } else {
            editor.putString(KEY_TIMER_SOUND_URI, uri.toString());
        }
        editor.apply();
    }

    static Uri getSelectedSoundUri(Context context) {
        if (context == null) {
            return null;
        }
        SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String uriString = preferences.getString(KEY_TIMER_SOUND_URI, null);
        if (TextUtils.isEmpty(uriString)) {
            return null;
        }
        return Uri.parse(uriString);
    }
}
