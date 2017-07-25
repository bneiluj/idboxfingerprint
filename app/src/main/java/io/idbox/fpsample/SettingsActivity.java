package io.idbox.fpsample;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import io.idbox.fpsample.util.Constants;
import io.idbox.fpsample.util.ValidatorUtil;

public class SettingsActivity extends AppCompatActivity {

    private EditText editTextPhoneNumber;
    private EditText editTextServerNumber;
    private EditText editTextCurrency;
    private CheckBox checkBoxConfirmation;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);


        // Restore preferences
        sharedPreferences = getSharedPreferences(Constants.PREF_FILE, 0);
        String phone = sharedPreferences.getString(Constants.PREF_KEY_PHONE, null);
        String server = sharedPreferences.getString(Constants.PREF_KEY_SERVER, null);
        String currency = sharedPreferences.getString(Constants.PREF_KEY_CURRENCY, null);
        boolean confirmation = sharedPreferences.getBoolean(Constants.PREF_KEY_CONFIRMATION, false);

        editTextPhoneNumber = (EditText)findViewById(R.id.editTextPhoneNumber);
        editTextServerNumber = (EditText)findViewById(R.id.editTextServerNumber);
        editTextCurrency = (EditText)findViewById(R.id.editTextCurrency);
        checkBoxConfirmation = (CheckBox)findViewById(R.id.checkboxSendUserConfirmation);


        if(phone!=null){
            editTextPhoneNumber.setText(phone);
        }
        if(server!=null){
            editTextServerNumber.setText(server);
        }
        if(currency!=null){
            editTextCurrency.setText(currency);
        }
        checkBoxConfirmation.setChecked(confirmation);
    }

    public void clickValidate(View view){


        String phoneNumber = editTextPhoneNumber.getText().toString();
        String serverNumber = editTextServerNumber.getText().toString();

        if(!ValidatorUtil.isPhoneNumber(phoneNumber)){
            Toast.makeText(this, R.string.error_sim_number, Toast.LENGTH_SHORT).show();
            return;
        }

        if(!ValidatorUtil.isPhoneNumber(serverNumber)){
            Toast.makeText(this, R.string.error_server_number, Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(Constants.PREF_KEY_PHONE, phoneNumber);
        editor.putString(Constants.PREF_KEY_SERVER, serverNumber);
        editor.putString(Constants.PREF_KEY_CURRENCY, editTextCurrency.getText().toString());
        editor.putBoolean(Constants.PREF_KEY_CONFIRMATION, checkBoxConfirmation.isChecked());

        // Commit the edits!
        editor.apply();

        finish();
    }
}
