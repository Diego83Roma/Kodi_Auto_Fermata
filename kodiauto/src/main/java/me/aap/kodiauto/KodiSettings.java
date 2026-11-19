package me.aap.kodiauto;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** Kodi connection preferences; password encrypted with Android Keystore AES/GCM. */
public final class KodiSettings {
    private static final String PREFS = "kodi_connection";
    private static final String USER = "username";
    private static final String PASS = "password";
    private static final String KEY_ALIAS = "kodiauto_password_key";

    private KodiSettings() {}

    public static void save(Context context, String user, String pass) throws Exception {
        SharedPreferences.Editor e = prefs(context).edit();
        String u = user == null ? "" : user.trim();
        String p = pass == null ? "" : pass;
        e.putString(USER, u);
        if (p.isEmpty()) {
            e.remove(PASS);
        } else {
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, key());
            byte[] iv = c.getIV();
            byte[] encrypted = c.doFinal(p.getBytes(StandardCharsets.UTF_8));
            ByteBuffer b = ByteBuffer.allocate(4 + iv.length + encrypted.length);
            b.putInt(iv.length).put(iv).put(encrypted);
            e.putString(PASS, Base64.encodeToString(b.array(), Base64.NO_WRAP));
        }
        e.apply();
    }

    public static String username(Context context) {
        return prefs(context).getString(USER, "");
    }

    public static String password(Context context) throws Exception {
        String encoded = prefs(context).getString(PASS, "");
        if (encoded.isEmpty()) return "";
        byte[] packed = Base64.decode(encoded, Base64.NO_WRAP);
        ByteBuffer b = ByteBuffer.wrap(packed);
        int n = b.getInt();
        if (n < 12 || n > 16 || packed.length <= 4 + n) {
            throw new IllegalStateException("Credenziali Kodi non valide");
        }
        byte[] iv = new byte[n];
        b.get(iv);
        byte[] encrypted = new byte[b.remaining()];
        b.get(encrypted);
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, iv));
        return new String(c.doFinal(encrypted), StandardCharsets.UTF_8);
    }

    public static KodiClient createClient(Context context) {
        try {
            String user = username(context);
            return new KodiClient("127.0.0.1", 8080, user, user.isEmpty() ? "" : password(context));
        } catch (Exception e) {
            throw new IllegalStateException("Impossibile leggere le credenziali Kodi", e);
        }
    }

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static SecretKey key() throws Exception {
        KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
        ks.load(null);
        if (ks.containsAlias(KEY_ALIAS)) return (SecretKey) ks.getKey(KEY_ALIAS, null);
        KeyGenerator g = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        g.init(new KeyGenParameterSpec.Builder(KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build());
        return g.generateKey();
    }
}