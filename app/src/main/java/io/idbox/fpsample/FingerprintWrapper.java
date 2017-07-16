package io.idbox.fpsample;

import android.content.Context;
import android.util.Log;

import java.util.Map;

import io.idbox.fpsample.callback.OnFingerprintCallback;
import io.idbox.fpsample.util.Constants;
import io.idbox.fpsample.util.FileUtil;

/**
 * Created by yhuel on 13/07/17.
 */

public class FingerprintWrapper {

    //size in pixel of uareu 4500: http://www.pixelcalculator.com/index.php?lang=en&DD_DP=1&i1_=14.6&i2_=18.1&i3_=512&i4_=294&i5_=365&DP=1&round=&FORM=1&DD=8#b
    private static final int FP_HEIGHT = 294;
    private static final int FP_WIDTH = 365;
    //TODO set good value for match success
    private static final float FP_MATCH_SUCCESS_RESULT = 1.0f;
    private static final String TAG = Constants.TAG_PREFIX + "FingerprintWrapper";
    public enum Status {IDLE, CHECKING};
    private static FingerprintWrapper instance = new FingerprintWrapper();

    private Status status;

    private FingerprintWrapper(){
        status = Status.IDLE;
    }

    public static FingerprintWrapper getInstance() {
        return instance;
    }

    public Status getStatus() {
        return status;
    }

    public synchronized void match(final Context ctx, final byte[] img, final OnFingerprintCallback callback){
        if(status != Status.IDLE){
            Log.w(TAG, "don't launch twice");
            return;
        }

        status = Status.CHECKING;

        new Thread(new Runnable() {
            @Override
            public void run() {

                Map<String, byte[]> files = FileUtil.lstFile(ctx);

                if(files == null || files.isEmpty()){
                    Log.w(TAG, "No fingreprint saved so nothing to match");
                    if(callback!= null){
                        callback.result(false);
                    }
                    status = Status.IDLE;
                    return;
                }
                boolean callbackResult = false;

                for(Map.Entry<String, byte[]> entry : files.entrySet()){
                    Log.d(TAG, "Checking " + entry.getKey());
                    float result = nativeCompare(img, entry.getValue(),FP_WIDTH, FP_HEIGHT);
                    Log.d(TAG, "Checked " + entry.getKey() + " result=" + result);

                    if(result>= FP_MATCH_SUCCESS_RESULT){
                         callbackResult = true;
                        break;
                    }
                }

                if(callback!= null){
                    callback.result(callbackResult);
                }
                status = Status.IDLE;
            }
        }).start();
    }

    //294 x 365 pix
    public static native float nativeCompare(byte[] capture, byte[] check, int width, int height);
}
