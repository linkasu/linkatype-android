package ru.ibakaidov.distypepro.components;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.recyclerview.widget.ListAdapter;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.MissingResourceException;
import java.util.function.Consumer;

import ru.ibakaidov.distypepro.R;
import ru.ibakaidov.distypepro.data.CategoryManager;
import ru.ibakaidov.distypepro.data.StatementManager;
import ru.ibakaidov.distypepro.dialogs.ConfirmDialog;
import ru.ibakaidov.distypepro.dialogs.ContextDialog;
import ru.ibakaidov.distypepro.dialogs.InputDialog;
import ru.ibakaidov.distypepro.structures.Category;
import ru.ibakaidov.distypepro.utils.Callback;
import ru.ibakaidov.distypepro.utils.HashMapAdapter;
import ru.ibakaidov.distypepro.utils.TTS;

public class BankGroup extends Component {

    private CategoryManager cm;
    private StatementManager sm;
    private GridView gridView;
    private ImageButton backButton;
    private ImageButton addButton;
    
    private TTS tts;
    private boolean state = false;



    public BankGroup(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.bank_group;
    }

    @Override
    protected void initUI() {
        gridView = findViewById(R.id.gridview);
        addButton = findViewById(R.id.add_button);
        backButton = findViewById(R.id.back_button);
        
        backButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                setState(false);
            }
        });
        
        addButton.setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        add();
                    }
                }
        );
        
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                HashMapAdapter adapter = (HashMapAdapter) adapterView.getAdapter();
                String key = adapter.getKey(i);
                String value = adapter.getItem(i);
                onItemSelected(key, value);
            }

        });

        gridView
                .setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                    @Override
                    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                        HashMapAdapter adapter = (HashMapAdapter) adapterView.getAdapter();
                        String key = adapter.getKey(i);
                        String value = adapter.getItem(i);
                        onLongItemClick(key, value);

                        return false;
                    }
                });
        cm = new CategoryManager();

        showCategories();

    }

    private void add() {
        InputDialog
                .showDialog(getContext(), R.string.create, new Callback<String>() {
                    @Override
                    public void onDone(String res) {
                        if(state){
                            sm.create(res, new Callback() {
                                @Override
                                public void onDone(Object res) {

                                }
                            });
                        } else {
                            cm.create(res, new Callback() {
                                @Override
                                public void onDone(Object res) {

                                }
                            });
                        }
                    }
                });
    }

    private void onLongItemClick(String key, String value) {
        ContextDialog
                .show(getContext(), value, new Callback<ContextDialog.ContextDialogActions>() {
                    @Override
                    public void onDone(ContextDialog.ContextDialogActions res) {
                        if(res== ContextDialog.ContextDialogActions.edit){
                            edit();
                        } else {
                            remove();
                        }
                    }

                    private void remove() {
                        ConfirmDialog
                                .showConfirmDialog(getContext(), R.string.remove, new Callback() {
                                    @Override
                                    public void onDone(Object res) {
                                        if(getState()){
                                            sm.remove(key, new Callback() {
                                                @Override
                                                public void onDone(Object res) {

                                                }
                                            });
                                        } else {
                                            cm.remove(key, new Callback() {
                                                @Override
                                                public void onDone(Object res) {

                                                }
                                            });
                                        }
                                    }
                                });
                    }

                    private void edit() {
                        InputDialog
                                .showDialog(getContext(), R.string.edit, value, new Callback<String>() {
                                    @Override
                                    public void onDone(String res) {
                                        if(getState()){
                                            sm.edit(key, res, new Callback() {
                                                @Override
                                                public void onDone(Object res) {

                                                }
                                            });
                                        } else {
                                            cm.edit(key, res, new Callback() {
                                                @Override
                                                public void onDone(Object res) {

                                                }
                                            });
                                        }
                                    }
                                });
                    }
                });
    }

    private void onItemSelected(String key, String value) {
        if(getState()){

            tts.speak(value);
        } else {
            // categories
            sm = new StatementManager(key);
            setState(true);

        }
    }


    private boolean getState(){
        return state;
    }

    private void setState(boolean value){
        state = value;
        if(value){
            showStatements();
            backButton.setVisibility(VISIBLE);
        } else {
            showCategories();
            backButton.setVisibility(GONE);
        }
    }

    private void showCategories() {

        cm.getList(new Callback<HashMap<String, String>>() {
            @Override
            public void onDone(HashMap<String, String> res) {
                if(getState()) return;

                HashMapAdapter adapter = new HashMapAdapter(getContext(),  res);
                gridView.setAdapter(adapter);
            }
        });

    }

    private void showStatements() {
        if(!getState()) return;
        sm.getList(new Callback<HashMap<String, String>>() {
            @Override
            public void onDone(HashMap<String, String> res) {
                HashMapAdapter adapter = new HashMapAdapter(getContext(),  res);
                gridView.setAdapter(adapter);
            }
        });
    }

    public void setTts(TTS tts) {
        this.tts = tts;
    }

    public void back() {
        setState(false);
    }
}
