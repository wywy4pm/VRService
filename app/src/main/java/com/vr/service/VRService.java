package com.vr.service;


import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.pvr.tobservice.ToBServiceHelper;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Objects;

//adb shell am start-foreground-service com.vr.service/.VRService
public class VRService extends Service implements UdpHelper.UdpListener, SocketListener {
    private static final String TAG = Constant.LOG_TAG;
    private UdpHelper udpHelper;
    private SocketHelper socketHelper;
    private String ipAddress;
    private String port = "8889";
    private MediaProjection mediaProjection;
    private Handler handler;
    public static int width = 1920;
    public static int height = 1080;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        if (Constant.usePicoSdk) {
            ToBServiceHelper.getInstance().bindTobService(this);
        } else {
            Intent intent = new Intent(this, MediaProjectionActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");
//        checkInit();
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand intent = " + intent);

        setNotification();
//        checkInit();
        if (!Constant.usePicoSdk) {
            int resultCode = intent.getIntExtra("code", 0);
            Intent data = intent.getParcelableExtra("data");
            if (resultCode == -1 && data != null) {
                //第三步：获取mediaProjection
                MediaProjectionManager mMediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
                mediaProjection = mMediaProjectionManager.getMediaProjection(resultCode, data);
                checkInit();
            }
        } else {
            checkInit();
        }
        return START_STICKY;
    }

    public void screenOut() {
        if (mediaProjection == null) {
            Log.e(TAG, "media projection is null");
            return;
        }
        screenShot(mediaProjection);
    }

    public void setNotification() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(
                    "10001",
                    "VRService",
                    NotificationManager.IMPORTANCE_HIGH
            );
            notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(notificationChannel);
        }

        Notification notification = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            notification = new Notification.Builder(this, "10001")
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .build();
            notification.flags |= Notification.FLAG_NO_CLEAR;
            startForeground(1, notification);
        }
    }

    public void checkInit() {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        width = metrics.widthPixels;
        height = metrics.heightPixels;
        handler = new Handler();
        destroyUdp();
        initUdp();
        if (!TextUtils.isEmpty(ipAddress) && !TextUtils.isEmpty(port)) {
            destroySocket();
            initSocket(ipAddress, port);
        }
        Constant.deviceSN = Utils.readFromSD(Utils.SN_FILE);
    }

    @Override
    public void onMessage(String message) {
        if (!TextUtils.isEmpty(message)) {
//            String[] ipAndPort = message.split(":");
//            if (ipAndPort.length == 2 && !TextUtils.equals(ipAddress, ipAndPort[0])) {
//                ipAddress = ipAndPort[0];
//                port = ipAndPort[1];
//                destroySocket();
//                initSocket(ipAddress, port);
//            }
            if (!TextUtils.equals(ipAddress, message)) {
                ipAddress = message;
                destroySocket();
                initSocket(ipAddress, port);
            }
        }
    }

    private void initSocket(String ipAddress, String port) {
        if (socketHelper == null) {
            socketHelper = new SocketHelper(this, ipAddress, port, this);
        }
    }

    private void destroySocket() {
        if (socketHelper != null) {
            socketHelper.disconnect();
            socketHelper.destroyConnection();
            socketHelper = null;
        }
    }

    private void initUdp() {
        if (udpHelper == null) {
            udpHelper = new UdpHelper(this);
        }
    }

    private void destroyUdp() {
        if (udpHelper != null) {
            udpHelper.stop();
            udpHelper = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        destroySocket();
        destroyUdp();
        if (Constant.usePicoSdk) {
            ToBServiceHelper.getInstance().unBindTobService(this);
        }
    }

    @Override
    public void openApp(String pkgName) {
        Intent intent = getPackageManager().getLaunchIntentForPackage(pkgName);
        if (intent != null) {
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }

    @Override
    public void startScreen() {
        if (!Constant.usePicoSdk) {
            screenOut();
        } else {
            @SuppressLint("WrongConstant")
            ImageReader imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 60);
            try {
                ToBServiceHelper.getInstance().getServiceBinder().pbsStartRecordScreenBySurface(imageReader.getSurface(), width, height, 0);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            if (handler != null) {
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        //取最新的图片
                        Image image = imageReader.acquireLatestImage();
                        handleImage(image);
                    }
                },1000);
            }
        }
    }

    public void screenShot(MediaProjection mediaProjection){
        Objects.requireNonNull(mediaProjection);
        @SuppressLint("WrongConstant")
        ImageReader imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 60);
        VirtualDisplay virtualDisplay = mediaProjection.createVirtualDisplay("screen", width, height, 1, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,imageReader.getSurface(), null, null);
        if (handler != null) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Image image = imageReader.acquireLatestImage();
                    //释放 virtualDisplay,不释放会报错
                    virtualDisplay.release();
                    handleImage(image);
                }
            },1000);
        }
    }

    public void handleImage(Image image) {
        Bitmap bitmap = Utils.image2Bitmap(image);
        if (bitmap != null) {
            Log.d(TAG, "screenOut bitmap width = " + bitmap.getWidth() + " height = " + bitmap.getHeight());
//            Utils.saveBitmapToFile(this, bitmap);
            String screenBase64 = Utils.bitmapToBase64(bitmap);
            if (socketHelper != null) {
                MessageData messageData = Utils.getMessageData(this, screenBase64);
                socketHelper.sendMessage(new GsonBuilder().create().toJson(messageData));
            }
//            byte[] data = Base64.decode(screenBase64, Base64.DEFAULT);
//            Bitmap newBmp = BitmapFactory.decodeByteArray(data, 0, data.length);
//            Utils.saveBitmapToFile(this, newBmp);
        }
    }
}
