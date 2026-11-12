package me.aap.fermata.ui.activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public class HomeActivity extends AppCompatActivity {
    private static final int PICK_ADDON_ZIP = 4102;
    private static final int BG = Color.rgb(12, 17, 29);
    private static final int PANEL = Color.rgb(25, 34, 52);
    private LinearLayout root;
    private final List<String> installed = new ArrayList<>();

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
        root.setPadding(dp(20), dp(28), dp(20), dp(24));
        root.setBackgroundColor(BG);
        scroll.addView(root);
        setContentView(scroll);

        TextView brand = text("KODI AUTO", 13, Color.rgb(89, 180, 255), true);
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
        Button open = button("Apri libreria Fermata", () -> startActivity(new Intent(this, MainActivity.class)));
        LinearLayout.LayoutParams openLp = lp(-1, -2); openLp.topMargin = dp(15);
        hero.addView(open, openLp);
        LinearLayout.LayoutParams heroLp = lp(-1, -2); heroLp.bottomMargin = dp(18);
        root.addView(hero, heroLp);

        addSection("CONTENUTI");
        addTile("TV in diretta", "Canali e playlist", "Apri la libreria per gestire le sorgenti", () -> startActivity(new Intent(this, MainActivity.class)));
        addTile("Film e serie", "La tua raccolta multimediale", "Apri la libreria per sfogliare i contenuti", () -> startActivity(new Intent(this, MainActivity.class)));

        addSection("ADD-ON E REPOSITORY");
        addTile("Installa da ZIP", "Aggiungi un pacchetto compatibile", "Seleziona un file ZIP dalla memoria", this::pickZip);
        addTile("I miei Add-on", "Pacchetti installati", "Visualizza gli add-on importati", this::showInstalled);
        addTile("Repository", "Sorgenti di pacchetti", "Aggiungi o rimuovi indirizzi repository", this::manageRepositories);

        TextView note = text("Kodi Auto Fermata · nuova interfaccia in sviluppo", 12, Color.rgb(126, 142, 164), false);
        LinearLayout.LayoutParams noteLp = lp(-1, -2); noteLp.topMargin = dp(22);
        root.addView(note, noteLp);
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

    private void pickZip() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("application/zip");
        try { startActivityForResult(i, PICK_ADDON_ZIP); }
        catch (Exception e) {
            i.setType("*/*");
            startActivityForResult(i, PICK_ADDON_ZIP);
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK_ADDON_ZIP || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        try {
            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Exception ignored) { }
        new Thread(() -> importZip(uri)).start();
    }

    private void importZip(Uri uri) {
        File staging = new File(getFilesDir(), "addons/import-" + System.currentTimeMillis());
        try {
            if (!staging.mkdirs() && !staging.isDirectory()) throw new Exception("Cartella temporanea non disponibile");
            JSONObject meta = null;
            int files = 0;
            long total = 0;
            try (InputStream raw = new BufferedInputStream(getContentResolver().openInputStream(uri));
                 ZipInputStream zip = new ZipInputStream(raw)) {
                ZipEntry entry;
                byte[] buffer = new byte[8192];
                while ((entry = zip.getNextEntry()) != null) {
                    if (++files > 2000) throw new Exception("Troppi file nel pacchetto");
                    String name = entry.getName().replace('\\', '/');
                    if (name.startsWith("/") || name.contains("../") || name.equals("..") || name.contains("/..")) throw new Exception("Percorso ZIP non valido");
                    if (entry.isDirectory()) continue;
                    if (name.equals("addon.json")) {
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        int n; while ((n = zip.read(buffer)) > 0) { total += n; if (total > 80L*1024*1024) throw new Exception("ZIP troppo grande"); out.write(buffer,0,n); }
                        meta = new JSONObject(out.toString("UTF-8"));
                        continue;
                    }
                    File target = new File(staging, name);
                    if (!target.getCanonicalPath().startsWith(staging.getCanonicalPath() + File.separator)) throw new Exception("Percorso ZIP non sicuro");
                    File parent = target.getParentFile();
                    if (parent != null) parent.mkdirs();
                    try (FileOutputStream out = new FileOutputStream(target)) {
                        int n; while ((n = zip.read(buffer)) > 0) { total += n; if (total > 80L*1024*1024) throw new Exception("ZIP troppo grande"); out.write(buffer,0,n); }
                    }
                }
            }
            if (meta == null) throw new Exception("Manca addon.json: questo ZIP non usa il formato Kodi Auto Fermata");
            String id = meta.optString("id", "");
            String name = meta.optString("name", "");
            String version = meta.optString("version", "1.0");
            if (!id.matches("[a-zA-Z0-9._-]{2,80}") || name.trim().isEmpty()) throw new Exception("Manifest addon.json non valido");
            File destination = new File(getFilesDir(), "addons/" + id);
            if (destination.exists()) deleteTree(destination);
            File parent = destination.getParentFile(); if (parent != null) parent.mkdirs();
            if (!staging.renameTo(destination)) throw new Exception("Impossibile completare l'installazione");
            String label = name + " · " + version;
            runOnUiThread(() -> {
                Toast.makeText(this, "Pacchetto importato: " + label, Toast.LENGTH_LONG).show();
                showInstalled();
            });
        } catch (Exception e) {
            deleteTree(staging);
            runOnUiThread(() -> new AlertDialog.Builder(this).setTitle("ZIP non installato")
                    .setMessage(e.getMessage() == null ? "Pacchetto non valido" : e.getMessage())
                    .setPositiveButton("OK", null).show());
        }
    }

    private void showInstalled() {
        File dir = new File(getFilesDir(), "addons");
        List<String> lines = new ArrayList<>();
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) for (File f : files) {
                if (!f.isDirectory()) continue;
                try {
                    JSONObject j = new JSONObject(new String(java.nio.file.Files.readAllBytes(new File(f, "addon.json").toPath()), "UTF-8"));
                    lines.add(j.optString("name", f.getName()) + "  ·  " + j.optString("version", ""));
                } catch (Exception ignored) { lines.add(f.getName()); }
            }
        }
        String msg = lines.isEmpty() ? "Nessun pacchetto installato." : android.text.TextUtils.join("\n", lines);
        new AlertDialog.Builder(this).setTitle("I miei Add-on").setMessage(msg).setPositiveButton("OK", null).show();
    }

    private void manageRepositories() {
        var prefs = getSharedPreferences("addon_manager", MODE_PRIVATE);
        List<String> repos = new ArrayList<>(prefs.getStringSet("repositories", new java.util.HashSet<>()));
        String current = android.text.TextUtils.join("\n", repos);
        EditText input = new EditText(this);
        input.setHint("https://esempio.it/repository.json");
        input.setText(current);
        input.setMinLines(3);
        new AlertDialog.Builder(this).setTitle("Repository")
                .setMessage("Inserisci un indirizzo HTTPS per riga. In questa prima versione gli indirizzi vengono salvati; il catalogo remoto sarà attivato nel passaggio successivo.")
                .setView(input)
                .setNegativeButton("Annulla", null)
                .setPositiveButton("Salva", (d,w) -> {
                    java.util.Set<String> values = new java.util.HashSet<>();
                    for (String line : input.getText().toString().split("\\n")) {
                        String u = line.trim();
                        if (u.startsWith("https://")) values.add(u);
                    }
                    prefs.edit().putStringSet("repositories", values).apply();
                    Toast.makeText(this, "Repository salvati: " + values.size(), Toast.LENGTH_SHORT).show();
                }).show();
    }

    private void deleteTree(File f) {
        if (f == null || !f.exists()) return;
        File[] children = f.listFiles();
        if (children != null) for (File c : children) deleteTree(c);
        f.delete();
    }

    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }
    private LinearLayout.LayoutParams lp(int w, int h) { return new LinearLayout.LayoutParams(w, h); }
}
