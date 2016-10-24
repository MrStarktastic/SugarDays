package com.mr_starktastic.sugardays.activity;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.FrameLayout;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.mr_starktastic.sugardays.R;
import com.mr_starktastic.sugardays.fragment.FoodDialogFragment;

import java.io.IOException;

public class BarcodeScanActivity extends AppCompatActivity implements SurfaceHolder.Callback {
    public static final String EXTRA_BARCODE_VALUE = "BARCODE_VALUE";

    private static final int RECOMMENDED_FPS = 15;

    private CameraSource cameraSource;

    /**
     * Converts a UPC-E barcode to UPC-A according to the following algorithm:
     * {@literal http://stackoverflow.com/questions/31539005/how-to-convert-a-upc-e-barcode-to-a-upc-a-barcode/31539006#31539006}.
     * Then converted to a GTIN-13 number by adding '0' to the left of the string till its length is 13.
     *
     * @param barcode {@link Barcode} object to convert to a GTIN-13 number.
     * @return GTIN-13 number as a {@link String}
     */
    private static String toGTIN_13(Barcode barcode) {
        final String rawVal = barcode.rawValue;

        if (barcode.format == Barcode.UPC_E) {
            final char n = rawVal.charAt(6);

            if (n == '0' || n == '1' || n == '2')
                return "0" + rawVal.substring(0, 2) + Character.toString(n) + "0000" + rawVal.substring(3, 5) + Character.toString(rawVal.charAt(7));
            else if (n == '3')
                return "0" + rawVal.substring(0, 3) + "00000" + rawVal.substring(4, 5) + Character.toString(rawVal.charAt(7));
            else if (n == '4')
                return "0" + rawVal.substring(0, 4) + "00000" + Character.toString(rawVal.charAt(5)) + Character.toString(rawVal.charAt(7));
            else // <=> n is in {5,...,9}
                return "0" + rawVal.substring(0, 5) + "0000" + Character.toString(n) + rawVal.charAt(7);
        }

        final StringBuilder builder = new StringBuilder(13);

        for (int i = 13 - rawVal.length(); i > 0; --i)
            builder.append('0');

        return builder.append(rawVal).toString();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barcode_scan);

        final DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        final int cameraSize, cameraGravity;

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            cameraSize = displayMetrics.widthPixels;
            cameraGravity = Gravity.CENTER_VERTICAL;
        } else {
            cameraSize = displayMetrics.heightPixels;
            cameraGravity = Gravity.CENTER_HORIZONTAL;
        }

        final SurfaceView cameraView = (SurfaceView) findViewById(R.id.camera_view);
        cameraView.setLayoutParams(
                new FrameLayout.LayoutParams(cameraSize, cameraSize, cameraGravity));

        final BarcodeDetector barcodeDetector = new BarcodeDetector.Builder(this)
                .setBarcodeFormats(Barcode.EAN_8 | Barcode.EAN_13 | Barcode.UPC_A | Barcode.UPC_E)
                .build();
        barcodeDetector.setProcessor(new Detector.Processor<Barcode>() {
            @Override
            public void release() {

            }

            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections) {
                final SparseArray<Barcode> barcodes = detections.getDetectedItems();

                if (barcodes.size() != 0)
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setResult(FoodDialogFragment.REQ_SCAN_BARCODE,
                                    new Intent().putExtra(EXTRA_BARCODE_VALUE,
                                            toGTIN_13(barcodes.valueAt(0))));
                            finish();
                        }
                    });
            }
        });

        cameraSource = new CameraSource.Builder(this, barcodeDetector)
                .setRequestedPreviewSize(cameraSize, cameraSize)
                .setAutoFocusEnabled(true)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setRequestedFps(RECOMMENDED_FPS)
                .build();
        cameraView.getHolder().addCallback(this);
    }

    // This activity starts with the hypothesis that all required permissions were granted
    @SuppressWarnings("MissingPermission")
    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        try {
            cameraSource.start(surfaceHolder);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        cameraSource.stop();
    }
}
