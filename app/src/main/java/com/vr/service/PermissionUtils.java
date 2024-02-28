package com.vr.service;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;

public class PermissionUtils {
    private static final int ACTION_OTHER_PERMISSION_REQUEST_CODE = 10001;
    public static final int ACTION_MANAGE_APP_FILE_PERMISSION_REQUEST_CODE = 10002;
    public static final String[] permissions = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    public static boolean hasPermission(Context context) {
        if (permissions != null) {
            ArrayList<String> toApplyList = new ArrayList<>();
            for (String perm : permissions) {
                int check = ContextCompat.checkSelfPermission(context, perm);
                Log.d(Constant.LOG_TAG, "hasPermission perm = " + perm + " check = " + check);
                if (PackageManager.PERMISSION_GRANTED != check) {
                    toApplyList.add(perm);
                }
            }
            return toApplyList.size() <= 0;
        }
        return false;
    }

    public static boolean requestPermission(Activity context) {
        if (permissions != null) {
            ArrayList<String> toApplyList = new ArrayList<String>();

            for (String perm : permissions) {
                int check = ContextCompat.checkSelfPermission(context, perm);
                Log.d(Constant.LOG_TAG, "hasPermission perm = " + perm + " check = " + check);
                if (PackageManager.PERMISSION_GRANTED != check) {
                    toApplyList.add(perm);
                }
            }
            String[] tmpList = new String[toApplyList.size()];
            if (!toApplyList.isEmpty()) {
                ActivityCompat.requestPermissions(context, toApplyList.toArray(tmpList), ACTION_OTHER_PERMISSION_REQUEST_CODE);
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    public static boolean handleRequestResult(int requestCode, int resultCode) {
        if (requestCode == ACTION_OTHER_PERMISSION_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            return true;
        }
        return false;
    }

    public static boolean checkManagerFilePermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + activity.getPackageName()));
                activity.startActivityForResult(intent, ACTION_MANAGE_APP_FILE_PERMISSION_REQUEST_CODE);
                return true;
            }
        }
        return false;
    }
}
