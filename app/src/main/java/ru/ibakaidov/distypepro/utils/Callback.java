package ru.ibakaidov.distypepro.utils;

public abstract class Callback<T> {
    public abstract  void  onDone(T res);
    public void onError(Exception e){}

}
