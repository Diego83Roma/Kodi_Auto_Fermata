package me.aap.kodiauto;

import android.app.Activity;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Small setup screen so Kodi credentials do not need to be hardcoded in the APK. */
public class KodiSetupActivity extends Activity {
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private EditText username;
    private EditText password;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);
        root.setLayoutParams(new ViewGroup.LayoutParams(-1, -1));

        TextView heading = new TextView(this);
        heading.setText("Kodi Auto");
        heading.setTextSize(24);
        root.addView(heading);

        TextView address = new TextView(this);
        address.setText("Connessione locale: 127.0.0.1:8080\nAttiva in Kodi il controllo remoto HTTP.");
        root.addView(address);

        username = new EditText(this);
        username.setSingleLine(true);
        username.setHint("Nome utente Kodi (vuoto se non richiesto)");
        username.setText(KodiSettings.username(this));
        root.addView(username);

        password = new EditText(this);
        password.setSingleLine(true);
        password.setHint("Password Kodi");
        password.setInputType(129);
        root.addView(password);

        Button save = new Button(this);
        save.setText("Salva credenziali");
        root.addView(save);
        save.setOnClickListener(v -> {
            try {
                KodiSettings.save(this, username.getText().toString(), password.getText().toString());
                password.setText("");
                Toast.makeText(this, "Credenziali salvate", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Errore salvataggio: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        Button test = new Button(this);
        test.setText("Verifica collegamento Kodi");
        root.addView(test);
        test.setOnClickListener(v -> {
            try {
                KodiSettings.save(this, username.getText().toString(), password.getText().toString());
                password.setText("");
            } catch (Exception e) {
                Toast.makeText(this, "Errore credenziali: " + e.getMessage(), Toast.LENGTH_LONG).show();
                return;
            }
            test.setEnabled(false);
            io.execute(() -> {
                String message;
                try {
                    KodiSettings.createClient(this).call("JSONRPC.Version", null);
                    message = "Collegamento Kodi riuscito";
                } catch (Exception e) {
                    message = "Connessione non riuscita: " + e.getMessage();
                }
                final String result = message;
                runOnUiThread(() -> {
                    test.setEnabled(true);
                    Toast.makeText(this, result, Toast.LENGTH_LONG).show();
                });
            });
        });
        setContentView(root);
    }

    @Override
    protected void onDestroy() {
        io.shutdownNow();
        super.onDestroy();
    }
}