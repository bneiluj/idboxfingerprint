package io.idbox.fpsample.barcode;

import com.google.android.gms.vision.barcode.Barcode;

/**
 * Listener when barcode detected
 * Created by yhuel on 10/07/17.
 */

public interface OnBarcodeDetectListener {

    void newBarcode(Barcode barcode);
}
