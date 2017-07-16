package io.idbox.fpsample.util;

/**
 * Validator utility
 * Created by yhuel on 10/07/17.
 */

public class ValidatorUtil {

    public static boolean isPhoneNumber(String val){
        return val != null && val.length() >3;
    }
}
