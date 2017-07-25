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
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.digitalpersona.uareu.Engine;
import com.digitalpersona.uareu.Fid;
import com.digitalpersona.uareu.Fmd;
import com.digitalpersona.uareu.Importer;
import com.digitalpersona.uareu.Reader;
import com.digitalpersona.uareu.ReaderCollection;
import com.digitalpersona.uareu.UareUException;
import com.digitalpersona.uareu.UareUGlobal;
import com.digitalpersona.uareu.dpfpddusbhost.DPFPDDUsbException;
import com.digitalpersona.uareu.dpfpddusbhost.DPFPDDUsbHost;

import java.util.Map;

import io.idbox.fpsample.fingerprint.Globals;
import io.idbox.fpsample.util.Constants;
import io.idbox.fpsample.util.FileUtil;

public class ShowUserActivity extends AppCompatActivity {

    private static final String ACTION_USB_PERMISSION = "com.digitalpersona.uareu.dpfpddusbhost.USB_PERMISSION";
    private static final String TAG = Constants.TAG_PREFIX + "EnrolActivity";
    private static final int TIME_SHOW_MSG = 2000;

    //fingerprint
    private Reader reader;
    private boolean readerReady;
    private Engine engine;
    private int readerDPI;
    private Importer importer;
    private Reader.CaptureResult cap_result = null;
    private boolean m_reset = false;
    //UI
    private ImageView imageViewFP;
    private ImageView imageViewFPResult;
    private TextView textViewFP;
    private ProgressBar progressBarMatching;

    //state
    private Step step;
    private boolean onResume;
    private int errorMsg;

