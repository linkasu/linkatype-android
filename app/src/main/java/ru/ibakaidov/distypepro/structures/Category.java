package ru.ibakaidov.distypepro.structures;
import java.util.List;
import java.util.Map;

public class Category extends Structure{
    public  String label;

    public Map<String, Statement> statements;

    public Category(){

    }
    public Category(String label) {
        super();
        this.label = label;
    }
}
