package ru.ibakaidov.distypepro.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import ru.ibakaidov.distypepro.structures.Category;
import ru.ibakaidov.distypepro.structures.Statement;
import ru.ibakaidov.distypepro.utils.Callback;

public class StatementManager extends Manager<Statement> {


    private String categoryId;

    public StatementManager(String categoryId){
        this.categoryId = categoryId;
    }

    @Override
    public void getList(Callback<HashMap<String, String>> callback) {
        getRoot()
                .orderByChild("created")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Iterable<DataSnapshot> children = snapshot.getChildren();
                        HashMap<String, String> res = new HashMap<>();
                        while( children.iterator().hasNext()){
                            DataSnapshot s = children.iterator().next();
                            Statement statement = Statement.fromHashMap((HashMap) s.getValue());
                            res.put(statement.id, statement.text);
                        }
                        callback.onDone(res);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    @Override
    public DatabaseReference getRoot() {
        return FirebaseDatabase
                .getInstance()
                .getReference("users/"+ FirebaseAuth.getInstance().getCurrentUser().getUid()+"/Category/"+categoryId+"/statements/");
    }

    @Override
    public void edit(String key, String value, Callback callback) {
        Map<String, Object> update = new HashMap<>();
        update.put("text", value);
        getRoot()
                .child(key)
                .updateChildren(update);
    }

    @Override
    public void create(String res, Callback callback) {
        DatabaseReference r = getRoot()
                .push();
        Map<String, Object> update = new HashMap<>();
        update.put("text", res);
        update.put("id", r.getKey());
        update.put("categoryId", categoryId);
        update.put("created", new Date().getTime());

        r.updateChildren(update, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(@Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                callback.onDone(null);
            }
        });
    }
}
