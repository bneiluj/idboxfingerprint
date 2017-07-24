package io.idbox.fpsample;

import android.*;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.vision.barcode.Barcode;

import java.io.File;

import io.idbox.fpsample.exception.FileAlreadyExists;
import io.idbox.fpsample.util.Constants;
import io.idbox.fpsample.util.FileUtil;
import io.idbox.fpsample.util.SmsUtil;
import io.idbox.fpsample.util.ValidatorUtil;

import static android.R.id.message;

/**
 * Create by IDbox
 */

public class UserInfoActivity extends AppCompatActivity {

    private static final int MY_PERMISSIONS_REQUEST_SEND_SMS = 402;
    private static final String TAG = Constants.TAG_PREFIX + "UserInfoActivity";
    private static final int RC_BARCODE_CAPTURE = 9001;

    private String barcodeValue;
    private String phoneNumber;
    private EditText editTextPhoneNumber;
    private EditText editTextPhoneNumberConfirm;
    private TextView textViewCardStatus;
    private Button buttonValidate;
    private boolean onResume;
    private byte[] fmd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_info);

        Intent intent = getIntent();
        if(intent == null || !intent.hasExtra(Constants.EXTRA_FMD)){
            Log.e(TAG, "no extra fp byte array in intent, so close");
            finish();
        }
        fmd = intent.getByteArrayExtra(Constants.EXTRA_FMD);
        editTextPhoneNumber = (EditText) findViewById(R.id.editTextPhoneNumber);
        editTextPhoneNumberConfirm = (EditText) findViewById(R.id.editTextPhoneNumberConfirm);
        textViewCardStatus = (TextView) findViewById(R.id.textViewCardStatus);
        buttonValidate = (Button) findViewById(R.id.buttonValidate);
    }

    @Override
    protected void onResume() {
        super.onResume();
        onResume = true;
    }

    @Override
    protected void onPause() {
        onResume = false;
        super.onPause();
    }

    public void clickBarCode(View view) {
        Intent intent = new Intent(this, BarcodeCaptureActivity.class);
        intent.putExtra(BarcodeCaptureActivity.AutoFocus, true);

        startActivityForResult(intent, RC_BARCODE_CAPTURE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RC_BARCODE_CAPTURE) {
            if (resultCode == CommonStatusCodes.SUCCESS) {
                if (data != null) {
                    Barcode barcode = data.getParcelableExtra(BarcodeCaptureActivity.BarcodeObject);
                    textViewCardStatus.setText(R.string.barcode_success);
                    barcodeValue = barcode.displayValue;
                    Log.d(TAG, "Barcode read: " + barcode.displayValue);
                } else {
                    barcodeValue = null;
                    textViewCardStatus.setText(R.string.barcode_failure);
                    Log.d(TAG, "No barcode captured, intent data is null");
                }
            } else {
                barcodeValue = null;
                textViewCardStatus.setText(String.format(getString(R.string.barcode_error),
                        CommonStatusCodes.getStatusCodeString(resultCode)));
            }
        }
        else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    public void clickValidate(View view) {
        phoneNumber = editTextPhoneNumber.getText().toString();
        String phoneNumerConfirm = editTextPhoneNumberConfirm.getText().toString();

        if(!ValidatorUtil.isPhoneNumber(phoneNumber)){
            Toast.makeText(this, R.string.error_phone, Toast.LENGTH_SHORT).show();
            return;
        }

        if(!phoneNumber.trim().equals(phoneNumerConfirm.trim())){
            Toast.makeText(this, R.string.error_phone_same, Toast.LENGTH_SHORT).show();
            return;
        }

        if(FileUtil.checkFileExist(this, phoneNumber)){
            Toast.makeText(this, R.string.user_already_exist, Toast.LENGTH_SHORT).show();
            return;
        }


        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    android.Manifest.permission.SEND_SMS)) {

            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.SEND_SMS},
                        MY_PERMISSIONS_REQUEST_SEND_SMS);
            }
        }else{
            sendSms();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_SEND_SMS: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    sendSms();
                } else {
                    Toast.makeText(getApplicationContext(),
                            "SMS failed, please try again.", Toast.LENGTH_LONG).show();
                    return;
                }
            }
        }

    }

    private void sendSms(){
        new SensSms().execute();
    }

    private class SensSms extends AsyncTask<Void,Void, Boolean>{

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            buttonValidate.setEnabled(false);
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            try {
                FileUtil.saveFile(UserInfoActivity.this,phoneNumber, fmd);
            } catch (FileAlreadyExists fileAlreadyExists) {
                Log.e(TAG, "File already exists");
                return false;

            }
            SmsUtil.sendSms(UserInfoActivity.this,phoneNumber, barcodeValue );

            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            if(result!= null && result){
                Toast.makeText(UserInfoActivity.this, R.string.enrol_done, Toast.LENGTH_SHORT).show();
            }else{
                Toast.makeText(UserInfoActivity.this, R.string.enrol_failed, Toast.LENGTH_SHORT).show();
            }
            finish();
        }
    }

}
