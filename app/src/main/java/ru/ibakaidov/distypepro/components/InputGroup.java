package ru.ibakaidov.distypepro.components;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;

import androidx.annotation.Nullable;

import com.google.firebase.analytics.FirebaseAnalytics;

import ru.ibakaidov.distypepro.R;
import ru.ibakaidov.distypepro.screens.SpotlightActivity;
import ru.ibakaidov.distypepro.utils.Callback;
import ru.ibakaidov.distypepro.utils.ProgressState;
import ru.ibakaidov.distypepro.utils.TTS;

public class InputGroup extends Component {
    private TTS tts;
    private EditText ttsEditText;
    private Button sayButton;
    private ImageButton spotlightButton;
    private Spinner chatSpinner;
    private int prevSpinnerValue = 0;
    private String[] textCache = new String[] {"", "", ""};

    public InputGroup(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public void setTts(TTS tts) {
        this.tts = tts;

        tts.setOnPlayCallback(new Callback<ProgressState>() {
            @Override
            public void onDone(ProgressState res) {
                sayButton.setText(res == ProgressState.START?R.string.stop:R.string.say);
            }
        });
    }

    @Override
    protected void initUI() {
        ttsEditText = findViewById(R.id.text_to_speech_edittext);
        sayButton = findViewById(R.id.say_button);
        spotlightButton = findViewById(R.id.spotlight_button);


        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if(ttsEditText.hasFocus()){
                    ttsEditText.clearFocus();
                }
            }
        });
        View v = this;
        ttsEditText.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                ViewGroup.LayoutParams params = v.getLayoutParams();
                params.height = b?LayoutParams.MATCH_PARENT: ViewGroup.LayoutParams.WRAP_CONTENT;
                v.setLayoutParams(params);
            }
        });

        sayButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                say();
            }
        });

        spotlightButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                spotlight();
            }
        });

    }

    private void setText(String s) {
        ttsEditText.setText(s);
    }

    private void spotlight() {
        SpotlightActivity
                .show(getContext(), getText());

        FirebaseAnalytics
                .getInstance(getContext())
                .logEvent("spotlight", null);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.input_group;
    }

    private void say() {
        String text = getText();
        tts.speak(text);
        FirebaseAnalytics
                .getInstance(getContext())
                .logEvent("say", null);
    }

    private String getText() {
        return ttsEditText.getText().toString();
    }

    public void setChatSpinner(Spinner chatSpinner) {
        this.chatSpinner = chatSpinner;
        ArrayAdapter adapter = new ArrayAdapter<String >(getContext(), androidx.appcompat.R.layout.support_simple_spinner_dropdown_item, new String[]{"1", "2", "3"});
        chatSpinner.setAdapter(adapter);

        chatSpinner
                .setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                        textCache[prevSpinnerValue] = getText();
                        setText(textCache[i]);
                        prevSpinnerValue = i;

                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {

                    }
                });
    }

    public void clear() {
        ttsEditText.setText("");
    }

    public void back() {
        ttsEditText.clearFocus();
    }
}