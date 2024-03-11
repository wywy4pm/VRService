package com.vr.service;


import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.AudioManager;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.gson.GsonBuilder;
import com.pvr.tobservice.ToBServiceHelper;
import com.pvr.tobservice.enums.PBS_PICOCastUrlTypeEnum;
import com.pvr.tobservice.interfaces.IIntCallback;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

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
    private ImageReader imageReader;
    private VirtualDisplay virtualDisplay;
    private boolean needOnePicture = false;
    private Timer timer;
    private TimerTask timerTask;

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
        width = 1280;
        height = 640;
        handler = new Handler();
        destroyUdp();
        initUdp();
        if (!TextUtils.isEmpty(ipAddress) && !TextUtils.isEmpty(port)) {
            destroySocket();
            initSocket(ipAddress, port);
        }
        Constant.deviceSN = Utils.readFromSD(Utils.SN_FILE);
        startTimerTask();
//        try {
//            ToBServiceHelper.getInstance().getServiceBinder().pbsAppKeepAlive(BuildConfig.APPLICATION_ID,true,0);
//        } catch (RemoteException e) {
//            e.printStackTrace();
//        }
    }

    @Override
    public void onMessage(String message) {
        if (!TextUtils.isEmpty(message)) {
            if (!TextUtils.equals(ipAddress, message)) {
                ipAddress = message;
                Log.d(TAG,"ipAddress = " + ipAddress + " port = " + port);
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
        stopScreen();
        cancelTimer();
    }

    @Override
    public void openApp(String pkgName) {
        Log.d(TAG, "openApp");
        if (!Constant.usePicoSdk) {
            Intent intent = getPackageManager().getLaunchIntentForPackage(pkgName);
            if (intent != null) {
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        } else {
            Utils.openApp(pkgName);
        }
    }

    @SuppressLint("WrongConstant")
    @Override
    public void startScreen() {
        Log.d(TAG,"startScreen");
        if (!Constant.usePicoSdk) {
            screenOut();
        } else {
//            imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 60);
//            try {
//                ToBServiceHelper.getInstance().getServiceBinder().pbsStartRecordScreenBySurface(imageReader.getSurface(), width, height, 0);
//            } catch (RemoteException e) {
//                Log.d(TAG,"startScreen e = " + e.toString());
//                e.printStackTrace();
//            }
//            if (needOnePicture) {
//                if (handler != null) {
//                    handler.postDelayed(new Runnable() {
//                        @Override
//                        public void run() {
//                            //取最新的图片
//                            Image image = imageReader.acquireLatestImage();
//                            handleImage(image);
//                        }
//                    },1000);
//                }
//            } else {
//                handlerMuImage();
//            }
            if (ToBServiceHelper.getInstance().getServiceBinder() == null) {
                return;
            }
            try {
                ToBServiceHelper.getInstance().getServiceBinder().pbsPicoCastInit(new IIntCallback() {
                    @Override
                    public void callback(int i) throws RemoteException {
                        Log.d(TAG,"startScreen pbsPicoCastInit callback i = " + i);
                    }

                    @Override
                    public IBinder asBinder() {
                        return null;
                    }
                },0);
                ToBServiceHelper.getInstance().getServiceBinder().pbsPicoCastSetShowAuthorization(1, 0);
                String rtmpUrl = ToBServiceHelper.getInstance().getServiceBinder().pbsPicoCastGetUrl(PBS_PICOCastUrlTypeEnum.RTMP_URL, 0);
                String normalUrl = ToBServiceHelper.getInstance().getServiceBinder().pbsPicoCastGetUrl(PBS_PICOCastUrlTypeEnum.NORMAL_URL, 0);
                Log.d(TAG, "startScreen pbsPicoCastGetUrl rtmpUrl = " + rtmpUrl);
                Log.d(TAG, "startScreen pbsPicoCastGetUrl normalUrl = " + normalUrl);
                MessageData messageData = Utils.getMessageData(this, rtmpUrl, normalUrl);
                if (socketHelper != null) {
                    socketHelper.sendMessage(messageData);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void stopScreen() {
        Log.d(TAG,"stopScreen");
//        if (virtualDisplay != null) {
//            virtualDisplay.release();
//        }
//        if (imageReader != null) {
//            imageReader.close();
//        }
        if (ToBServiceHelper.getInstance().getServiceBinder() != null) {
            try {
                int result = ToBServiceHelper.getInstance().getServiceBinder().pbsPicoCastStopCast(0);
                Log.d(TAG,"stopScreen pbsPicoCastStopCast result = " + result);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void shutDownAndReBoot(boolean isReboot) {
        Utils.rebootAndShutDown(isReboot);
    }

    @Override
    public void setVolume(int volumeIndex) {
        Utils.setVolumeIndex(this, volumeIndex);
    }

    @Override
    public void setFreezeScreen(boolean freezeScreen) {
        Utils.setFreezeScreen(freezeScreen);
    }

    private void cancelTimer() {
        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    private void startTimerTask() {
        cancelTimer();
        if (timer == null) {
            timer = new Timer();
        }
        timerTask = new TimerTask() {
            @Override
            public void run() {
                sendPowerMsg();
            }
        };
        timer.schedule(timerTask, 1000, 2 * 1000);
        Log.d(TAG,"startTimerTask");
    }

    public void sendPowerMsg() {
        if (socketHelper != null) {
            int power = Utils.getDevicePower(this);
            MessageData messageData = Utils.getMessageData(this, power);
            socketHelper.sendMessage(messageData);
        }
    }

    @SuppressLint("WrongConstant")
    public void screenShot(MediaProjection mediaProjection){
//        Objects.requireNonNull(mediaProjection);
//        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 60);
//        virtualDisplay = mediaProjection.createVirtualDisplay("screen", width, height, 1, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,imageReader.getSurface(), null, null);
//        if (needOnePicture) {
//            if (handler != null) {
//                handler.postDelayed(new Runnable() {
//                    @Override
//                    public void run() {
//                        Image image = imageReader.acquireLatestImage();
//                        //释放 virtualDisplay,不释放会报错
//                        virtualDisplay.release();
//                        handleImage(image);
//                    }
//                }, 1000);
//            }
//        } else {
//            handlerMuImage();
//        }
    }

    public void handlerMuImage() {
        imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                Image image = reader.acquireLatestImage();
                handleImage(image);
                image.close();
            }
        }, handler);
    }

//    long countTime = 0;
//    int fps = 0;
    byte[] data;

    public void handleImage(Image image) {
        Log.d(TAG,"handleImage");
//        Bitmap bitmap = Utils.image2Bitmap(image);
        if (image != null) {
//            if (System.currentTimeMillis() - countTime >= 1000) {
//                Log.d(TAG, "handleImage fps = " + fps);
//                countTime = System.currentTimeMillis();
//                fps = 0;
//            } else {
//                fps++;
//            }
//            Utils.saveBitmapToFile(this, bitmap);
//            String screenBase64 = Utils.bitmapToBase64(bitmap);
            Image.Plane[] planes = image.getPlanes();
            ByteBuffer buffer = planes[0].getBuffer();
            Log.d(TAG, "handleImage screenOut image width = " + image.getWidth() + " height = " + image.getHeight() + " remaining = " + buffer.remaining());
            if (data == null) {
                data = new byte[buffer.remaining() + 4];
            }
            buffer.get(data, 3, buffer.remaining());
            if (socketHelper != null) {
//                MessageData messageData = Utils.getMessageData(this, screenBase64);
                socketHelper.sendMessage(data);
            }
//            byte[] data = Base64.decode(screenBase64, Base64.DEFAULT);
//            Bitmap newBmp = BitmapFactory.decodeByteArray(data, 0, data.length);
//            Utils.saveData(data);
        }
    }
}
