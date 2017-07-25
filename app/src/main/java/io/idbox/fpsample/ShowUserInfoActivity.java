package io.idbox.fpsample;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.vision.barcode.Barcode;

import io.idbox.fpsample.exception.FileAlreadyExists;
import io.idbox.fpsample.util.Constants;
import io.idbox.fpsample.util.FileUtil;
import io.idbox.fpsample.util.SmsUtil;
import io.idbox.fpsample.util.ValidatorUtil;

/**
 * Create by IDbox
 */
public class ShowUserInfoActivity extends AppCompatActivity {

    private static final int MY_PERMISSIONS_REQUEST_SEND_SMS = 402;
    private static final String TAG = Constants.TAG_PREFIX + "UserInfoActivity";
    private static final int RC_BARCODE_CAPTURE = 9001;

    private String barcodeValue;
    private EditText editTextPhoneNumber;
    private TextView textViewCardStatus;
    private Button buttonValidate;
    private String phoneNumber, savedPhoneNumber;
    private byte[] fmd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_user);

        Intent intent = getIntent();
        if(intent == null || !intent.hasExtra(Constants.EXTRA_PHONE_NUMBER)
                || !intent.hasExtra(Constants.EXTRA_FMD)){
            Log.e(TAG, "not the right extras in intent, so close");
            finish();
        }
        fmd = intent.getByteArrayExtra(Constants.EXTRA_FMD);
        savedPhoneNumber = intent.getStringExtra(Constants.EXTRA_PHONE_NUMBER);
        editTextPhoneNumber = (EditText) findViewById(R.id.editTextPhoneNumber);
        textViewCardStatus = (TextView) findViewById(R.id.textViewCardStatus);
        buttonValidate = (Button) findViewById(R.id.buttonValidate);

        editTextPhoneNumber.setText(savedPhoneNumber);
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

        if(!ValidatorUtil.isPhoneNumber(phoneNumber)){
            Toast.makeText(this, R.string.error_phone, Toast.LENGTH_SHORT).show();
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
        if (!TextUtils.equals(savedPhoneNumber, phoneNumber) || barcodeValue != null) {
            new SensSms().execute();
        } else {
            Toast.makeText(ShowUserInfoActivity.this, "You must update number or attach QR code",
                    Toast.LENGTH_LONG).show();
        }
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
                if (!TextUtils.equals(savedPhoneNumber, phoneNumber)) {
                    FileUtil.deleteFile(ShowUserInfoActivity.this, savedPhoneNumber);
                    FileUtil.saveFile(ShowUserInfoActivity.this, phoneNumber, fmd);
                }
            } catch (FileAlreadyExists fileAlreadyExists) {
                Log.e(TAG, "File already exists");
                return false;

            }
            SmsUtil.sendSmsToServer(ShowUserInfoActivity.this,phoneNumber, barcodeValue);
            if (getSharedPreferences(Constants.PREF_FILE, 0)
                    .getBoolean(Constants.PREF_KEY_CONFIRMATION, false)) {
                String msg = getString(R.string.sms_id_updated);
                SmsUtil.sendSmsToUser(ShowUserInfoActivity.this, phoneNumber, msg);
            }

            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            if(result!= null && result){
                Toast.makeText(ShowUserInfoActivity.this, R.string.show_user_done, Toast.LENGTH_SHORT).show();
            }else{
                Toast.makeText(ShowUserInfoActivity.this, R.string.show_user_failed, Toast.LENGTH_SHORT).show();
            }
            finish();
        }
    }

}
