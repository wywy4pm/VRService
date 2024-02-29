package com.vr.service;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;

public class EmptyActivity extends AppCompatActivity {
    public static final int OVERLAY_CODE = 1001;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_empty);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isEnableOverLay()) {
            startService();
        }
    }

    public void startService() {
        Intent service = new Intent(this, VRService.class);
        startForegroundService(service);
        finish();
    }

    public boolean isEnableOverLay() {
        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, OVERLAY_CODE);
            return false;
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
//        if (requestCode == OVERLAY_CODE && resultCode == RESULT_OK) {
//            finish();
//        }
    }
}