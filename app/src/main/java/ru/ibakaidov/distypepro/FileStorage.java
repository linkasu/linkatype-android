package ru.ibakaidov.distypepro;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by aacidov on 01.11.16.
 */
public class FileStorage {
    private File root;

    public FileStorage (Activity activity){
        verifyStoragePermissions(activity);
        root = new File(Environment.getExternalStorageDirectory(), "DisType/");
        if (!root.exists()){
            root.mkdirs();
        }
    }

    public File getAudioFile(){

        SimpleDateFormat simpleDate =  new SimpleDateFormat("dd-MM-yyyy-HH-mm-ss");

        String filename = simpleDate.format(new Date())+".wav";

        File file = new File(root, filename);
        return file;
    }

    private void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(activity, new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
        }
    }

}
