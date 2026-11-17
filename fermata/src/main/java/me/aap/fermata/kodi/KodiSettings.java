package me.aap.fermata.kodi;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** Local Kodi connection settings. Passwords are encrypted with an Android Keystore key. */
public final class KodiSettings {
    public static final String HOST = KodiJsonRpcClient.DEFAULT_HOST;
    public static final int PORT = KodiJsonRpcClient.DEFAULT_PORT;

    private static final String PREFS = "kodi_connection";
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password_encrypted";
    private static final String KEY_ALIAS = "fermata_kodi_jsonrpc_password";
    private static final String KEYSTORE = "AndroidKeyStore";

    private KodiSettings() {}

    public static String getUsername(Context context) {
        return prefs(context).getString(USERNAME, "");
    }

    public static String getPassword(Context context) throws Exception {
        String encoded = prefs(context).getString(PASSWORD, null);
        if (encoded == null || encoded.isEmpty()) return "";
        byte[] packed = Base64.decode(encoded, Base64.NO_WRAP);
        ByteBuffer buffer = ByteBuffer.wrap(packed);
        int ivLength = buffer.getInt();
        if (ivLength < 12 || ivLength > 16 || packed.length <= 4 + ivLength) {
            throw new IOException("Credenziali Kodi non valide");
        }
        byte[] iv = new byte[ivLength];
        buffer.get(iv);
        byte[] encrypted = new byte[buffer.remaining()];
        buffer.get(encrypted);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, getKey(), new GCMParameterSpec(128, iv));
        return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
    }

    public static void save(Context context, String username, String password) throws Exception {
        SharedPreferences.Editor edit = prefs(context).edit();
        String user = (username == null) ? "" : username.trim();
        String pass = (password == null) ? "" : password;
        edit.putString(USERNAME, user);
        if (pass.isEmpty()) {
            edit.remove(PASSWORD);
        } else {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, getKey());
            byte[] iv = cipher.getIV();
            byte[] encrypted = cipher.doFinal(pass.getBytes(StandardCharsets.UTF_8));
            ByteBuffer packed = ByteBuffer.allocate(4 + iv.length + encrypted.length);
            packed.putInt(iv.length).put(iv).put(encrypted);
            edit.putString(PASSWORD, Base64.encodeToString(packed.array(), Base64.NO_WRAP));
        }
        edit.apply();
    }

    public static KodiJsonRpcClient createClient(Context context) throws Exception {
        String username = getUsername(context);
        String password = username.isEmpty() ? null : getPassword(context);
        return new KodiJsonRpcClient(HOST, PORT,
                username.isEmpty() ? null : username, password);
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static SecretKey getKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(KEYSTORE);
        keyStore.load(null);
        if (keyStore.containsAlias(KEY_ALIAS)) {
            return (SecretKey) keyStore.getKey(KEY_ALIAS, null);
        }
        KeyGenerator generator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES, KEYSTORE);
        generator.init(new KeyGenParameterSpec.Builder(KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build());
        return generator.generateKey();
    }
}
