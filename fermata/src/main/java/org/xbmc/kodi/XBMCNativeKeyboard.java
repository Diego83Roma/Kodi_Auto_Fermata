package org.xbmc.kodi;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

/** Native keyboard bridge used by Kodi's Android platform layer. */
public final class XBMCNativeKeyboard {
    private native void _onTextChanged(String text);
    private native void _onInputFinished(String text);
    private native void _onInputCanceled();
    private AlertDialog dialog;
    private EditText editText;
    private boolean callbackSent = true;

    public XBMCNativeKeyboard() {}

    @SuppressWarnings("deprecation")
    public void show(final String heading, final String initialText, final boolean hiddenInput) {
        Main.MainActivity.runOnUiThread(new Runnable() {
            @Override public void run() {
                callbackSent = false;
                editText = new EditText(Main.MainActivity);
                editText.setSingleLine(true);
                editText.setImeOptions(EditorInfo.IME_ACTION_DONE);
                editText.setInputType(hiddenInput
                    ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD
                    : InputType.TYPE_CLASS_TEXT);
                editText.setText(initialText);
                editText.setSelection(editText.getText().length());
                editText.addTextChangedListener(new TextWatcher() {
                    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                    @Override public void afterTextChanged(Editable s) { _onTextChanged(s.toString()); }
                });
                editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                    @Override public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                        if (actionId == EditorInfo.IME_ACTION_DONE) { finishInput(); return true; }
                        return false;
                    }
                });
                AlertDialog.Builder builder = new AlertDialog.Builder(Main.MainActivity);
                builder.setTitle(heading).setView(editText);
                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int which) { finishInput(); }
                });
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override public void onDismiss(DialogInterface d) { cancelInput(); }
                });
                dialog = builder.create();
                dialog.show();
                if (dialog.getWindow() != null)
                    dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            }
        });
    }

    public void setText(final String text) {
        Main.MainActivity.runOnUiThread(new Runnable() {
            @Override public void run() {
                if (editText == null) return;
                editText.setText(text);
                editText.setSelection(editText.getText().length());
            }
        });
    }

    public void hide() {
        Main.MainActivity.runOnUiThread(new Runnable() {
            @Override public void run() { if (dialog != null) dialog.dismiss(); }
        });
    }

    private void finishInput() {
        if (callbackSent || editText == null) return;
        callbackSent = true;
        _onInputFinished(editText.getText().toString());
        if (dialog != null) dialog.dismiss();
    }

    private void cancelInput() {
        if (callbackSent) return;
        callbackSent = true;
        _onInputCanceled();
    }
}
