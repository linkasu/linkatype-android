package ru.ibakaidov.distypepro;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.text.TextUtils;
import android.widget.AutoCompleteTextView;

/**
 * Created by aacidov on 06.06.16.
 */
public class SpeechController implements DialogInterface.OnClickListener {
    private static final int DIALOGS_COUNT = 5;
    private final String newDialog;
    /**
     * Current dialog id.
     */
    private int dialogId = 0;
    private String[] speechs = new String[DIALOGS_COUNT];
    private AutoCompleteTextView speechInput;

    public SpeechController(String newDialog, AutoCompleteTextView speechInput) {
        this.newDialog = newDialog;
        this.speechInput = speechInput;
    }

    public void openDialog() {
        String currentText = speechInput.getText().toString();
        speechs[dialogId] = currentText;
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(speechInput.getContext());
        dialogBuilder.setSingleChoiceItems(getSpeechsNames(), dialogId, this);
        dialogBuilder.setNegativeButton(R.string.cancel, null);
        dialogBuilder.setTitle(R.string.selectSpeech);
        dialogBuilder.show();
    }

    String[] getSpeechsNames() {
        String[] names = new String[DIALOGS_COUNT];
        for (int i = 0; i < DIALOGS_COUNT; i++) {
            if (TextUtils.isEmpty(speechs[i])) {
                names[i] = newDialog;
                continue;
            }
            names[i] = speechs[i];
        }
        return names;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        dialogId = which;
        String newText = speechs[dialogId];
        if (newText == null) {
            speechInput.setText("");
        } else {
            speechInput.setText(newText);
        }
        dialog.cancel();
    }

    public void onSay() {
        speechs[dialogId] = null;
    }
}