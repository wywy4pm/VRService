package com.vr.service;

import android.text.TextUtils;
import android.util.Log;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;

public class UdpHelper {
    public static final String TAG = Constant.LOG_TAG + "-UdpTest";
    public boolean isRunning = false;
    private DatagramSocket socket;

    public interface UdpListener {
        void onMessage(String message);
    }

    public UdpHelper(UdpListener udpListener) {
        setUdpListener(udpListener);
        start();
    }

    private UdpListener udpListener;

    public void setUdpListener(UdpListener udpListener) {
        this.udpListener = udpListener;
    }

    public void start() {
        Log.d(TAG, "start");
        new Thread(
                () -> {
                    int port = 8888;
                    //InetAddress address = InetAddress.getLocalHost();
                    //创建DatagramSocket对象
                    try {
                        if (socket == null) {
                            socket = new DatagramSocket(null);
                            socket.setReuseAddress(true);
                            socket.bind(new InetSocketAddress(port));
                        }
                        try {
                            isRunning = true;
                            byte[] buf = new byte[1024];  //定义byte数组
                            DatagramPacket packet = new DatagramPacket(buf, buf.length);  //创建DatagramPacket对象
                            while (isRunning) {
                                socket.receive(packet);  //通过套接字接收数据
                                String getMsg = new String(buf, 0, packet.getLength());
//                                Log.d(TAG, "getMsg = " + getMsg);
                                if (udpListener != null && !TextUtils.isEmpty(getMsg)) {
                                    udpListener.onMessage(getMsg);
                                }
                            }
//                            if (!socket.isClosed()) {
//                                socket.close();  //关闭套接字
//                                Log.d(TAG,"udp close");
//                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            Log.d(TAG, "Exception e = " + e);
                        }
                    } catch (SocketException e) {
                        e.printStackTrace();
                        Log.d(TAG, "SocketException e = " + e);
                    } finally {
                        if (socket != null) {
                            socket.close();
                        }
                    }
                }
        ).start();
    }

    public void stop() {
        Log.d(TAG, "stop");
        setRunning(false);
        if (socket != null && !socket.isClosed()) {
            socket.close();
            Log.d(TAG, "udp stop");
        }
    }

    private void setRunning(boolean running) {
        isRunning = running;
    }
}
