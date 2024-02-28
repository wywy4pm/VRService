package com.vr.service;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(Constant.LOG_TAG,"BootReceiver onReceive action = " + action);
//        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
//            Intent toIntent = new Intent(context, VRService.class);
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                context.startForegroundService(toIntent);
//            } else {
//                context.startService(toIntent);
//            }
//        }
    }

//    public static void StartCheckService(Activity activity) {
//        PackageManager packageManager = activity.getPackageManager();
//        Intent intent = packageManager.getLaunchIntentForPackage("com.vr.service");
//        intent.setClassName("com.vr.service","com.vr.service.VRService");
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            myActivity.startForegroundService(intent);
//        } else {
//            myActivity.startService(intent);
//        }
//    }
}
