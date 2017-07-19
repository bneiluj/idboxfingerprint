package io.idbox.fpsample.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.klinker.android.send_message.Message;
import com.klinker.android.send_message.Settings;
import com.klinker.android.send_message.Transaction;

/**
 * Created by IDbox on 15/07/2017.
 */

public class SmsUtil {

    private static final String TAG = Constants.TAG_PREFIX + "SmsUtil";

    public static void sendSms(Context ctx, String phoneNumber, String qrCodeData){
        String msg = formatSms(phoneNumber, qrCodeData);

        SharedPreferences sharedPreferences = ctx.getSharedPreferences(Constants.PREF_FILE, 0);
        String phone = sharedPreferences.getString(Constants.PREF_KEY_SERVER, null);
        Settings settings = new Settings();
        settings.setUseSystemSending(true);
        Transaction transaction = new Transaction(ctx, settings);
        Message message = new Message(msg, phone);
        transaction.sendNewMessage(message, Transaction.NO_THREAD_ID);
        Log.d(TAG, "sending sms to " + phone );
    }

    private static final String formatSms(String phoneNumber, String qrCodeData){
        String msg = "r(p("+phoneNumber;
        if(qrCodeData!= null && !qrCodeData.isEmpty()){
            msg += "),q("+qrCodeData;
        }
        msg += "))";
        return msg;
    }


}
