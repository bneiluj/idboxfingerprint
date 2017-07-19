package io.idbox.fpsample.callback;

/**
 * Created by USER on 15/07/2017.
 */

public interface OnFingerprintCallback {

    /**
     * callback result of matching fingerprint
     * @param success if one fingerprint match
     */
    void result(boolean success);
}
