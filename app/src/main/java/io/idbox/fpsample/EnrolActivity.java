package io.idbox.fpsample;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.graphics.BitmapCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import asia.kanopi.fingerscan.Fingerprint;
import asia.kanopi.fingerscan.Status;
import io.idbox.fpsample.callback.OnFingerprintCallback;
import io.idbox.fpsample.util.Constants;

public class EnrolActivity extends AppCompatActivity implements OnFingerprintCallback {

    private static final String TAG = Constants.TAG_PREFIX + "EnrolActivity";
    private static final int TIME_SHOW_MSG = 3000;
    private byte[] imgFp;

    private enum Step {PUT_FINGER, CAPTURED, MATCHING, ERROR}

    private ImageView imageViewFP;
    private ImageView imageViewFPResult;
    private TextView textViewFP;
    private ProgressBar progressBarMatching;

    private Fingerprint fingerprint;
    private Step step;
    private boolean onResume;



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
            fingerprint = new Fingerprint();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        onResume = true;
        if (!BuildConfig.FP_FAKE) {

            switch (FingerprintWrapper.getInstance().getStatus()) {
                case IDLE:
                    manageView(Step.PUT_FINGER);
                    fingerprint.scan(this, printHandler, updateHandler);
                    break;
                case CHECKING:
                    manageView(Step.MATCHING);
                    break;
            }

        }
    }

    @Override
    protected void onPause() {
        onResume = false;
        if (!BuildConfig.FP_FAKE) {
            fingerprint.turnOffReader();
        }
        super.onPause();
    }

    @Override
    public void result(boolean success) {
        if(success){
            //fingerprint already registered
            Toast.makeText(EnrolActivity.this, R.string.fp_already_registered, Toast.LENGTH_SHORT).show();
            finish();
        }else{
            //no fingerprint matching, could go to user info
            nextActivity(imgFp);
        }
    }

    Handler updateHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            int status = msg.getData().getInt("status");
            switch (status) {
                case Status.INITIALISED:
                    textViewFP.setText("Setting up reader");
                    break;
                case Status.SCANNER_POWERED_ON:
                    textViewFP.setText("Reader powered on");
                    break;
                case Status.READY_TO_SCAN:
                    textViewFP.setText("Ready to scan finger");
                    break;
                case Status.FINGER_DETECTED:
                    textViewFP.setText("Finger detected");
                    break;
                case Status.RECEIVING_IMAGE:
                    textViewFP.setText("Receiving image");
                    break;
                case Status.FINGER_LIFTED:
                    textViewFP.setText("Finger has been lifted off reader");
                    break;
                case Status.SCANNER_POWERED_OFF:
                    textViewFP.setText("Reader is off");
                    break;
                case Status.SUCCESS:
                    textViewFP.setText("Fingerprint successfully captured");
                    break;
                case Status.ERROR:
                    textViewFP.setText(msg.getData().getString("errorMessage"));
                    break;
                default:
                    textViewFP.setText(msg.getData().getString("errorMessage"));
                    break;

            }
        }
    };

    Handler printHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            byte[] image;
            String errorMessage = "empty";
            int status = msg.getData().getInt("status");
            if (status == Status.SUCCESS) {

                imgFp = msg.getData().getByteArray("img");
                Bitmap bm = BitmapFactory.decodeByteArray(imgFp, 0, imgFp.length);
                imageViewFPResult.setImageBitmap(bm);

                manageView(Step.CAPTURED);
            } else {
                errorMessage = msg.getData().getString("errorMessage");
                textViewFP.setText(errorMessage);

                manageView(Step.ERROR);
            }

        }
    };

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
                                FingerprintWrapper.getInstance().match(EnrolActivity.this,imgFp, EnrolActivity.this);
                            }
                        }, TIME_SHOW_MSG);
                    case MATCHING:
                        imageViewFP.setVisibility(View.INVISIBLE);
                        imageViewFPResult.setVisibility(View.INVISIBLE);
                        progressBarMatching.setVisibility(View.VISIBLE);

                        textViewFP.setText(R.string.fp_matching);
                        break;
                    case ERROR:
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
}
