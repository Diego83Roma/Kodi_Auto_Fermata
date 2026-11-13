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
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ImageView;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import me.aap.fermata.R;
import me.aap.fermata.addon.KodiRepositoryClient;
import me.aap.fermata.addon.KodiAddonPackageManager;

import org.json.JSONObject;
import org.json.JSONArray;

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
    private static final String DEFAULT_KODI_REPOSITORY = "https://mirrors.kodi.tv/addons/piers/addons.xml.gz";
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
        TextView brand = text("KODI AUTO FERMATA", 13, Color.rgb(89, 180, 255), true);
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

        addSection("CONTENUTI");
        addTile("TV in diretta", "Canali e playlist", "Apri la TV integrata di Fermata", () -> openFermataScreen(R.id.tv_fragment));
        addTile("Preferiti", "I tuoi contenuti salvati", "Apri i preferiti di Fermata", () -> openFermataScreen(R.id.favorites_fragment));
        addTile("Playlist", "Le tue raccolte personalizzate", "Apri le playlist di Fermata", () -> openFermataScreen(R.id.playlists_fragment));

        addSection("GESTIONE FILE");
        addTile("Sorgenti file", "Collegamenti a cartelle e archivi remoti", "Crea, modifica, apri o elimina una sorgente", this::showFileSources);

        addSection("ADD-ON E REPOSITORY");
        addTile("Installa da ZIP", "Aggiungi un pacchetto compatibile", "Seleziona un file ZIP dalla memoria", this::pickZip);
        addTile("I miei Add-on", "Pacchetti installati", "Visualizza gli add-on importati", this::showInstalled);
        addTile("Repository", "Sorgenti Kodi", "Aggiungi o rimuovi indirizzi repository", this::manageRepositories);
        addTile("Catalogo Kodi", "Ricerca add-on nei repository", "Aggiorna e cerca nel catalogo Kodi", this::browseKodiCatalog);

        addSection("WEB E VIDEO");
        addTile("Web Browser", "Navigazione web integrata", "Apri il browser di Fermata", () -> openBuiltInAddon(R.id.web_browser_fragment));
        addTile("YouTube", "Video e canali YouTube", "Apri YouTube integrato in Fermata", () -> openBuiltInAddon(R.id.youtube_fragment));

        addSection("APP E PREFERENZE");
        addTile("Impostazioni", "Interfaccia, compatibilità e gestione", "Apri le impostazioni di Kodi Auto Fermata", this::showSettings);

        TextView note = text("Kodi Auto Fermata · nuova interfaccia in sviluppo", 12, Color.rgb(126, 142, 164), false);
        LinearLayout.LayoutParams noteLp = lp(-1, -2); noteLp.topMargin = dp(22);
        root.addView(note, noteLp);
    }

    private void openBuiltInAddon(int fragmentId) {
        openFermataScreen(fragmentId);
    }

    private void openFermataScreen(int fragmentId) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("kodiauto.open_fragment", fragmentId);
        intent.putExtra("kodiauto.modern_host", true);
        startActivity(intent);
    }

    private void showSettings() {
        String[] sections = {"Interfaccia e dimensioni", "Adattamento automatico", "Riproduzione", "Add-on e repository", "Archiviazione", "Informazioni"};
        new AlertDialog.Builder(this).setTitle("Impostazioni Kodi Auto Fermata")
                .setItems(sections, (dialog, which) -> {
                    switch (which) {
                        case 0: showInterfaceSettings(); break;
                        case 1: showResizeInfo(); break;
                        case 2: showPlaybackSettings(); break;
                        case 3: showAddonSettings(); break;
                        case 4: showStorageInfo(); break;
                        default: showAbout(); break;
                    }
                }).setNegativeButton("Chiudi", null).show();
    }

    private void showInterfaceSettings() {
        final String[] sizes = {"Compatta", "Normale", "Grande"};
        int current = getSharedPreferences("ui_settings", MODE_PRIVATE).getInt("scale", 1);
        new AlertDialog.Builder(this).setTitle("Dimensione interfaccia")
                .setSingleChoiceItems(sizes, current, (d, which) -> {
                    getSharedPreferences("ui_settings", MODE_PRIVATE).edit().putInt("scale", which).apply();
                    d.dismiss();
                    Toast.makeText(this, "Dimensione salvata. Si applica alla prossima apertura.", Toast.LENGTH_SHORT).show();
                }).setNegativeButton("Annulla", null).show();
    }

    private void showResizeInfo() {
        new AlertDialog.Builder(this).setTitle("Adattamento automatico")
                .setMessage("Attivo: la schermata usa la larghezza disponibile, limita la larghezza massima sui display grandi e si adatta a orientamento e dimensioni dello schermo. L'attività supporta il ridimensionamento Android.")
                .setPositiveButton("OK", null).show();
    }

    private void showPlaybackSettings() {
        new AlertDialog.Builder(this).setTitle("Riproduzione")
                .setMessage("Le impostazioni del motore di riproduzione restano quelle gestite da Fermata. Le opzioni avanzate saranno esposte qui quando collegate direttamente al player, senza controlli fittizi.")
                .setPositiveButton("Apri riproduzione", (d, w) -> openFermataScreen(R.id.folders_fragment))
                .setNegativeButton("Chiudi", null).show();
    }

    private void showAddonSettings() {
        new AlertDialog.Builder(this).setTitle("Add-on e repository")
                .setItems(new String[]{"I miei Add-on", "Gestisci Repository", "Installa da ZIP"}, (d, which) -> {
                    if (which == 0) showInstalled();
                    else if (which == 1) manageRepositories();
                    else pickZip();
                }).setNegativeButton("Chiudi", null).show();
    }

    private void showStorageInfo() {
        File dir = new File(getFilesDir(), "addons");
        String value = android.text.format.Formatter.formatShortFileSize(this, directorySize(dir));
        new AlertDialog.Builder(this).setTitle("Archiviazione")
                .setMessage("Spazio occupato dai pacchetti Add-on importati: " + value + "\nI dati e la cache di Fermata sono gestiti separatamente.")
                .setPositiveButton("OK", null).show();
    }

    private long directorySize(File file) {
        if (file == null || !file.exists()) return 0;
        if (file.isFile()) return file.length();
        long size = 0;
        File[] children = file.listFiles();
        if (children != null) for (File child : children) size += directorySize(child);
        return size;
    }

    private void showAbout() {
        new AlertDialog.Builder(this).setTitle("Kodi Auto Fermata")
                .setMessage("Interfaccia moderna in sviluppo\nVersione di prova")
                .setPositiveButton("OK", null).show();
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

    private File getKodiPackagesDirectory() {
        File dir = new File(getFilesDir(), "kodi-packages");
        if (!dir.isDirectory()) dir.mkdirs();
        return dir;
    }

    private void collectZipFiles(File directory, List<File> output) {
        File[] entries = directory.listFiles();
        if (entries == null) return;
        for (File entry : entries) {
            if (entry.isDirectory()) collectZipFiles(entry, output);
            else if (entry.isFile() && entry.getName().toLowerCase(java.util.Locale.ROOT).endsWith(".zip")) {
                output.add(entry);
            }
        }
    }

    private void pickZip() {
        File packageDir = getKodiPackagesDirectory();
        List<File> archives = new ArrayList<>();
        collectZipFiles(packageDir, archives);
        java.util.Collections.sort(archives, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));

        List<String> labels = new ArrayList<>();
        for (File archive : archives) {
            String relative = archive.getAbsolutePath().substring(packageDir.getAbsolutePath().length());
            labels.add(archive.getName() + "  ·  " +
                    android.text.format.Formatter.formatShortFileSize(this, archive.length()) + relative);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, labels);
        ListView list = new ListView(this);
        list.setAdapter(adapter);
        TextView empty = text("Nessun pacchetto ZIP scaricato. Puoi importare un file dal telefono: verrà copiato qui.", 14,
                Color.rgb(170, 185, 205), false);
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(12), dp(8), dp(12), dp(8));
        if (archives.isEmpty()) panel.addView(empty, lp(-1, -2));
        else panel.addView(list, lp(-1, dp(360)));

        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Pacchetti ZIP scaricati")
                .setView(panel)
                .setPositiveButton("Importa dal telefono", (d, w) -> pickZipFromPhone())
                .setNegativeButton("Chiudi", null).create();
        list.setOnItemClickListener((parent, view, position, id) -> {
            File selected = archives.get(position);
            dialog.dismiss();
            new Thread(() -> importZip(Uri.fromFile(selected))).start();
        });
        dialog.show();
    }

    private void pickZipFromPhone() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        i.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                "application/zip", "application/x-zip", "application/x-zip-compressed",
                "application/octet-stream", "application/x-compressed"
        });
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(i, PICK_ADDON_ZIP);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK_ADDON_ZIP || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        try {
            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Exception ignored) { }
        new Thread(() -> {
            File imported = new File(getKodiPackagesDirectory(), "imported-" + System.currentTimeMillis() + ".zip");
            try (InputStream in = getContentResolver().openInputStream(uri);
                 FileOutputStream out = new FileOutputStream(imported)) {
                if (in == null) throw new Exception("Impossibile leggere il file selezionato");
                byte[] buffer = new byte[16384];
                int count;
                long total = 0;
                while ((count = in.read(buffer)) != -1) {
                    total += count;
                    if (total > 100L * 1024 * 1024) throw new Exception("ZIP oltre il limite di 100 MB");
                    out.write(buffer, 0, count);
                }
                out.getFD().sync();
                importZip(Uri.fromFile(imported));
            } catch (Exception e) {
                if (imported.exists()) imported.delete();
                runOnUiThread(() -> new AlertDialog.Builder(this).setTitle("Importazione non riuscita")
                        .setMessage(e.getMessage() == null ? "Impossibile copiare il file ZIP" : e.getMessage())
                        .setPositiveButton("OK", null).show());
            }
        }).start();
    }

    private void importZip(Uri uri) {
        File staging = new File(getFilesDir(), "addons/import-" + System.currentTimeMillis());
        try {
            if (!staging.mkdirs() && !staging.isDirectory()) throw new Exception("Cartella temporanea non disponibile");
            JSONObject meta = null;
            byte[] kodiXml = null;
            int files = 0;
            long total = 0;
            try (InputStream raw = new BufferedInputStream(getContentResolver().openInputStream(uri));
                 ZipInputStream zip = new ZipInputStream(raw)) {
                if (raw == null) throw new Exception("Impossibile leggere il file selezionato");
                ZipEntry entry;
                byte[] buffer = new byte[8192];
                while ((entry = zip.getNextEntry()) != null) {
                    if (++files > 4000) throw new Exception("Troppi file nel pacchetto");
                    String name = entry.getName().replace('\\', '/');
                    if (name.startsWith("/") || name.contains("../") || name.equals("..")
                            || name.contains("/..") || name.matches("^[A-Za-z]:.*")) {
                        throw new Exception("Percorso ZIP non valido");
                    }
                    if (entry.isDirectory()) continue;
                    File target = new File(staging, name);
                    if (!target.getCanonicalPath().startsWith(staging.getCanonicalPath() + File.separator)) {
                        throw new Exception("Percorso ZIP non sicuro");
                    }
                    File parent = target.getParentFile();
                    if (parent != null && !parent.mkdirs() && !parent.isDirectory()) {
                        throw new Exception("Impossibile creare una cartella del pacchetto");
                    }
                    boolean isJson = name.equalsIgnoreCase("addon.json") || name.toLowerCase(java.util.Locale.ROOT).endsWith("/addon.json");
                    boolean isXml = name.equalsIgnoreCase("addon.xml") || name.toLowerCase(java.util.Locale.ROOT).endsWith("/addon.xml");
                    ByteArrayOutputStream manifest = (isJson || isXml) ? new ByteArrayOutputStream() : null;
                    try (FileOutputStream out = new FileOutputStream(target)) {
                        int n;
                        while ((n = zip.read(buffer)) != -1) {
                            if (n == 0) continue;
                            total += n;
                            if (total > 100L * 1024 * 1024) throw new Exception("ZIP oltre il limite di 100 MB");
                            out.write(buffer, 0, n);
                            if (manifest != null) {
                                if (manifest.size() + n > 1024 * 1024) throw new Exception("Manifest add-on troppo grande");
                                manifest.write(buffer, 0, n);
                            }
                        }
                    }
                    if (isJson && meta == null) meta = new JSONObject(manifest.toString("UTF-8"));
                    if (isXml && kodiXml == null) kodiXml = manifest.toByteArray();
                }
            }
            // Supporta sia il formato JSON interno sia il formato standard dei pacchetti Kodi.
            if (meta == null && kodiXml != null) {
                org.xmlpull.v1.XmlPullParser parser = android.util.Xml.newPullParser();
                parser.setInput(new java.io.ByteArrayInputStream(kodiXml), "UTF-8");
                int event = parser.getEventType();
                while (event != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
                    if (event == org.xmlpull.v1.XmlPullParser.START_TAG && "addon".equals(parser.getName())) {
                        String id = parser.getAttributeValue(null, "id");
                        String version = parser.getAttributeValue(null, "version");
                        String name = parser.getAttributeValue(null, "name");
                        String provider = parser.getAttributeValue(null, "provider-name");
                        meta = new JSONObject();
                        meta.put("manifestVersion", 1);
                        meta.put("id", id == null ? "" : id);
                        meta.put("name", (name == null || name.trim().isEmpty()) ? id : name);
                        meta.put("version", version == null ? "1.0" : version);
                        meta.put("provider", provider == null ? "" : provider);
                        break;
                    }
                    event = parser.next();
                }
            }
            if (meta == null) throw new Exception("Nel file non trovo addon.xml (Kodi) o addon.json. Verifica che sia un pacchetto ZIP di un add-on.");
            if (meta.optInt("manifestVersion", 1) != 1) throw new Exception("Versione del manifest add-on non supportata");
            String id = meta.optString("id", "");
            String name = meta.optString("name", id);
            String version = meta.optString("version", "1.0");
            if (!id.matches("[a-zA-Z0-9._-]{2,100}") || name.trim().isEmpty()) {
                throw new Exception("Manifest add-on non valido: ID o nome mancante");
            }
            // Conserva un riepilogo uniforme per la schermata I miei Add-on.
            try (FileOutputStream manifestOut = new FileOutputStream(new File(staging, "addon.json"))) {
                manifestOut.write(meta.toString(2).getBytes("UTF-8"));
            }
            File destination = new File(getFilesDir(), "addons/" + id);
            File parent = destination.getParentFile();
            if (parent != null && !parent.mkdirs() && !parent.isDirectory()) throw new Exception("Cartella add-on non disponibile");
            File backup = new File(parent, id + ".previous");
            if (backup.exists()) deleteTree(backup);
            if (destination.exists() && !destination.renameTo(backup)) throw new Exception("Impossibile aggiornare il pacchetto già presente");
            if (!staging.renameTo(destination)) {
                if (backup.exists()) backup.renameTo(destination);
                throw new Exception("Impossibile completare l'importazione");
            }
            if (backup.exists()) deleteTree(backup);
            String label = name + " · " + version;
            runOnUiThread(() -> {
                Toast.makeText(this, "Pacchetto riconosciuto: " + label, Toast.LENGTH_LONG).show();
                showInstalled();
            });
        } catch (Exception e) {
            deleteTree(staging);
            runOnUiThread(() -> new AlertDialog.Builder(this).setTitle("ZIP non importato")
                    .setMessage(e.getMessage() == null ? "Archivio ZIP non valido o non leggibile" : e.getMessage())
                    .setPositiveButton("OK", null).show());
        }
    }

    private void showInstalled() {
        File dir = new File(getFilesDir(), "addons");
        if (!dir.isDirectory()) dir.mkdirs();
        List<File> packages = new ArrayList<>();
        File[] entries = dir.listFiles();
        if (entries != null) {
            for (File entry : entries) {
                if (entry.isDirectory() && new File(entry, "addon.json").isFile()) packages.add(entry);
            }
        }
        packages.sort((a, b) -> {
            try {
                JSONObject ja = readAddonMetadata(a);
                JSONObject jb = readAddonMetadata(b);
                return ja.optString("name", a.getName()).compareToIgnoreCase(jb.optString("name", b.getName()));
            } catch (Exception ignored) { return a.getName().compareToIgnoreCase(b.getName()); }
        });

        int screenWidthDp = (int) (getResources().getDisplayMetrics().widthPixels /
                getResources().getDisplayMetrics().density);
        int columns = screenWidthDp >= 900 ? 4 : screenWidthDp >= 600 ? 3 : 2;
        int availableWidth = Math.min(dp(980), getResources().getDisplayMetrics().widthPixels - dp(40));
        int cardWidth = Math.max(dp(132), (availableWidth - dp(12) * (columns - 1)) / columns);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(14), dp(8), dp(14), dp(8));
        TextView count = text(packages.size() + " ADD-ON IMPORTATI", 12, Color.rgb(143, 205, 255), true);
        count.setLetterSpacing(0.10f);
        content.addView(count, lp(-1, -2));

        if (packages.isEmpty()) {
            TextView empty = text("Non hai ancora importato pacchetti. Scarica un add-on dal Catalogo Kodi oppure usa Installa da ZIP.", 15,
                    Color.rgb(174, 188, 208), false);
            LinearLayout.LayoutParams emptyLp = lp(-1, -2);
            emptyLp.topMargin = dp(18);
            content.addView(empty, emptyLp);
        } else {
            LinearLayout row = null;
            for (int i = 0; i < packages.size(); i++) {
                if (i % columns == 0) {
                    row = new LinearLayout(this);
                    row.setOrientation(LinearLayout.HORIZONTAL);
                    LinearLayout.LayoutParams rowLp = lp(-1, -2);
                    rowLp.topMargin = dp(12);
                    content.addView(row, rowLp);
                }
                File folder = packages.get(i);
                JSONObject meta = readAddonMetadata(folder);
                String name = meta.optString("name", folder.getName());
                String version = meta.optString("version", "");
                LinearLayout card = new LinearLayout(this);
                card.setOrientation(LinearLayout.VERTICAL);
                card.setGravity(Gravity.CENTER_HORIZONTAL);
                card.setPadding(dp(10), dp(12), dp(10), dp(12));
                card.setBackground(shape(PANEL, 16));

                ImageView icon = new ImageView(this);
                icon.setScaleType(ImageView.ScaleType.CENTER_CROP);
                File iconFile = findAddonIcon(folder, 0);
                if (iconFile != null) {
                    android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeFile(iconFile.getAbsolutePath());
                    if (bitmap != null) icon.setImageBitmap(bitmap);
                }
                if (icon.getDrawable() == null) {
                    icon.setImageResource(android.R.drawable.ic_menu_view);
                    icon.setColorFilter(Color.rgb(143, 205, 255));
                    icon.setBackground(shape(Color.rgb(31, 62, 91), 14));
                    icon.setPadding(dp(24), dp(24), dp(24), dp(24));
                }
                card.addView(icon, lp(dp(104), dp(104)));

                TextView title = text(name, 14, Color.WHITE, true);
                title.setGravity(Gravity.CENTER);
                title.setMaxLines(2);
                LinearLayout.LayoutParams titleLp = lp(-1, -2);
                titleLp.topMargin = dp(9);
                card.addView(title, titleLp);

                TextView ver = text(version.isEmpty() ? "Pacchetto importato" : "Versione " + version,
                        11, Color.rgb(160, 179, 202), false);
                ver.setGravity(Gravity.CENTER);
                LinearLayout.LayoutParams verLp = lp(-1, -2);
                verLp.topMargin = dp(4);
                card.addView(ver, verLp);

                LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(cardWidth, dp(190));
                if (i % columns != 0) cardLp.leftMargin = dp(12);
                row.addView(card, cardLp);
                card.setOnClickListener(v -> showAddonDetails(folder));
            }
        }

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(content, new ScrollView.LayoutParams(-1, -2));
        new AlertDialog.Builder(this).setTitle("I miei Add-on")
                .setView(scroll).setPositiveButton("Chiudi", null).show();
    }

    private JSONObject readAddonMetadata(File folder) {
        try {
            File metadata = new File(folder, "addon.json");
            byte[] bytes = java.nio.file.Files.readAllBytes(metadata.toPath());
            return new JSONObject(new String(bytes, "UTF-8"));
        } catch (Exception ignored) {
            JSONObject fallback = new JSONObject();
            try { fallback.put("id", folder.getName()); fallback.put("name", folder.getName()); }
            catch (Exception ignoredAgain) { }
            return fallback;
        }
    }

    private File findAddonIcon(File folder, int depth) {
        if (folder == null || depth > 5) return null;
        File[] files = folder.listFiles();
        if (files == null) return null;
        for (File file : files) {
            if (!file.isFile()) continue;
            String name = file.getName().toLowerCase(java.util.Locale.ROOT);
            if (name.equals("icon.png") || name.equals("addon.png") || name.equals("logo.png")) return file;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                File found = findAddonIcon(file, depth + 1);
                if (found != null) return found;
            }
        }
        return null;
    }

    private void showAddonDetails(File folder) {
        JSONObject meta = readAddonMetadata(folder);
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(20), dp(12), dp(20), dp(8));
        ImageView icon = new ImageView(this);
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        File iconFile = findAddonIcon(folder, 0);
        if (iconFile != null) {
            android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeFile(iconFile.getAbsolutePath());
            if (bitmap != null) icon.setImageBitmap(bitmap);
        }
        if (icon.getDrawable() == null) icon.setImageResource(android.R.drawable.ic_menu_view);
        LinearLayout.LayoutParams iconLp = lp(dp(112), dp(112));
        iconLp.gravity = Gravity.CENTER_HORIZONTAL;
        panel.addView(icon, iconLp);

        String details = "ID: " + meta.optString("id", folder.getName()) +
                "\nVersione: " + meta.optString("version", "—") +
                "\nAutore: " + meta.optString("provider", "—") +
                "\n\nPacchetto ZIP importato correttamente. La visualizzazione e la gestione sono disponibili; l'esecuzione degli add-on Kodi richiede ancora il relativo motore compatibile.";
        TextView info = text(details, 14, Color.rgb(190, 203, 220), false);
        LinearLayout.LayoutParams infoLp = lp(-1, -2);
        infoLp.topMargin = dp(14);
        panel.addView(info, infoLp);
        new AlertDialog.Builder(this).setTitle(meta.optString("name", folder.getName()))
                .setView(panel).setPositiveButton("Chiudi", null).show();
    }


    private JSONArray readFileSources() {
        String raw = getSharedPreferences("kodi_file_sources", MODE_PRIVATE).getString("sources", "[]");
        try { return new JSONArray(raw); } catch (Exception ignored) { return new JSONArray(); }
    }

    private void saveFileSources(JSONArray sources) {
        getSharedPreferences("kodi_file_sources", MODE_PRIVATE).edit()
                .putString("sources", sources.toString()).apply();
    }

    private void showFileSources() {
        JSONArray sources = readFileSources();
        List<String> labels = new ArrayList<>();
        for (int i = 0; i < sources.length(); i++) {
            JSONObject item = sources.optJSONObject(i);
            if (item != null) labels.add(item.optString("name", "Sorgente") + "\n" + item.optString("url", ""));
        }
        ListView list = new ListView(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, labels);
        list.setAdapter(adapter);
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(12), dp(8), dp(12), dp(8));
        Button add = button("＋ Aggiungi sorgente", () -> editFileSource(-1));
        panel.addView(add, lp(-1, -2));
        panel.addView(list, lp(-1, dp(320)));
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Gestione file")
                .setView(panel).setNegativeButton("Chiudi", null).create();
        list.setOnItemClickListener((parent, view, position, id) -> {
            JSONObject item = readFileSources().optJSONObject(position);
            if (item == null) return;
            String name = item.optString("name", "Sorgente");
            String url = item.optString("url", "");
            new AlertDialog.Builder(this).setTitle(name)
                    .setItems(new String[]{"Apri sorgente", "Modifica", "Elimina"}, (d, which) -> {
                        if (which == 0) {
                            try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }
                            catch (Exception e) { Toast.makeText(this, "Impossibile aprire la sorgente", Toast.LENGTH_SHORT).show(); }
                        } else if (which == 1) editFileSource(position);
                        else new AlertDialog.Builder(this).setTitle("Eliminare la sorgente?")
                                .setMessage(name).setNegativeButton("Annulla", null)
                                .setPositiveButton("Elimina", (dd, ww) -> {
                                    JSONArray updated = readFileSources();
                                    JSONArray kept = new JSONArray();
                                    for (int i = 0; i < updated.length(); i++) if (i != position) kept.put(updated.opt(i));
                                    saveFileSources(kept);
                                    showFileSources();
                                }).show();
                    }).show();
        });
        dialog.show();
    }

    private void editFileSource(int index) {
        JSONArray sources = readFileSources();
        JSONObject old = index >= 0 ? sources.optJSONObject(index) : null;
        EditText name = new EditText(this);
        name.setSingleLine(true);
        name.setHint("Nome sorgente");
        EditText url = new EditText(this);
        url.setSingleLine(true);
        url.setHint("https://server/percorso/cartella/");
        if (old != null) {
            name.setText(old.optString("name", ""));
            url.setText(old.optString("url", ""));
        }
        LinearLayout fields = new LinearLayout(this);
        fields.setOrientation(LinearLayout.VERTICAL);
        fields.setPadding(dp(18), dp(8), dp(18), dp(8));
        fields.addView(name, lp(-1, -2));
        fields.addView(url, lp(-1, -2));
        new AlertDialog.Builder(this).setTitle(index < 0 ? "Aggiungi sorgente" : "Modifica sorgente")
                .setView(fields).setNegativeButton("Annulla", null)
                .setPositiveButton("Salva", (d, w) -> {
                    String n = name.getText().toString().trim();
                    String u = url.getText().toString().trim();
                    Uri parsed = Uri.parse(u);
                    if (n.isEmpty() || !("https".equalsIgnoreCase(parsed.getScheme())
                            || "http".equalsIgnoreCase(parsed.getScheme())) || parsed.getHost() == null) {
                        new AlertDialog.Builder(this).setMessage("Inserisci un nome e un indirizzo HTTP/HTTPS valido.")
                                .setPositiveButton("OK", null).show();
                        return;
                    }
                    JSONArray updated = readFileSources();
                    JSONObject value = new JSONObject();
                    try { value.put("name", n); value.put("url", u); } catch (Exception ignored) { }
                    if (index < 0) updated.put(value);
                    else {
                        JSONArray replaced = new JSONArray();
                        for (int i = 0; i < updated.length(); i++) replaced.put(i == index ? value : updated.opt(i));
                        updated = replaced;
                    }
                    saveFileSources(updated);
                    Toast.makeText(this, "Sorgente salvata", Toast.LENGTH_SHORT).show();
                    showFileSources();
                }).show();
    }

    private void manageRepositories() {
        var prefs = getSharedPreferences("addon_manager", MODE_PRIVATE);
        List<String> repos = new ArrayList<>(prefs.getStringSet("repositories", new java.util.HashSet<>()));
        if (repos.isEmpty()) repos.add(DEFAULT_KODI_REPOSITORY);
        String current = android.text.TextUtils.join("\n", repos);
        EditText input = new EditText(this);
        input.setHint("https://host/percorso/addons.xml.gz");
        input.setText(current);
        input.setMinLines(3);
        new AlertDialog.Builder(this).setTitle("Repository Kodi")
                .setMessage("Inserisci un indirizzo HTTPS per riga. Puoi indicare direttamente addons.xml / addons.xml.gz oppure la cartella che contiene il catalogo. Il catalogo verrà scaricato e indicizzato.")
                .setView(input)
                .setNegativeButton("Annulla", null)
                .setNeutralButton("Cerca", (d, w) -> {
                    saveRepositories(input.getText().toString());
                    browseKodiCatalog();
                })
                .setPositiveButton("Salva", (d,w) -> saveRepositories(input.getText().toString()))
                .show();
    }

    private void saveRepositories(String raw) {
        java.util.Set<String> values = new java.util.HashSet<>();
        for (String line : raw.split("\\n")) {
            String u = line.trim();
            if (u.startsWith("https://")) values.add(u);
        }
        getSharedPreferences("addon_manager", MODE_PRIVATE).edit()
                .putStringSet("repositories", values).apply();
        Toast.makeText(this, "Repository salvati: " + values.size(), Toast.LENGTH_SHORT).show();
    }

    private void browseKodiCatalog() {
        java.util.Set<String> saved = getSharedPreferences("addon_manager", MODE_PRIVATE)
                .getStringSet("repositories", new java.util.HashSet<>());
        List<String> repositories = new ArrayList<>(saved);
        if (repositories.isEmpty()) {
            repositories.add(DEFAULT_KODI_REPOSITORY);
            getSharedPreferences("addon_manager", MODE_PRIVATE).edit()
                    .putStringSet("repositories", new java.util.HashSet<>(repositories)).apply();
        }
        Toast.makeText(this, "Aggiornamento cataloghi Kodi…", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            List<KodiRepositoryClient.Addon> all = new ArrayList<>();
            List<String> errors = new ArrayList<>();
            for (String repository : repositories) {
                try {
                    all.addAll(KodiRepositoryClient.load(repository));
                } catch (Exception e) {
                    errors.add(repository + ": " + (e.getMessage() == null ? "errore di lettura" : e.getMessage()));
                }
            }
            java.util.Map<String, KodiRepositoryClient.Addon> unique = new java.util.LinkedHashMap<>();
            for (KodiRepositoryClient.Addon addon : all) unique.put(addon.id, addon);
            List<KodiRepositoryClient.Addon> catalog = new ArrayList<>(unique.values());
            catalog.sort((a, b) -> a.name.compareToIgnoreCase(b.name));
            runOnUiThread(() -> {
                if (catalog.isEmpty()) {
                    String message = errors.isEmpty() ? "I repository non contengono add-on leggibili." :
                            android.text.TextUtils.join("\n\n", errors);
                    new AlertDialog.Builder(this).setTitle("Catalogo non disponibile")
                            .setMessage(message).setPositiveButton("OK", null).show();
                } else {
                    showKodiSearch(catalog, errors);
                }
            });
        }).start();
    }

    private void showKodiSearch(List<KodiRepositoryClient.Addon> catalog, List<String> errors) {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(18), dp(8), dp(18), dp(8));
        EditText search = new EditText(this);
        search.setSingleLine(true);
        search.setHint("Cerca per nome, ID o categoria…");
        content.addView(search, lp(-1, -2));
        TextView count = text(catalog.size() + " add-on trovati", 12, Color.rgb(150, 170, 195), false);
        LinearLayout.LayoutParams countLp = lp(-1, -2);
        countLp.topMargin = dp(6);
        content.addView(count, countLp);
        ListView list = new ListView(this);
        LinearLayout.LayoutParams listLp = lp(-1, dp(420));
        listLp.topMargin = dp(8);
        content.addView(list, listLp);
        ArrayAdapter<KodiRepositoryClient.Addon> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_list_item_1, new ArrayList<>(catalog));
        list.setAdapter(adapter);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Catalogo Kodi")
                .setView(content).setNegativeButton("Chiudi", null).create();
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int countValue) {
                String query = s.toString().trim().toLowerCase(java.util.Locale.ROOT);
                List<KodiRepositoryClient.Addon> filtered = new ArrayList<>();
                for (KodiRepositoryClient.Addon addon : catalog) {
                    if (addon.name.toLowerCase(java.util.Locale.ROOT).contains(query)
                            || addon.id.toLowerCase(java.util.Locale.ROOT).contains(query)
                            || addon.type.toLowerCase(java.util.Locale.ROOT).contains(query)
                            || addon.summary.toLowerCase(java.util.Locale.ROOT).contains(query)) {
                        filtered.add(addon);
                    }
                }
                adapter.clear();
                adapter.addAll(filtered);
                count.setText(filtered.size() + " risultati · " + catalog.size() + " add-on indicizzati");
            }
            @Override public void afterTextChanged(Editable s) { }
        });
        list.setOnItemClickListener((parent, view, position, id) -> {
            KodiRepositoryClient.Addon addon = adapter.getItem(position);
            if (addon == null) return;
            String details = "ID: " + addon.id + "\nVersione: " + addon.version
                    + "\nTipo: " + addon.type + "\nAutore: " + addon.provider
                    + "\n\n" + (addon.summary.isEmpty() ? "" : addon.summary + "\n\n")
                    + addon.description
                    + "\n\nPacchetto trovato nel repository. L'esecuzione degli add-on Kodi richiede ancora il runtime Kodi e non è attiva in questa versione.";
            new AlertDialog.Builder(this).setTitle(addon.name).setMessage(details)
                    .setPositiveButton("Scarica e verifica ZIP", (d, w) -> downloadKodiAddon(addon))
                    .setNegativeButton("Chiudi", null).show();
        });
        dialog.show();
        if (!errors.isEmpty()) {
            Toast.makeText(this, "Catalogo parziale: " + errors.size() + " repository non letti", Toast.LENGTH_LONG).show();
        }
    }

    private void downloadKodiAddon(KodiRepositoryClient.Addon addon) {
        Toast.makeText(this, "Download e verifica pacchetto Kodi…", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                KodiAddonPackageManager.Result result =
                        KodiAddonPackageManager.downloadAndValidate(getApplicationContext(), addon);
                StringBuilder message = new StringBuilder();
                message.append("Pacchetto scaricato e manifest Kodi verificato.\n");
                message.append("ID: ").append(result.id).append("\n");
                message.append("Versione nel pacchetto: ").append(result.version).append("\n");
                message.append("Dimensione: ").append(result.archive.length() / 1024).append(" KB\n\n");
                if (result.dependencies.isEmpty()) {
                    message.append("Dipendenze dichiarate: nessuna.");
                } else {
                    message.append("Dipendenze dichiarate (").append(result.dependencies.size()).append("):\n");
                    for (KodiAddonPackageManager.Dependency dependency : result.dependencies) {
                        message.append("• ").append(dependency.toString()).append("\n");
                    }
                }
                message.append("\nIl pacchetto è conservato nell'area privata dell'app. Non è ancora installato né eseguito: prima va preparato il runtime compatibile.");
                String report = message.toString();
                runOnUiThread(() -> new AlertDialog.Builder(this)
                        .setTitle("Pacchetto verificato")
                        .setMessage(report)
                        .setPositiveButton("OK", null).show());
            } catch (Exception e) {
                String error = e.getMessage() == null ? "Errore durante il download o la verifica" : e.getMessage();
                runOnUiThread(() -> new AlertDialog.Builder(this)
                        .setTitle("Download non completato")
                        .setMessage(error)
                        .setPositiveButton("OK", null).show());
            }
        }).start();
    }

    private void deleteTree(File f) {
        if (f == null || !f.exists()) return;
        File[] children = f.listFiles();
        if (children != null) for (File c : children) deleteTree(c);
        f.delete();
    }

    private int dp(int v) {
        int scale = getSharedPreferences("ui_settings", MODE_PRIVATE).getInt("scale", 1);
        float factor = scale == 0 ? 0.90f : (scale == 2 ? 1.12f : 1.0f);
        return (int)(v * getResources().getDisplayMetrics().density * factor + 0.5f);
    }
    private LinearLayout.LayoutParams lp(int w, int h) { return new LinearLayout.LayoutParams(w, h); }
}
