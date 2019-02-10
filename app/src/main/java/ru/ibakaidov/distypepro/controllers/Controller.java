package ru.ibakaidov.distypepro.controllers;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

import ru.ibakaidov.distypepro.structures.Category;
import ru.ibakaidov.distypepro.structures.Structure;

public abstract class Controller<T extends Structure> {
    private final Context mContext;
    private final DatabaseReference mRootTableReference;
    private DatabaseReference mTableReference;
    private final Class<T> mDataClass;

    public Controller(Context context, Class<T> dataClass) {
        mContext = context;
        mDataClass = dataClass;
        mRootTableReference =  FirebaseDatabase.getInstance().getReference().child("users").child(FirebaseAuth.getInstance().getCurrentUser().getUid());
    }

    public void  pushToTable (T data){

        mTableReference.child(data.id).setValue(data);
    }

    public void setmTableReference(DatabaseReference reference){
        mTableReference = reference;
    }

    public DatabaseReference getmTableReference() {
        return mTableReference;
    }

    public DatabaseReference getmRootTableReference() {
        return mRootTableReference;
    }

    public void setOnDataListener(final OnDataListener listener){
        mTableReference.orderByChild("created").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                ArrayList<T> list = new ArrayList<>();
                for(DataSnapshot dataChild : dataSnapshot.getChildren()){
                    Class<T> mClass = mDataClass;
                    list.add(dataChild.getValue(mClass));
                }
                listener.onData(list);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    public void removeItem(Structure structure){
        getmTableReference().child(structure.id).removeValue();
    }


    public static abstract class OnDataListener<T> {
        public abstract void onData(ArrayList<T> list);
    }
}
