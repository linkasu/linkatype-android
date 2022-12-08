package ru.ibakaidov.distypepro.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;

import java.util.HashMap;

import ru.ibakaidov.distypepro.utils.Callback;

public abstract class Manager<T> {

    public abstract void getList(Callback<HashMap<String, String>> callback);
    public abstract DatabaseReference getRoot();

    public void remove(String key, Callback callback) {
        getRoot().child(key).removeValue(new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(@Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                if(error!=null){
                    callback.onError(error.toException());
                    return;
                }
                callback.onDone(null);
            }
        });
    }

    public abstract void edit(String key, String value, Callback callback);

    public abstract void create(String res, Callback callback);
}
