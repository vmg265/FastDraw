package com.vmg265.fastdrawplus.fork.prefs;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.google.android.material.snackbar.Snackbar;

import com.vmg265.fastdrawplus.fork.R;

public class DismissableSnackbars {
    private DismissableSnackbars() {}

    private static final String PREFS_DONT_SHOW_AGAIN = "dont_show_again";

    public static void show(@NonNull final Context context, @NonNull Snackbar snackbar, @NonNull String id) {
        final SharedPreferences prefs = getPrefs(context);
        if (prefs.getBoolean(id, false)) {
            return;
        }

        snackbar.setAction(R.string.action_dont_show_again, v -> {
            prefs.edit().putBoolean(id, true).apply();
        });
        snackbar.show();
    }

    public static boolean hasDismissals(@NonNull final Context context) {
        return !getPrefs(context).getAll().isEmpty();
    }

    public static void clearDismissals(@NonNull final Context context) {
        getPrefs(context).edit().clear().apply();
    }

    private static SharedPreferences getPrefs(@NonNull final Context context) {
        return context.getSharedPreferences(PREFS_DONT_SHOW_AGAIN, Context.MODE_PRIVATE);
    }
}
