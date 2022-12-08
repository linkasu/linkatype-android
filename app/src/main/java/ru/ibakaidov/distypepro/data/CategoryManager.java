package ru.ibakaidov.distypepro.data;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import ru.ibakaidov.distypepro.structures.Category;
import ru.ibakaidov.distypepro.utils.Callback;

public class CategoryManager extends Manager<Category> {

    @Override
    public void getList(Callback<HashMap<String, String>> callback) {
        getRoot()
                .orderByChild("created")
                .addValueEventListener(new ValueEventListener() {
                    @RequiresApi(api = Build.VERSION_CODES.N)
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Iterable<DataSnapshot> children = snapshot.getChildren();
                        HashMap<String, String> res = new HashMap<>();
                        ArrayList<Category> list = new ArrayList<Category>();
                        while( children.iterator().hasNext()){
                            DataSnapshot s = children.iterator().next();
                            Category category = Category.fromHashMap((HashMap) s.getValue());
                            list.add(category);
                        }
                        list.sort(new Comparator<Category>() {
                            @Override
                            public int compare(Category t1, Category t2) {
                                return (int) (t2.created-t1.created);
                            }
                        });
                        list.forEach(new Consumer<Category>() {
                            @Override
                            public void accept(Category category) {
                                res.put(category.id, category.label);
                            }
                        });
                        callback.onDone(res);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onError(error.toException());
                    }
                });
    }

    @Override
    public DatabaseReference getRoot() {
        return FirebaseDatabase
                .getInstance()
                .getReference("users/"+ FirebaseAuth.getInstance().getCurrentUser().getUid())
                .child("Category");
    }

    @Override
    public void edit(String key, String value, Callback callback) {
        Map<String, Object> update = new HashMap<>();
        update.put("label", value);
        getRoot()
                .child(key)
                .updateChildren(update);
    }

    @Override
    public void create(String res, Callback callback) {
        DatabaseReference r = getRoot()
                .push();
        Map<String, Object> update = new HashMap<>();
        update.put("label", res);
        update.put("id", r.getKey());
        update.put("created", new Date().getTime());
        r.updateChildren(update, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(@Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                callback.onDone(null);
            }
        });
    }
}
