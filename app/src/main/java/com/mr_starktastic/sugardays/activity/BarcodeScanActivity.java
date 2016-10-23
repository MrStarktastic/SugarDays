package com.mr_starktastic.sugardays.activity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.mr_starktastic.sugardays.R;

import java.io.IOException;

public class BarcodeScanActivity extends AppCompatActivity implements SurfaceHolder.Callback {
    private CameraSource cameraSource;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barcode_scan);

        final DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        final int cameraSize = displayMetrics.widthPixels;

        final SurfaceView cameraView = (SurfaceView) findViewById(R.id.camera_view);
        cameraView.setLayoutParams(
                new FrameLayout.LayoutParams(cameraSize, cameraSize, Gravity.CENTER_VERTICAL));

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
                            final Barcode b = barcodes.valueAt(0);
                            Toast.makeText(BarcodeScanActivity.this,
                                    "Raw value: " + b.rawValue + "\nFormat: " + b.format,
                                    Toast.LENGTH_LONG).show();
                            barcodeDetector.release();
                        }
                    });
            }
        });

        cameraSource = new CameraSource.Builder(this, barcodeDetector)
                .setRequestedPreviewSize(cameraSize, cameraSize)
                .setAutoFocusEnabled(true)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setRequestedFps(15)
                .build();
        cameraView.getHolder().addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
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
