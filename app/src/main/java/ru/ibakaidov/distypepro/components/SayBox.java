package ru.ibakaidov.distypepro.components;

import android.os.Build;
import android.speech.tts.UtteranceProgressListener;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import ru.ibakaidov.distypepro.R;
import ru.ibakaidov.distypepro.utils.TTS;

public class SayBox extends Component {
    private final EditText mEditText;
    private final Button mSayButton;
    private  final TTS mTTS;

    public  SayBox(View view) {
        super(view);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            mTTS = new TTS(mContext, new SayBoxUtteranceProgressListener());
        } else {
            mTTS = new TTS(mContext);
        }

        mEditText = (EditText) view.findViewById(R.id.sayEditText);
        mSayButton = (Button) view.findViewById(R.id.sayButton);


        mSayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                say();
            }
        });
    }

    private void say(){
        String text = mEditText.getText().toString();
        say(text);
    }
    public void say(String text) {
        if(mTTS.getSpeaking()){
            mTTS.stop();
            return;
        }

        mTTS.speak(text);

    }

    public String getText() {
        return mEditText.getText().toString();
    }

    public void clear() {
        mEditText.setText("");
    }

    public void pasteText(String text) {
        mEditText.getText().append(text);
    }

    @RequiresApi(api = Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
    private class SayBoxUtteranceProgressListener extends UtteranceProgressListener {
        @Override
        public void onStart(String utteranceId) {
            mSayButton.setText(R.string.stop);
        }

        @Override
        public void onDone(String utteranceId) {
            mSayButton.setText(R.string.say);
        }

        @Override
        public void onStop(String utteranceId, boolean interrupted) {
            onDone(utteranceId);
        }


        @Override
        public void onError(String utteranceId) {

        }
    }
}
