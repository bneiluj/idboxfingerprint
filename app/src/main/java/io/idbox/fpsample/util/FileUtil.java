package io.idbox.fpsample.util;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import io.idbox.fpsample.exception.FileAlreadyExists;

/**
 * File utility
 * Created by USER on 15/07/2017.
 */

public class FileUtil {

    private static final String EXTENSION = ".idb";
    private static final String TAG = Constants.TAG_PREFIX + "FileUtil";


    public static boolean checkFileExist(Context ctx, String phoneNumber){
        if(!isExternalStorageReadable()){
            Log.e(TAG, "external storage not readable");
            return false;
        }

        File file = createFilenameFromNumber(ctx, phoneNumber);

        Log.d(TAG, "Check if exists " + file.getAbsolutePath());
        return file.exists();
    }

    public static boolean saveFile(Context ctx, String phoneNumber, byte[] data) throws FileAlreadyExists{
        if(!isExternalStorageWritable()){
            Log.e(TAG, "external storage not writable");
            return false;
        }
        File file = createFilenameFromNumber(ctx, phoneNumber);
        Log.d(TAG, "Save to " + file.getAbsolutePath());

        if (file.exists()){
            Log.e(TAG, "File " + file.getAbsolutePath() + " already exists");
            throw new FileAlreadyExists();
        }
        try {
            if(!file.createNewFile()){
                Log.e(TAG, "Cant create file "+file.getAbsolutePath());
                return false;
            }
        } catch (IOException e) {
            Log.e(TAG, "Cant create file "+file.getAbsolutePath() , e);
            return false;
        }

        try {
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
            bos.write(data);
            bos.flush();
            bos.close();
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Cant write to file "+file.getAbsolutePath() , e);
            return false;
        }
    }

    private static File createFilenameFromNumber(Context ctx, String phoneNumber){
        return new File(ctx.getExternalFilesDir(null), phoneNumber + EXTENSION);
    }

    public static Map<String,byte[]> lstFile (Context ctx){
        if(!isExternalStorageReadable()){
            Log.e(TAG, "external storage not readable");
            return null;
        }

        File[] files = ctx.getExternalFilesDir(null).listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(EXTENSION);
            }
        });

        if(files==null || files.length==0){
            Log.d(TAG, "no files with extension " + EXTENSION);
            return null;
        }

        Map<String, byte[]> result = new HashMap<>();
        for(File file : files){
            byte[] data = readFile(ctx,file);

            if(data!=null){
                result.put(file.getName(),data);
            }
        }
        return result;
    }

    private static byte[] readFile(Context ctx, File file){
        if (!file.exists()){
            Log.e(TAG, "File dont exists");
            return null;
        }
        try {
            if(!file.createNewFile()){
                Log.e(TAG, "Cant create file "+file.getAbsolutePath());
                return null;
            }
        } catch (IOException e) {
            Log.e(TAG, "Cant create file "+file.getAbsolutePath() , e);
            return null;
        }
        BufferedInputStream bis = null;
        try {
            byte[] data = new byte[(int) file.length()];
            bis = new BufferedInputStream(new FileInputStream(file));
            bis.read(data);
            bis.close();
            return data;
        } catch (IOException e) {
            Log.e(TAG, "Cant read file "+file.getAbsolutePath() , e);
            return null;
        }
    }

    /* Checks if external storage is available for read and write */
    private static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    /* Checks if external storage is available to at least read */
    private static boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }
}
