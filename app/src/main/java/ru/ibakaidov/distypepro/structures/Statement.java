package ru.ibakaidov.distypepro.structures;

public class Statement extends Structure {
    public String  text;
    public  String categoryId;
    public Statement(){
        super();
    }
    public  Statement(String text, String categoryId){
        super();
        this.text = text;
        this.categoryId = categoryId;
    }

    @Override
    public String toString() {
        return text;
    }
}
