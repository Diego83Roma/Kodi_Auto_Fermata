package me.aap.fermata.ui.activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.text.InputType;
import android.widget.ImageView;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import me.aap.fermata.BuildConfig;
import me.aap.fermata.R;
import me.aap.fermata.kodi.KodiJsonRpcClient;
import me.aap.fermata.kodi.KodiDiagnostics;
import me.aap.fermata.kodi.KodiSettings;



public class HomeActivity extends AppCompatActivity {
    private static final int BG = Color.rgb(12, 17, 29);
    private static final int PANEL = Color.rgb(25, 34, 52);
    private LinearLayout root;

    @Override protected void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        buildHome();
    }

    private GradientDrawable shape(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius * getResources().getDisplayMetrics().density);
        d.setStroke((int)(1 * getResources().getDisplayMetrics().density), Color.rgb(48, 64, 88));
        return d;
    }

    private void showBlueDialog(AlertDialog dialog) {
        dialog.setOnShowListener(ignored -> {
            if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(shape(Color.rgb(24, 54, 83), 18));
            TextView title = dialog.findViewById(androidx.appcompat.R.id.alertTitle);
            if (title != null) title.setTextColor(Color.WHITE);
            TextView message = dialog.findViewById(android.R.id.message);
            if (message != null) message.setTextColor(Color.rgb(220, 232, 246));
            for (int id : new int[]{AlertDialog.BUTTON_POSITIVE, AlertDialog.BUTTON_NEGATIVE, AlertDialog.BUTTON_NEUTRAL}) {
                Button button = dialog.getButton(id);
                if (button != null) button.setTextColor(Color.rgb(143, 205, 255));
            }
        });
        dialog.show();
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private void buildHome() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int sidePadding = screenWidth >= dp(900) ? dp(48) : dp(20);
        root.setPadding(sidePadding, dp(24), sidePadding, dp(24));
        root.setBackgroundColor(BG);
        FrameLayout frame = new FrameLayout(this);
        int contentWidth = Math.min(dp(980), Math.max(dp(280), screenWidth - dp(32)));
        FrameLayout.LayoutParams contentLp = new FrameLayout.LayoutParams(contentWidth, -2, Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        frame.addView(root, contentLp);
        scroll.addView(frame, new ScrollView.LayoutParams(-1, -2));
        setContentView(scroll);

        int logoId = getResources().getIdentifier("kodiauto_original_logo", "drawable", getPackageName());
        if (logoId != 0) {
            ImageView logo = new ImageView(this);
            logo.setImageResource(logoId);
            logo.setContentDescription("Logo Kodi Auto Fermata");
            logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
            LinearLayout.LayoutParams logoLp = lp(dp(76), dp(76));
            logoLp.bottomMargin = dp(12);
            root.addView(logo, logoLp);
        }
        TextView brand = text("FERMATA KODI AUTO", 13, Color.rgb(89, 180, 255), true);
        brand.setLetterSpacing(0.18f);
        root.addView(brand);
        TextView title = text("Fermata", 34, Color.WHITE, true);
        root.addView(title);
        TextView subtitle = text("Il tuo centro multimediale", 15, Color.rgb(174, 188, 208), false);
        LinearLayout.LayoutParams subLp = lp(-1, -2); subLp.bottomMargin = dp(22);
        root.addView(subtitle, subLp);

        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setPadding(dp(18), dp(18), dp(18), dp(18));
        hero.setBackground(shape(Color.rgb(24, 54, 83), 22));
        TextView h = text("LA TUA LIBRERIA", 12, Color.rgb(143, 205, 255), true);
        hero.addView(h);
        TextView body = text("Film, serie, dirette e contenuti online in un'unica schermata.", 19, Color.WHITE, true);
        LinearLayout.LayoutParams bodyLp = lp(-1, -2); bodyLp.topMargin = dp(9);
        hero.addView(body, bodyLp);
        Button open = button("Apri libreria", () -> openFermataScreen(R.id.folders_fragment));
        LinearLayout.LayoutParams openLp = lp(-1, -2); openLp.topMargin = dp(15);
        hero.addView(open, openLp);
        LinearLayout.LayoutParams heroLp = lp(-1, -2); heroLp.bottomMargin = dp(18);
        root.addView(hero, heroLp);

        addSection("CONTENUTI FERMATA");
        addTile("TV in diretta", "Canali e playlist Fermata", "Apri la TV integrata di Fermata", () -> openFermataScreen(R.id.tv_fragment));
        addTile("Libreria", "Cartelle e contenuti multimediali", "Sfoglia i contenuti con Fermata", () -> openFermataScreen(R.id.folders_fragment));
        addTile("Preferiti", "I tuoi contenuti salvati", "Apri i preferiti di Fermata", () -> openFermataScreen(R.id.favorites_fragment));
        addTile("Playlist", "Le tue raccolte personalizzate", "Apri le playlist di Fermata", () -> openFermataScreen(R.id.playlists_fragment));

        addSection("COLLEGAMENTI KODI");
        addTile("TV in diretta Kodi", "Canali TV configurati in Kodi", "Apri TV in diretta in Kodi", () -> openKodiSection("tvchannels", null));
        addTile("Film Kodi", "Libreria film di Kodi", "Apri i film in Kodi", () -> openKodiSection("videos", "videodb://movies/titles/"));
        addTile("Serie TV Kodi", "Libreria serie TV di Kodi", "Apri le serie TV in Kodi", () -> openKodiSection("videos", "videodb://tvshows/titles/"));
        addTile("Preferiti Kodi", "Preferiti salvati in Kodi", "Apri i preferiti in Kodi", () -> openKodiSection("favouritesbrowser", null));
        addTile("Playlist Kodi", "Playlist video salvate in Kodi", "Apri le playlist in Kodi", () -> openKodiSection("videos", "special://videoplaylists/"));

        addSection("WEB E VIDEO");
        addTile("Web Browser", "Navigazione web integrata", "Apri il browser di Fermata", () -> openBuiltInAddon(R.id.web_browser_fragment));
        addTile("YouTube", "Video e canali YouTube", "Apri YouTube integrato in Fermata", () -> openBuiltInAddon(R.id.youtube_fragment));

        addSection("APP E PREFERENZE");
        addTile("Impostazioni", "Interfaccia, compatibilità e gestione", "Apri le impostazioni di Kodi Auto Fermata", this::showSettings);

        TextView note = text("Fermata Kodi Auto · interfaccia moderna", 12, Color.rgb(126, 142, 164), false);
        LinearLayout.LayoutParams noteLp = lp(-1, -2); noteLp.topMargin = dp(22);
        root.addView(note, noteLp);
    }

    private void openBuiltInAddon(int fragmentId) {
        String target = fragmentId == R.id.youtube_fragment ? "youtube"
                : fragmentId == R.id.web_browser_fragment ? "browser" : null;
        if (target != null) {
            openFermataScreen(fragmentId, Uri.parse("kodiauto://open/" + target));
        } else {
            openFermataScreen(fragmentId, null);
        }
    }

    private void openFermataScreen(int fragmentId) {
        openFermataScreen(fragmentId, null);
    }

    private void openFermataScreen(int fragmentId, Uri shortcutUri) {
        Intent intent = new Intent(this, MainActivity.class);
        if (shortcutUri != null) {
            intent.setAction(Intent.ACTION_VIEW);
            intent.setData(shortcutUri);
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            intent.addCategory(Intent.CATEGORY_BROWSABLE);
        }
        intent.putExtra("kodiauto.open_fragment", fragmentId);
        intent.putExtra("kodiauto.modern_host", true);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
    }

    private void openKodiSection(String window, String path) {
        Intent launchIntent = getPackageManager().getLaunchIntentForPackage("org.xbmc.kodi");
        if (launchIntent == null) {
            Toast.makeText(this, "Kodi non risulta installato", Toast.LENGTH_LONG).show();
            return;
        }

        try {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(launchIntent);
        } catch (Exception e) {
            Toast.makeText(this, "Impossibile aprire Kodi: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
            return;
        }

        Toast.makeText(this, "Apertura della sezione Kodi...", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            Exception lastError = null;
            KodiJsonRpcClient client = null;

            // Wait for Kodi's JSON-RPC service to become available. Retry only the
            // harmless version probe, never GUI.ActivateWindow itself.
            for (int attempt = 0; attempt < 20; attempt++) {
                try {
                    client = KodiSettings.createClient(this);
                    client.getApiVersion();
                    lastError = null;
                    break;
                } catch (Exception e) {
                    lastError = e;
                    try {
                        Thread.sleep(750);
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }

            if (client == null || lastError != null) {
                final Exception error = lastError;
                KodiDiagnostics.error("Servizio Kodi non pronto: impossibile inviare la navigazione", error);
                final String message = (error == null || error.getMessage() == null)
                        ? "Servizio JSON-RPC non disponibile" : error.getMessage();
                runOnUiThread(() -> Toast.makeText(this,
                        "Kodi si è avviato, ma il servizio di controllo non risponde: " + message,
                        Toast.LENGTH_LONG).show());
                return;
            }

            try {
                client.activateWindow(window, path);
                KodiDiagnostics.info("Aperta sezione Kodi: " + window
                        + (path == null ? "" : " -> " + path));
            } catch (Exception e) {
                KodiDiagnostics.error("Comando di navigazione Kodi non riuscito", e);
                final String message = (e.getMessage() == null)
                        ? e.getClass().getSimpleName() : e.getMessage();
                runOnUiThread(() -> Toast.makeText(this,
                        "Kodi è aperto, ma non ha accettato la navigazione: " + message,
                        Toast.LENGTH_LONG).show());
            }
        }, "Kodi-section-launch").start();
    }

    private void showSettings() {
        String[] sections = {"Interfaccia e dimensioni", "Adattamento automatico", "Riproduzione", "Kodi", "Informazioni"};
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Impostazioni Fermata Kodi Auto")
                .setItems(sections, (d, which) -> {
                    switch (which) {
                        case 0: showInterfaceSettings(); break;
                        case 1: showResizeInfo(); break;
                        case 2: showPlaybackSettings(); break;
                        case 3: showKodiSettings(); break;
                        default: showAbout(); break;
                    }
                }).setNegativeButton("Chiudi", null).create();
        showBlueDialog(dialog);
    }

    private void showInterfaceSettings() {
        final String[] sizes = {"Compatta", "Normale", "Grande"};
        int current = getSharedPreferences("ui_settings", MODE_PRIVATE).getInt("scale", 1);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Dimensione interfaccia")
                .setSingleChoiceItems(sizes, current, (d, which) -> {
                    getSharedPreferences("ui_settings", MODE_PRIVATE).edit().putInt("scale", which).apply();
                    d.dismiss();
                    Toast.makeText(this, "Dimensione salvata. Si applica alla prossima apertura.", Toast.LENGTH_SHORT).show();
                }).setNegativeButton("Annulla", null).create();
        showBlueDialog(dialog);
    }

    private void showResizeInfo() {
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Adattamento automatico")
                .setMessage("Attivo: la schermata usa la larghezza disponibile, limita la larghezza massima sui display grandi e si adatta a orientamento e dimensioni dello schermo. L'attività supporta il ridimensionamento Android.")
                .setPositiveButton("OK", null).create();
        showBlueDialog(dialog);
    }

    private void showPlaybackSettings() {
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Riproduzione")
                .setMessage("Le impostazioni del motore di riproduzione restano quelle gestite da Fermata. Le opzioni avanzate saranno esposte qui quando collegate direttamente al player, senza controlli fittizi.")
                .setPositiveButton("Apri riproduzione", (d, w) -> openFermataScreen(R.id.folders_fragment))
                .setNegativeButton("Chiudi", null).create();
        showBlueDialog(dialog);
    }

    private void showKodiSettings() {
        LinearLayout menu = createKodiSettingsMenu();
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Kodi")
                .setView(menu)
                .setNegativeButton("Chiudi", null)
                .create();
        menu.setTag(dialog);
        showBlueDialog(dialog);
    }

    private LinearLayout createKodiSettingsMenu() {
        LinearLayout menu = new LinearLayout(this);
        menu.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(20);
        menu.setPadding(padding, dp(8), padding, dp(8));

        TextView description = text(
                "Gestisci l'accesso al servizio locale Kodi (127.0.0.1:8080).",
                14, Color.rgb(174, 188, 208), false);
        LinearLayout.LayoutParams descriptionLp = lp(-1, -2);
        descriptionLp.bottomMargin = dp(12);
        menu.addView(description, descriptionLp);

        Button credentials = button("Credenziali Kodi", () -> {
            ((android.app.Dialog) menu.getTag()).dismiss();
            showKodiCredentials();
        });
        Button connection = button("Verifica collegamento", () -> {
            ((android.app.Dialog) menu.getTag()).dismiss();
            testKodiConnection();
        });
        Button diagnostics = button("Log diagnosi", () -> {
            ((android.app.Dialog) menu.getTag()).dismiss();
            showKodiDiagnostics();
        });

        for (Button action : new Button[]{credentials, connection, diagnostics}) {
            LinearLayout.LayoutParams actionLp = lp(-1, -2);
            actionLp.bottomMargin = dp(8);
            menu.addView(action, actionLp);
        }
        return menu;
    }

    private void showKodiCredentials() {
        EditText username = new EditText(this);
        username.setSingleLine(true);
        username.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_NORMAL);
        username.setHint("Nome utente Kodi");
        username.setText(KodiSettings.getUsername(this));

        EditText password = new EditText(this);
        password.setSingleLine(true);
        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        password.setHint("Password Kodi");
        try {
            password.setText(KodiSettings.getPassword(this));
        } catch (Exception e) {
            password.setText("");
            KodiDiagnostics.error("Password Kodi salvata non leggibile: inserire nuovamente le credenziali", e);
        }

        LinearLayout fields = new LinearLayout(this);
        fields.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(20);
        fields.setPadding(padding, dp(8), padding, 0);
        fields.addView(username, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        fields.addView(password, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Credenziali Kodi")
                .setMessage("Inserisci le credenziali HTTP di Kodi. Lascia vuoti entrambi i campi se l'autenticazione è disattivata.")
                .setView(fields)
                .setNegativeButton("Annulla", null)
                .setPositiveButton("Salva", (clickedDialog, which) -> {
                    try {
                        KodiSettings.save(this, username.getText().toString(),
                                password.getText().toString());
                        Toast.makeText(this, "Credenziali Kodi salvate", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(this, "Errore nel salvataggio: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                })
                .create();
        showBlueDialog(dialog);
    }

    private void showKodiDiagnostics() {
        TextView content = text(KodiDiagnostics.getLog(), 12, Color.WHITE, false);
        content.setTextIsSelectable(true);
        content.setPadding(dp(12), dp(8), dp(12), dp(8));
        ScrollView scroll = new ScrollView(this);
        scroll.addView(content);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Log diagnosi Kodi")
                .setView(scroll)
                .setPositiveButton("Copia", (d, w) -> {
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    if (clipboard != null) {
                        clipboard.setPrimaryClip(ClipData.newPlainText("Log diagnosi Kodi",
                                KodiDiagnostics.getLog()));
                        Toast.makeText(this, "Log copiato", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNeutralButton("Svuota", (d, w) -> {
                    KodiDiagnostics.clear();
                    showKodiDiagnostics();
                })
                .setNegativeButton("Chiudi", null).create();
        showBlueDialog(dialog);
    }

    private void testKodiConnection() {
        KodiDiagnostics.info("Avviato test del collegamento Kodi");
        Toast.makeText(this, "Verifica collegamento a Kodi...", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            String resultMessage;
            boolean resultConnected;
            try {
                org.json.JSONObject result = KodiSettings.createClient(this).getApiVersion();
                org.json.JSONObject version = result.optJSONObject("version");
                if (version == null) version = result;
                resultMessage = "Collegamento riuscito. Kodi JSON-RPC "
                        + version.optInt("major") + "."
                        + version.optInt("minor") + "."
                        + version.optInt("patch");
                resultConnected = true;
                KodiDiagnostics.info("Test collegamento Kodi riuscito");
            } catch (Exception e) {
                KodiDiagnostics.error("Test collegamento Kodi non riuscito", e);
                resultMessage = "Collegamento non riuscito: "
                        + ((e.getMessage() == null) ? e.getClass().getSimpleName() : e.getMessage());
                resultConnected = false;
            }
            final String message = resultMessage;
            final boolean connected = resultConnected;
            runOnUiThread(() -> showBlueDialog(new AlertDialog.Builder(this)
                    .setTitle(connected ? "Kodi collegato" : "Kodi non raggiungibile")
                    .setMessage(message)
                    .setPositiveButton("OK", null)
                    .create()));
        }, "Kodi-connection-test").start();
    }

    private void showAbout() {
        String message = "Fermata Kodi Auto\n"
                + "Versione: " + BuildConfig.VERSION_NAME + "\n"
                + "Build: " + BuildConfig.VERSION_CODE;
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Informazioni")
                .setMessage(message)
                .setPositiveButton("OK", null).create();
        showBlueDialog(dialog);
    }

    private void addSection(String title) {
        TextView t = text(title, 12, Color.rgb(134, 157, 187), true);
        t.setLetterSpacing(0.12f);
        LinearLayout.LayoutParams p = lp(-1, -2); p.topMargin = dp(8); p.bottomMargin = dp(10);
        root.addView(t, p);
    }

    private void addTile(String title, String detail, String action, Runnable run) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setBackground(shape(PANEL, 18));
        TextView t = text(title, 18, Color.WHITE, true);
        card.addView(t);
        TextView d = text(detail, 13, Color.rgb(170, 185, 205), false);
        LinearLayout.LayoutParams dlp = lp(-1, -2); dlp.topMargin = dp(4);
        card.addView(d, dlp);
        TextView a = text(action + "  ›", 13, Color.rgb(91, 183, 255), true);
        LinearLayout.LayoutParams alp = lp(-1, -2); alp.topMargin = dp(10);
        card.addView(a, alp);
        card.setOnClickListener(v -> run.run());
        LinearLayout.LayoutParams cp = lp(-1, -2); cp.bottomMargin = dp(10);
        root.addView(card, cp);
    }

    private Button button(String label, Runnable action) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(27, 119, 194)));
        b.setOnClickListener(v -> action.run());
        return b;
    }

    private int dp(int v) {
        int scale = getSharedPreferences("ui_settings", MODE_PRIVATE).getInt("scale", 1);
        float factor = scale == 0 ? 0.90f : (scale == 2 ? 1.12f : 1.0f);
        return (int)(v * getResources().getDisplayMetrics().density * factor + 0.5f);
    }
    private LinearLayout.LayoutParams lp(int w, int h) { return new LinearLayout.LayoutParams(w, h); }
}
