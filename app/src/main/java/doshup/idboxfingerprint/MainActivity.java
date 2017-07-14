package doshup.idboxfingerprint;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import asia.kanopi.fingerscan.Status;
import java.io.File;

public class MainActivity extends AppCompatActivity {

  // Used to load the 'native-lib' library on application startup.
  static {
    System.loadLibrary("native-lib");
  }

  ImageView ivFinger;
  TextView tvMessage;
  byte[] img;
  Bitmap bm;
  private static final int SCAN_FINGER = 0;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    //        // Example of a call to a native method
    //        TextView tv = (TextView) findViewById(R.id.sample_text);
    //        tv.setText(stringFromJNI());
    tvMessage = (TextView) findViewById(R.id.tvMessage);
    ivFinger = (ImageView) findViewById(R.id.ivFingerDisplay);

    getMyFilesDir();
  }


  private void getMyFilesDir() {
    File rootDataDir = getExternalFilesDir("Testing");
    String img1 = rootDataDir + File.separator + "101_1.tif";
    String img2 = rootDataDir + File.separator + "101_2.tif";

    Log.d("Testing", rootDataDir.exists()+"");
    Log.d("Testing", img1);
    Log.d("Testing", img2);

    isSameFingerprint(img1, img2);
  }

  /**
   * A native method that is implemented by the 'native-lib' native library,
   * which is packaged with this application.
   */
  //    public native String stringFromJNI();
  public void startScan(View view) {
    Intent intent = new Intent(this, ScanActivity.class);
    startActivityForResult(intent, SCAN_FINGER);
  }

  @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
    int status;
    String errorMesssage;
    switch (requestCode) {
      case (SCAN_FINGER): {
        if (resultCode == RESULT_OK) {
          status = data.getIntExtra("status", Status.ERROR);
          if (status == Status.SUCCESS) {
            tvMessage.setText("Fingerprint captured");
            img = data.getByteArrayExtra("img");
            bm = BitmapFactory.decodeByteArray(img, 0, img.length);
            ivFinger.setImageBitmap(bm);
          } else {
            errorMesssage = data.getStringExtra("errorMessage");
            tvMessage.setText("-- Error: " + errorMesssage + " --");
          }
        }
        break;
      }
    }
  }



   public void isSameFingerprint(String img1, String img2) {
    String testFinger = nativeIsSameFinger(img1, img2);


    Log.d("Testing", "It was a " + testFinger);




//     File file = new File(Environment.getExternalStorageDirectory() + "/" +Environment.DIRECTORY_DOWNLOADS + "/", fileName);
   }


   public native String nativeIsSameFinger(String img1, String img2);



//    Array Images = "/path/folder/img";
//    for (var i in Images){
//      int score = 0;
//      var scoreNew = compareFromCLib(currentFingerprint, Images[i]);
//      if (scoreNew > score) {
//
//      }
//      if (higherScore > 5000) {
//        //found match!
//      } else {
//        // not found - save image inside
//      }
//    }

}
