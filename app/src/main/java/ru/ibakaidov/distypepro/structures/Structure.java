package ru.ibakaidov.distypepro.structures;

import java.util.Date;

public abstract class Structure {
    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyz1234567890";
    private static final int SIZE = 16;
    public String id;
    public long created;
    public Structure(){
        id = generateId();
        created = new Date().getTime();
    }
    public static String generateId() {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < SIZE; i++) {
            result.append(ALPHABET.charAt((int) Math.floor(Math.random() * ALPHABET.length())));
        }
        return result.toString();
    }
}
