package ru.ibakaidov.distypepro;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import ru.ibakaidov.distypepro.ui.MainActivity;

/**
 * Created by aacidov on 01.11.16.
 */
public class FileStorage {
    private static FileStorage instance;
    private File root;
    private OnPermissionSuccess onPermissionSuccess;

    public FileStorage(){
        checkPermision(new OnPermissionSuccess(){
            @Override
            public void onSuccess() {
                super.onSuccess();
                createRootDir();
            }
        });

    }

    private void createRootDir() {
        root = new File(Environment.getExternalStorageDirectory(), "DisType/");
        if (!root.exists()){
            root.mkdirs();
        }
    }

    public static FileStorage getInstance(){
        if(instance==null) instance = new FileStorage();
        return FileStorage.instance;
    }

    public void getAudioFile(final OnAudioFile onAudioFile){

        checkPermision(new OnPermissionSuccess(){
            @Override
            public void onSuccess() {
                super.onSuccess();
                createRootDir();

                SimpleDateFormat simpleDate =  new SimpleDateFormat("dd-MM-yyyy-HH-mm-ss");

                String filename = simpleDate.format(new Date())+".wav";

                File file = new File(root, filename);
                onAudioFile.onCreate(file);
            }

            @Override
            public void onFail() {
                super.onFail();
                onAudioFile.onFail();
            }
        });
    }

    private void verifyStoragePermissions() {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(MainActivity.activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(MainActivity.activity, new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
        } else {
            onPermissionSuccess.onSuccess();
        }
    }

    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 0: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    onPermissionSuccess.onSuccess();
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    onPermissionSuccess.onFail();
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }
    public void checkPermision(OnPermissionSuccess onPermissionSuccess) {
        this.onPermissionSuccess = onPermissionSuccess;
        verifyStoragePermissions();
    }

    protected static class OnAudioFile {
        public void onCreate(File file) {
        }

        public void onFail() {
        }
    }

    private static class OnPermissionSuccess {
        public void onSuccess() {

        }

        public void onFail() {
        }
    }
}
