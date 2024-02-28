package com.vr.service;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.IOException;

public class MediaProjectionActivity extends AppCompatActivity {
    private static final String TAG = Constant.LOG_TAG;
    private static final int SCREEN_SHOT_CODE = 1116;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permission);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!Constant.usePicoSdk) {
            boolean checkManagerFilePermission = PermissionUtils.checkManagerFilePermission(this);
            if (!checkManagerFilePermission) {
                boolean hasPermission = PermissionUtils.hasPermission(this);
                if (!hasPermission) {
                    PermissionUtils.requestPermission(this);
                } else {
                    goCaptureIntent();
                }
            }
        } else {
            startService(0, null);
        }
    }

    public void goCaptureIntent() {
        Log.d(TAG,"goCaptureIntent");
        //第一步.调起系统捕获屏幕的Intent
        MediaProjectionManager mMediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        Intent captureIntent = mMediaProjectionManager.createScreenCaptureIntent();
        startActivityForResult(captureIntent, SCREEN_SHOT_CODE);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult requestCode = " + requestCode + " resultCode = " + resultCode);
        if (resultCode == RESULT_OK) {
            if (requestCode == SCREEN_SHOT_CODE) {
                //第二步通过startForegroundService来获取mediaProjection
                startService(resultCode, data);
            } else {
                boolean success = PermissionUtils.handleRequestResult(requestCode, resultCode);
                if (success) {
                    goCaptureIntent();
                }
            }
        }
    }

    public void startService(int resultCode, Intent data) {
        Intent service = new Intent(this, VRService.class);
        service.putExtra("code", resultCode);
        service.putExtra("data", data);
        startForegroundService(service);
        finish();
    }
}