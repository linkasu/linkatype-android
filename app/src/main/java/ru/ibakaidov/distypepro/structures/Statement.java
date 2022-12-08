package ru.ibakaidov.distypepro.structures;

import java.util.HashMap;

public class Statement {
    public String categoryId;
    public long created;
    public String id;
    public String text;

    public Statement(String id, String categoryId, String text, long created){

        this.id = id;
        this.categoryId = categoryId;
        this.text = text;
        this.created = created;
    }


    public static Statement fromHashMap(HashMap hashMap){
        long created = hashMap.containsKey("created")? (long) hashMap.get("created"):0;
        String id = (String) hashMap.get("id");
        String categoryId = (String) hashMap.get("categoryId");
        String text = (String) hashMap.get("text");

        return new Statement(id,categoryId, text, created);
    }
}
