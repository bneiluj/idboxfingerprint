package io.idbox.fpsample;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import io.idbox.fpsample.util.Constants;

/**
 * Create by IDbox
 */
public class PayAsYouGoInfoActivity extends AppCompatActivity {

    private static final String TAG = PayAsYouGoInfoActivity.class.getSimpleName();

    private ProgressDialog progress;
    int jumpTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pay_as_you_go);
        progress = new ProgressDialog(this);

        Intent intent = getIntent();
        if(intent == null || !intent.hasExtra(Constants.EXTRA_PHONE_NUMBER)){
            Log.e(TAG, "not the right extras in intent, so close");
            finish();
        }
        //noinspection ConstantConditions
        String savedPhoneNumber = intent.getStringExtra(Constants.EXTRA_PHONE_NUMBER);
        EditText editTextPhoneNumber = (EditText) findViewById(R.id.editTextPhoneNumber);

        editTextPhoneNumber.setText(savedPhoneNumber);
    }

    public void clickCharge(View view) {
        progress.setMessage("Charging... ");
        progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progress.setIndeterminate(true);
        progress.setProgressNumberFormat(null);
        progress.setProgressPercentFormat(null);
        progress.show();

        progress.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                finish();
            }
        });

        final int totalProgressTime = 100;

        final Thread t = new Thread(){

            @Override
            public void run(){
                while(jumpTime < totalProgressTime){
                    try {
                        sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
                progress.dismiss();
            }
        };
        t.start();
    }

}
