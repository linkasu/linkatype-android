package ru.ibakaidov.distypepro;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.EditText;

/**
 * Created by aacidov on 06.06.16.
 */
public class SpeechController implements DialogInterface.OnClickListener{
    private static final int DIALOGSCOUNT = 5;
    private final String nd;
    private Context cxt;
    private int cid = 0; //current dialog id
    private String[] speechs = new String[DIALOGSCOUNT];

    private EditText si;

    public SpeechController(Context con, String newDialog, EditText speechInput){
        this.cxt=con;
        this.nd = newDialog;
        this.si = speechInput;

    }
    public void openDialog() {
        String currentText = si.getText().toString();
        speechs[cid]=currentText;

        AlertDialog.Builder adb = new AlertDialog.Builder(cxt);

        adb.setSingleChoiceItems(getSpeechsNames(), cid, this);
        adb.setNegativeButton(R.string.cancel, null);
        adb.setTitle(R.string.selectSpeech);
        adb.show();
    }

    String[] getSpeechsNames(){
        String[] sn = new String[DIALOGSCOUNT];
        for (int i = 0; i < DIALOGSCOUNT; i++) {
            if (speechs[i]==null||speechs[i]==""){
                sn[i]=this.nd;
                continue;
            }
            sn[i] = speechs[i];

        }
        return sn;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        cid = which;
        String newText = speechs[cid];
        if (newText == null) {
            si.setText("");
        } else {
            si.setText(newText);
        }

        dialog.cancel();
    }
    public void onSay(){
        speechs[cid]=null;
    }
}