    private boolean usbReceiverRegistered;

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                Log.d(TAG, "onReceive com.digitalpersona.uareu.dpfpddusbhost.USB_PERMISSION");
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            openFpReader();
                            startEnrollAndMatch();
                        }
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
    private String readerName;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate FP_FAKE=" + BuildConfig.FP_FAKE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enrol);

        imageViewFP = (ImageView) findViewById(R.id.imageViewFP);
        textViewFP = (TextView) findViewById(R.id.textViewFP);
        imageViewFPResult = (ImageView) findViewById(R.id.imageViewFPResult);
        progressBarMatching = (ProgressBar) findViewById(R.id.progressBarMatching);
        handler = new Handler();

        if (BuildConfig.FP_FAKE) {
            manageView(Step.PUT_FINGER);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    manageView(Step.MATCHING);

                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            nextActivity(null, new byte[]{0x20});
                        }
                    }, TIME_SHOW_MSG);
                }
            }, TIME_SHOW_MSG);
        } else {
            step = Step.INIT;
            readerReady = false;
            // initialize dp sdk
            if (getFpReader()) {
                checkUsbPermission();
            }
        }
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();
        onResume = true;
        manageView(step);
        if (reader != null && readerReady && Step.PUT_FINGER.equals(step)) {
            startEnrollAndMatch();
        }
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        onResume = false;
        if (!BuildConfig.FP_FAKE) {
            stopFpReader();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (usbReceiverRegistered) {
            unregisterReceiver(mUsbReceiver);
        }
        super.onDestroy();
    }

    private boolean getFpReader() {
        Log.d(TAG, "getFpReader");
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
        readerName = reader.GetDescription().name; // Need the name in other places
        Log.d(TAG, "fpr reader found=" + readerName);
        return true;
    }

    private void openFpReader() {
        Log.d(TAG, "openFpReader");
        try {
            reader = Globals.getInstance().getReader(readerName, getApplicationContext());
            reader.Open(Reader.Priority.EXCLUSIVE);
            readerDPI = Globals.GetFirstDPI(reader);
            engine = UareUGlobal.GetEngine();
            importer = UareUGlobal.GetImporter();
            readerReady = true;
            step = Step.PUT_FINGER;
        } catch (Exception e) {
            Log.e(TAG, "Error when open fp reader", e);
            errorMsg = R.string.error_fp_readers_not_found;
            step = Step.ERROR;
            manageView(Step.ERROR);
        }
    }

    private void startEnrollAndMatch() {
        Log.d(TAG, "startEnrollAndMatch");
        // loop capture on a separate thread to avoid freezing the UI
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Fmd fmd = null;

                    m_reset = false;
                    try {
                        cap_result = reader.Capture(Fid.Format.ANSI_381_2004, Globals.DefaultImageProcessing, readerDPI, -1);
                    } catch (Exception e) {
                        if (!m_reset) {
                            Log.w("UareUSampleJava", "error during capture: " + e.toString());
                            readerName = "";
                            onBackPressed();
                        }
                    }

                    // an error occurred
                    if (cap_result == null || cap_result.image == null) return;

                    try {
                        // save bitmap image locally
                        bitmap = Globals.GetBitmapFromRaw(cap_result.image.getViews()[0].getImageData(), cap_result.image.getViews()[0].getWidth(), cap_result.image.getViews()[0].getHeight());
                        fmd = engine.CreateFmd(cap_result.image, Fmd.Format.ANSI_378_2004);
                    } catch (Exception e) {
                        Log.w("UareUSampleJava", "Egine error: " + e.toString());
                    }

                    final String[] found = new String[1];

                    Map<String, byte[]> files = FileUtil.readFiles(ShowUserActivity.this);
                    if (files != null && !files.isEmpty()) {
                        Log.d(TAG, files.size() + " fingerprint saved to match");
                        Fmd[] fmds = new Fmd[files.size()];
                        final String[] phones = new String[files.size()];
                        int i = 0;
                        for (Map.Entry<String, byte[]> entry : files.entrySet()) {
                            Log.d(TAG, "ImportFmd " + entry.getKey());
                            Fmd fmdSaved = importer.ImportFmd(entry.getValue(), Fmd.Format.ANSI_378_2004, Fmd.Format.ANSI_378_2004);
                            fmds[i] = fmdSaved;
                            phones[i] = entry.getKey();
                            i++;
                        }

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                updateGUI(Step.MATCHING);
                            }
                        });


                        Log.d(TAG, "Identify");
                        final Engine.Candidate[] candidates = engine.Identify(fmd, 0, fmds, 100000, 1);

                        final Fmd finalFmd = fmd;
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (candidates.length != 0) {
                                    int idx = candidates[0].fmd_index;
                                    found[0] = phones[idx];
                                    Log.d(TAG, "Fingerprint found=" + found[0]);
                                    nextActivity(found[0], finalFmd.getData());
                                } else {
                                    Log.d(TAG, "No saved fingerprint matching");
                                    errorMsg = R.string.fp_not_matching;
                                    manageView(Step.ERROR);
                                }
                            }
                        }, TIME_SHOW_MSG);

                    } else {
                        Log.d(TAG, "No saved FMDs, nothing to compare");
                        errorMsg = R.string.fp_not_matching;
                        manageView(Step.ERROR);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "error during capture", e);
                    errorMsg = R.string.fp_capture_failed;
                    manageView(Step.ERROR);
                }
            }
        }).start();
    }

    private void updateGUI(Step step) {
        manageView(step);
        imageViewFPResult.setImageBitmap(bitmap);
        imageViewFPResult.invalidate();
    }

    private void stopFpReader() {
        Log.d(TAG, "stopFpReader");
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
        Log.d(TAG, "checkUsbPermission");
        PendingIntent mPermissionIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        usbReceiverRegistered = true;
        registerReceiver(mUsbReceiver, filter);

        try {
            if (DPFPDDUsbHost.DPFPDDUsbCheckAndRequestPermissions(getApplicationContext(), mPermissionIntent, readerName)) {
                openFpReader();
            }
        } catch (DPFPDDUsbException e) {
            Log.e(TAG, "Failed to check usb permission ", e);
            errorMsg = R.string.fp_init_failed;
            manageView(Step.ERROR);
        }

    }

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

                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                textViewFP.setText("Place same finger again!");
                            }
                        }, 1000);
                        break;
                    case MATCHING:
                        // prevent handler from CAPTURED setting text
                        handler.removeCallbacksAndMessages(null);

                        imageViewFP.setVisibility(View.INVISIBLE);
                        imageViewFPResult.setVisibility(View.VISIBLE);
                        progressBarMatching.setVisibility(View.VISIBLE);

                        textViewFP.setTextColor(ContextCompat
                                .getColor(ShowUserActivity.this, R.color.green));
                        textViewFP.setText(R.string.fp_matching);
                        break;
                    case ERROR:
                        textViewFP.setTextColor(ContextCompat
                                .getColor(ShowUserActivity.this, R.color.red));
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

    private void nextActivity(String phoneNumber, byte[] fmd) {
        Log.d(TAG, "nextActivity");
        Intent intent = new Intent(ShowUserActivity.this, ShowUserInfoActivity.class);
        intent.putExtra(Constants.EXTRA_PHONE_NUMBER, phoneNumber);
        intent.putExtra(Constants.EXTRA_FMD, fmd);
        startActivity(intent);
        finish();   // Changed no history flag to false in manifest so that activity doesn't finish
                    // when reader initialising for the first time.
    }

    private enum Step {
        INIT,
        PUT_FINGER,
        CAPTURED,
        MATCHING,
        ERROR
    }

}
