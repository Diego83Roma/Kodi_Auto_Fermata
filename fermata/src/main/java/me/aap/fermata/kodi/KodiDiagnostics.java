package me.aap.fermata.kodi;

import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import me.aap.fermata.FermataApplication;

/** Small persistent, bounded diagnostic log for the local Kodi connection. */
public final class KodiDiagnostics {
    private static final String PREFS = "kodi_diagnostics";
    private static final String KEY_LOG = "entries";
    private static final int MAX_ENTRIES = 150;
    private static final int MAX_CHARS = 24000;

    private KodiDiagnostics() {
    }

    public static synchronized void info(String message) {
        append("INFO", message);
    }

    public static synchronized void error(String message, Throwable error) {
        String detail = (error == null) ? "" : ": "
                + error.getClass().getSimpleName() + " - " + safe(error.getMessage());
        append("ERROR", message + detail);
    }

    private static void append(String level, String message) {
        try {
            SharedPreferences prefs = FermataApplication.get()
                    .getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE);
            String old = prefs.getString(KEY_LOG, "");
            String stamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ITALY)
                    .format(new Date());
            String entry = stamp + " [" + level + "] " + safe(message);
            String combined = old.isEmpty() ? entry : old + "\n" + entry;
            String[] lines = combined.split("\\n");
            StringBuilder bounded = new StringBuilder();
            int start = Math.max(0, lines.length - MAX_ENTRIES);
            for (int i = start; i < lines.length; i++) {
                if (bounded.length() != 0) bounded.append('\n');
                bounded.append(lines[i]);
            }
            String result = bounded.toString();
            if (result.length() > MAX_CHARS) {
                result = result.substring(result.length() - MAX_CHARS);
                int firstLine = result.indexOf('\n');
                if (firstLine >= 0) result = result.substring(firstLine + 1);
            }
            prefs.edit().putString(KEY_LOG, result).apply();
        } catch (Exception ignored) {
            // Diagnostics must never interfere with playback or connection checks.
        }
    }

    private static String safe(String value) {
        if (value == null) return "";
        return value.replace('\n', ' ').replace('\r', ' ').trim();
    }

    public static synchronized String getLog() {
        return FermataApplication.get().getSharedPreferences(PREFS,
                android.content.Context.MODE_PRIVATE).getString(KEY_LOG,
                "Nessun evento registrato.");
    }

    public static synchronized void clear() {
        FermataApplication.get().getSharedPreferences(PREFS,
                android.content.Context.MODE_PRIVATE).edit().remove(KEY_LOG).apply();
        info("Log diagnosi svuotato");
    }
}
