package com.nhancv.kurentoandroid.ui;

import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.gc.materialdesign.views.ButtonFlat;
import com.nhancv.kurentoandroid.util.NDialog;
import com.nhancv.kurentoandroid.R;

import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by nhancao on 9/18/16.
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getName();
    private static final int REQUEST_CAMERA = 0x01;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        checkCameraPermission();
    }


    /**
     * Method to check permission
     */
    void checkCameraPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            // Camera permission has not been granted.
            requestCameraPermission();
        }
    }

    /**
     * Method to request permission for camera
     */
    private void requestCameraPermission() {
        // Camera permission has not been granted yet. Request it directly.
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                REQUEST_CAMERA);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_CAMERA) {
            // BEGIN_INCLUDE(permission_result)
            // Received permission result for camera permission.
            Log.i(TAG, "Received response for Camera permission request.");

            // Check if the only required permission has been granted
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Camera permission has been granted, preview can be displayed
            } else {
                //Permission not granted
                Toast.makeText(MainActivity.this, "You need to grant camera permission to use camera", Toast.LENGTH_LONG).show();
            }

        }
    }

    @OnClick(R.id.btOne2One)
    public void btOnce2OneOnClick() {
        startActivity(new Intent(MainActivity.this, One2OneActivity.class));
    }

    @OnClick(R.id.btOne2Many)
    public void btOne2ManyOnClick() {
        Dialog d = NDialog.showDialog(this, R.layout.dialog_one2many, view -> {
            ButtonFlat btPresenter = (ButtonFlat) view.findViewById(R.id.btPresenter);
            ButtonFlat btViewer = (ButtonFlat) view.findViewById(R.id.btViewer);
            btPresenter.setOnClickListener(v -> {
                startActivity(new Intent(MainActivity.this, One2ManyPresenterActivity.class));
            });
            btViewer.setOnClickListener(v -> {
                startActivity(new Intent(MainActivity.this, One2ManyViewerActivity.class));
            });


        });
        d.show();
    }


}
