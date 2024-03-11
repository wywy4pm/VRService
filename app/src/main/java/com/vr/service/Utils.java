package com.vr.service;

import static com.vr.service.UdpHelper.TAG;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.Image;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.os.BatteryManager;
import android.os.Environment;
import android.os.RemoteException;
import android.util.Base64;
import android.util.Log;

import com.pvr.tobservice.ToBServiceHelper;
import com.pvr.tobservice.enums.PBS_DeviceControlEnum;
import com.pvr.tobservice.interfaces.IIntCallback;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Enumeration;

public class Utils {
    public static final String SN_FILE = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "deviceSN.json";

    public static Bitmap image2Bitmap(Image image) {
        if (image == null) {
            return null;
        }
        int width = image.getWidth();
        int height = image.getHeight();

        final Image.Plane[] planes = image.getPlanes();
        final ByteBuffer buffer = planes[0].getBuffer();
        int pixelStride = planes[0].getPixelStride();
        int rowStride = planes[0].getRowStride();
        int rowPadding = rowStride - pixelStride * width;

        Bitmap bitmap = Bitmap.createBitmap(width+ rowPadding / pixelStride , height, Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(buffer);
        image.close();
        return bitmap;
    }

    public static MessageData getMessageData(Context context, String rtmpUrl, String normalUrl) {
        MessageData messageData = new MessageData(MessageData.TAG_ANDROID, getIP(context), Constant.deviceSN, rtmpUrl, normalUrl);
        return messageData;
    }

    public static MessageData getMessageData(Context context, int powerValue) {
        MessageData messageData = new MessageData(MessageData.TAG_ANDROID, getIP(context), Constant.deviceSN, powerValue);
        return messageData;
    }

    public static MessageData getMessageData(Context context, String base64Data) {
        MessageData messageData = new MessageData(MessageData.TAG_ANDROID, getIP(context), Constant.deviceSN, base64Data);
        return messageData;
    }

    public static MessageData getCommonMessageData(Context context) {
        MessageData messageData = new MessageData(MessageData.TAG_ANDROID, getIP(context), Constant.deviceSN);
        return messageData;
    }

    /**
     * bitmap转为base64
     * @param bitmap
     * @return
     */
    public static String bitmapToBase64(Bitmap bitmap) {
        String result = null;
        ByteArrayOutputStream baos = null;
        try {
            if (bitmap != null) {
                baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);

                baos.flush();
                baos.close();

                byte[] bitmapBytes = baos.toByteArray();
                result = Base64.encodeToString(bitmapBytes, Base64.DEFAULT);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (baos != null) {
                    baos.flush();
                    baos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    public static String readFromSD(String filename) {
        StringBuilder sb = new StringBuilder();
        File file = new File(filename);
        if (file.exists()) {
            try {
                if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                    //打开文件输入流
                    FileInputStream input = new FileInputStream(filename);
                    byte[] temp = new byte[1024];

                    int len = 0;
                    //读取文件内容:
                    while ((len = input.read(temp)) > 0) {
                        sb.append(new String(temp, 0, len));
                    }
                    //关闭输入流
                    input.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }

    public static void saveBitmapToFile(Context context, Bitmap bitmap) {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            try {
                File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), System.currentTimeMillis() + ".png");
                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, bos);
                bos.flush();
                bos.close();
                refreshMediaStore(context, file);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void refreshMediaStore(Context context, File file) {
        if (file != null && file.exists()) {
            Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            Uri uri = Uri.fromFile(file);
            intent.setData(uri);
            context.sendBroadcast(intent);
        }
    }

    /**
     * 获取IP
     *
     * @param context
     * @return
     */
    public static String getIP(Context context) {
        String ip = "0.0.0.0";
        ConnectivityManager connectivityManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = connectivityManager.getActiveNetworkInfo();
        int type = info.getType();
        if (type == ConnectivityManager.TYPE_ETHERNET) {
            ip = getEtherNetIP();
        } else if (type == ConnectivityManager.TYPE_WIFI) {
            ip = getWifiIP(context);
        }
        return ip;
    }

    public static void saveData(byte[] data) {
        try {
            // 创建输出流对象并指定要写入的文件路径
            FileOutputStream fos = new FileOutputStream("sdcard/output");
            // 向文件中写入内容
            fos.write(data);
            // 关闭输出流
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取有线地址
     *
     * @return
     */
    public static String getEtherNetIP() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface
                    .getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf
                        .getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()
                            && inetAddress instanceof Inet4Address) {
                        return inetAddress.getHostAddress().toString();
                    }
                }
            }
        } catch (SocketException ex) {
            Log.d("WifiPreference IpAddress", ex.toString());
        }
        return "0.0.0.0";
    }

    /**
     * 获取wifiIP地址
     *
     * @param context
     * @return
     */
    public static String getWifiIP(Context context) {
        android.net.wifi.WifiManager wifi = (android.net.wifi.WifiManager) context
                .getSystemService(android.content.Context.WIFI_SERVICE);
        WifiInfo wifiinfo = wifi.getConnectionInfo();
        int intaddr = wifiinfo.getIpAddress();
        byte[] byteaddr = new byte[] { (byte) (intaddr & 0xff),
                (byte) (intaddr >> 8 & 0xff), (byte) (intaddr >> 16 & 0xff),
                (byte) (intaddr >> 24 & 0xff) };
        InetAddress addr = null;
        try {
            addr = InetAddress.getByAddress(byteaddr);
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        String mobileIp = addr.getHostAddress();
        return mobileIp;
    }

    public static void setVolumeIndex(Context context, int volumeIndex) {
        Log.d(TAG, "setVolumeIndex volumeIndex = " + volumeIndex);
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, volumeIndex, 0);
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volumeIndex, 0);
        }
    }

    public static int getDevicePower(Context context) {
        BatteryManager batteryManager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
        int curPower = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        Log.d(TAG, "getDevicePower curPower = " + curPower);
        return curPower;
    }

    public static void openApp(String pkgName) {
        if (ToBServiceHelper.getInstance().getServiceBinder() == null) {
            return;
        }
        try {
            Log.d(TAG, "openApp pbsStartActivity pkgName = " + pkgName);
            ToBServiceHelper.getInstance().getServiceBinder().pbsStartActivity(pkgName,"","","",null,new int[]{Intent.FLAG_ACTIVITY_NEW_TASK},0);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public static void rebootAndShutDown(boolean isReboot) {
        if (ToBServiceHelper.getInstance().getServiceBinder() == null) {
            return;
        }
        Log.d(TAG, "rebootAndShutDown isReboot = " + isReboot);
        //重启
        try {
            PBS_DeviceControlEnum type = isReboot ? PBS_DeviceControlEnum.DEVICE_CONTROL_REBOOT : PBS_DeviceControlEnum.DEVICE_CONTROL_SHUTDOWN;
            ToBServiceHelper.getInstance().getServiceBinder().pbsControlSetDeviceAction(type, new IIntCallback.Stub() {
                @Override
                public void callback(int result) throws RemoteException {
                    Log.d(TAG, "rebootAndShutDown callback: " + result);
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public static void setFreezeScreen(boolean freezeScreen) {
        if (ToBServiceHelper.getInstance().getServiceBinder() == null) {
            return;
        }
        Log.d(TAG, "setFreezeScreen freezeScreen = " + freezeScreen);
        try {
            ToBServiceHelper.getInstance().getServiceBinder().pbsFreezeScreen(freezeScreen);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}
