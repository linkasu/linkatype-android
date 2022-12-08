package ru.ibakaidov.distypepro.structures;

import java.util.HashMap;

public class Category {

    public long created = 0;
    public String id = null;
    public String label = null;

    public Category( String id, String label, long created) {

        this.created = created;
        this.id = id;
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }

    public static Category fromHashMap(HashMap hashMap){
       long created = hashMap.containsKey("created")? (long) hashMap.get("created"):0;
       String id = (String) hashMap.get("id");
       String label = (String) hashMap.get("label");

        return new Category(id, label, created);
    }

}
