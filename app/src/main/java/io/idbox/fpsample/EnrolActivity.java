package io.idbox.fpsample;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.digitalpersona.uareu.Engine;
import com.digitalpersona.uareu.Fid;
import com.digitalpersona.uareu.Fmd;
import com.digitalpersona.uareu.Reader;
import com.digitalpersona.uareu.ReaderCollection;
import com.digitalpersona.uareu.UareUException;
import com.digitalpersona.uareu.UareUGlobal;
import com.digitalpersona.uareu.dpfpddusbhost.DPFPDDUsbException;
import com.digitalpersona.uareu.dpfpddusbhost.DPFPDDUsbHost;

import io.idbox.fpsample.callback.OnFingerprintCallback;
import io.idbox.fpsample.fingerprint.Globals;
import io.idbox.fpsample.util.Constants;

public class EnrolActivity extends AppCompatActivity implements OnFingerprintCallback {

    private static final String ACTION_USB_PERMISSION = "com.digitalpersona.uareu.dpfpddusbhost.USB_PERMISSION";
    private static final String TAG = Constants.TAG_PREFIX + "EnrolActivity";
    private static final int TIME_SHOW_MSG = 3000;
    private byte[] imgFp;
    private Reader reader;
    private boolean readerReady;
    private Engine engine;
    private int readerDPI;
    private EnrollmentCallback enrollThread;
    private ImageView imageViewFP;
    private ImageView imageViewFPResult;
    private TextView textViewFP;
    private ProgressBar progressBarMatching;
    private Step step;
    private boolean onResume;
    private int errorMsg;
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (device != null && intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        openFpReader();

                    } else {
                        Log.e(TAG, "Failed to request usb permission ");
                        errorMsg = R.string.fp_init_failed;
                        manageView(Step.ERROR);
                    }
                }
            }
        }
    };
    private Bitmap bitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enrol);

        imageViewFP = (ImageView) findViewById(R.id.imageViewFP);
        textViewFP = (TextView) findViewById(R.id.textViewFP);
        imageViewFPResult = (ImageView) findViewById(R.id.imageViewFPResult);
        progressBarMatching = (ProgressBar) findViewById(R.id.progressBarMatching);

        if (BuildConfig.FP_FAKE) {
            manageView(Step.PUT_FINGER);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    manageView(Step.MATCHING);

                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            nextActivity(new byte[]{0x20});
                        }
                    }, TIME_SHOW_MSG);
                }
            }, TIME_SHOW_MSG);
        } else {
            step = Step.INIT;
            readerReady = false;
            // initiliaze dp sdk
            if (getFpReader()) {
                checkUsbPermission();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        onResume = true;
        manageView(step);
        if (reader != null && readerReady && Step.PUT_FINGER.equals(step)) {
            startEnrollAndMatch();
        }
    }

    @Override
    protected void onPause() {
        onResume = false;
        if (!BuildConfig.FP_FAKE) {
            stopFpReader();
        }
        super.onPause();
    }

    @Override
    public void result(boolean success) {
        if (success) {
            //fingerprint already registered
            Toast.makeText(EnrolActivity.this, R.string.fp_already_registered, Toast.LENGTH_SHORT).show();
            finish();
        } else {
            //no fingerprint matching, could go to user info
            nextActivity(imgFp);
        }
    }

    private boolean getFpReader() {
        ReaderCollection readers;
        try {
            readers = Globals.getInstance().getReaders(getApplicationContext());
        } catch (UareUException e) {
            Log.e(TAG, "Error when retrieve fp readers", e);
            errorMsg = R.string.error_fp_readers_not_found;
            step = Step.ERROR;
            return false;
        }

        if (readers.size() < 1) {
            Log.e(TAG, "No fp readers");
            errorMsg = R.string.error_fp_readers_not_found;
            step = Step.ERROR;
            return false;
        }
        reader = readers.get(0);
        Log.d(TAG, "fpr reader found=" + reader.GetDescription().name);
        return true;
    }

    private void openFpReader() {
        try {
            reader.Open(Reader.Priority.EXCLUSIVE);
            readerDPI = Globals.GetFirstDPI(reader);
            engine = UareUGlobal.GetEngine();
            readerReady = true;
        } catch (Exception e) {
            Log.e(TAG, "Error when open fp reader", e);
            errorMsg = R.string.error_fp_readers_not_found;
            step = Step.ERROR;
            manageView(Step.ERROR);
        }
    }

    private void startEnrollAndMatch() {
        // loop capture on a separate thread to avoid freezing the UI
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    enrollThread = new EnrollmentCallback();

                    Fmd fmd = engine.CreateEnrollmentFmd(Fmd.Format.ANSI_378_2004, enrollThread);
                    if(fmd==null){
                        Log.e(TAG,"capture failed");
                        errorMsg = R.string.fp_capture_failed;
                        manageView(Step.ERROR);
                        return;
                    }
                    if(fmd.getViewCnt()!=1){
                        Log.e(TAG,"Not one capture in result : " + fmd.getViewCnt());
                        errorMsg = R.string.fp_capture_failed;
                        manageView(Step.ERROR);
                        return;
                    }

                    imageViewFPResult.setImageBitmap(bitmap);
                    manageView(Step.CAPTURED);

                    //TODO get FIV from result + match with other FIV  + send template to other activity
//                    fmd.getViews()[0].
//
//
//                    Fmd m_temp = engine.CreateFmd(cap_result.image, Fmd.Format.ANSI_378_2004);
//                    Fmd[] m_fmds_temp = new Fmd[] {m_fmd1, m_fmd2, m_fmd3, m_fmd4};
//                    results = m_engine.Identify(m_temp, 0, m_fmds_temp, 100000, 2);
//
//                    if (results.length != 0)
//                    {
//                        m_score = m_engine.Compare(m_fmds_temp[results[0].fmd_index], 0, m_temp, 0);



                } catch (Exception e) {
                    Log.e(TAG, "error during capture", e);
                    errorMsg = R.string.fp_capture_failed;
                    manageView(Step.ERROR);
                }
            }
        }).start();
    }

    private void stopFpReader() {
        if (reader != null && readerReady) {
            try {
                reader.CancelCapture();
            } catch (Exception e) {
                Log.e(TAG, "Error when cancel capture", e);
            }
            try {
                reader.Close();
            } catch (UareUException e) {
                Log.e(TAG, "Error when close reader", e);
            }
        }
    }

    private void checkUsbPermission() {
        PendingIntent mPermissionIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        registerReceiver(mUsbReceiver, filter);

        try {
            if (DPFPDDUsbHost.DPFPDDUsbCheckAndRequestPermissions(getApplicationContext(), mPermissionIntent, reader.GetDescription().name)) {
                openFpReader();
            }
        } catch (DPFPDDUsbException e) {
            Log.e(TAG, "Failed to check usb permission ", e);
            errorMsg = R.string.fp_init_failed;
            manageView(Step.ERROR);
        }

    }


    //    Handler updateHandler = new Handler(Looper.getMainLooper()) {
    //        @Override
    //        public void handleMessage(Message msg) {
    //            int status = msg.getData().getInt("status");
    //            switch (status) {
    //                case Status.INITIALISED:
    //                    textViewFP.setText("Setting up reader");
    //                    break;
    //                case Status.SCANNER_POWERED_ON:
    //                    textViewFP.setText("Reader powered on");
    //                    break;
    //                case Status.READY_TO_SCAN:
    //                    textViewFP.setText("Ready to scan finger");
    //                    break;
    //                case Status.FINGER_DETECTED:
    //                    textViewFP.setText("Finger detected");
    //                    break;
    //                case Status.RECEIVING_IMAGE:
    //                    textViewFP.setText("Receiving image");
    //                    break;
    //                case Status.FINGER_LIFTED:
    //                    textViewFP.setText("Finger has been lifted off reader");
    //                    break;
    //                case Status.SCANNER_POWERED_OFF:
    //                    textViewFP.setText("Reader is off");
    //                    break;
    //                case Status.SUCCESS:
    //                    textViewFP.setText("Fingerprint successfully captured");
    //                    break;
    //                case Status.ERROR:
    //                    textViewFP.setText(msg.getData().getString("errorMessage"));
    //                    break;
    //                default:
    //                    textViewFP.setText(msg.getData().getString("errorMessage"));
    //                    break;
    //
    //            }
    //        }
    //    };
    //
    //    Handler printHandler = new Handler(Looper.getMainLooper()) {
    //        @Override
    //        public void handleMessage(Message msg) {
    //            byte[] image;
    //            String errorMessage = "empty";
    //            int status = msg.getData().getInt("status");
    //            if (status == Status.SUCCESS) {
    //
    //                imgFp = msg.getData().getByteArray("img");
    //                Bitmap bm = BitmapFactory.decodeByteArray(imgFp, 0, imgFp.length);
    //                imageViewFPResult.setImageBitmap(bm);
    //
    //                manageView(Step.CAPTURED);
    //            } else {
    //                errorMessage = msg.getData().getString("errorMessage");
    //                textViewFP.setText(errorMessage);
    //
    //                manageView(Step.ERROR);
    //            }
    //
    //        }
    //    };

    private void manageView(final Step step) {
        Log.d(TAG, "manageView " + step);
        this.step = step;
        if (!onResume) {
            return;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switch (step) {

                    case INIT:
                        imageViewFP.setVisibility(View.VISIBLE);
                        imageViewFPResult.setVisibility(View.INVISIBLE);
                        progressBarMatching.setVisibility(View.INVISIBLE);

                        textViewFP.setText(R.string.init);
                        break;
                    case PUT_FINGER:
                        imageViewFP.setVisibility(View.VISIBLE);
                        imageViewFPResult.setVisibility(View.INVISIBLE);
                        progressBarMatching.setVisibility(View.INVISIBLE);

                        textViewFP.setText(R.string.finger_put);
                        break;
                    case CAPTURED:
                        imageViewFP.setVisibility(View.INVISIBLE);
                        imageViewFPResult.setVisibility(View.VISIBLE);
                        progressBarMatching.setVisibility(View.INVISIBLE);

                        textViewFP.setText(R.string.finger_success);

                        //goto matching step
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                manageView(Step.MATCHING);
                                FingerprintWrapper.getInstance().match(EnrolActivity.this, imgFp, EnrolActivity.this);
                            }
                        }, TIME_SHOW_MSG);
                    case MATCHING:
                        imageViewFP.setVisibility(View.INVISIBLE);
                        imageViewFPResult.setVisibility(View.INVISIBLE);
                        progressBarMatching.setVisibility(View.VISIBLE);

                        textViewFP.setText(R.string.fp_matching);
                        break;
                    case ERROR:
                        textViewFP.setText(errorMsg);
                        imageViewFP.setVisibility(View.VISIBLE);
                        imageViewFPResult.setVisibility(View.INVISIBLE);
                        progressBarMatching.setVisibility(View.INVISIBLE);

                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                finish();
                            }
                        }, TIME_SHOW_MSG);
                        break;
                }
            }
        });
    }

    private void nextActivity(byte[] img) {
        Intent intent = new Intent(EnrolActivity.this, UserInfoActivity.class);
        intent.putExtra(Constants.EXTRA_FP, img);
        startActivity(intent);
    }

    private enum Step {
        INIT,
        PUT_FINGER,
        CAPTURED,
        MATCHING,
        ERROR
    }

    public class EnrollmentCallback extends Thread implements Engine.EnrollmentCallback {


        public EnrollmentCallback() {
        }

        // callback function is called by dp sdk to retrieve fmds until a null is returned
        @Override
        public Engine.PreEnrollmentFmd GetFmd(Fmd.Format format) {
            Engine.PreEnrollmentFmd result = null;

            Reader.CaptureResult captureResult;
            try {
                captureResult = reader.Capture(Fid.Format.ANSI_381_2004, Globals.DefaultImageProcessing, readerDPI, -1);
            } catch (Exception e) {
                Log.e(TAG, "error during capture: " + e.toString());
                return null;
            }


            try {
                // save bitmap image locally
                bitmap = Globals.GetBitmapFromRaw(captureResult.image.getViews()[0].getImageData(), captureResult.image.getViews()[0].getWidth(), captureResult.image.getViews()[0].getHeight());
                result = new Engine.PreEnrollmentFmd();
                result.fmd = engine.CreateFmd(captureResult.image, Fmd.Format.ANSI_378_2004);
                result.view_index = 0;

            } catch (Exception e) {
                Log.e(TAG, "Engine error: " + e.toString());
                return null;
            }

            return result;
        }
    }

}
