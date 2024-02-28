package com.vr.service;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.easysocket.EasySocket;
import com.easysocket.config.EasySocketOptions;
import com.easysocket.entity.OriginReadData;
import com.easysocket.entity.SocketAddress;
import com.easysocket.interfaces.conn.ISocketActionListener;
import com.easysocket.interfaces.conn.SocketActionListener;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.nio.charset.StandardCharsets;

public class SocketHelper {
    private static final String TAG = Constant.LOG_TAG;
    private Context context;
    private SocketListener socketListener;

    public SocketHelper(Context context, String ipAddress, String port, SocketListener socketListener) {
        this.context = context;
        this.socketListener = socketListener;
        initEasySocket(ipAddress, port);
    }

    /**
     * 初始化EasySocket
     */
    private void initEasySocket(String ipAddress, String port) {
        //socket配置
        EasySocketOptions options = new EasySocketOptions.Builder()
                .setSocketAddress(new SocketAddress(ipAddress, TextUtils.isEmpty(port) ? 8889 : Integer.parseInt(port))) //主机地址
                // 强烈建议定义一个消息协议，方便解决 socket黏包、分包的问题
//                .setReaderProtocol(new DefaultMessageProtocol()) // 默认的消息协议
                .build();
        //初始化EasySocket
        EasySocket.getInstance().createConnection(options, context);
        //监听socket相关行为
        EasySocket.getInstance().subscribeSocketAction(socketActionListener);
    }

    /**
     * socket行为监听
     */
    private ISocketActionListener socketActionListener = new SocketActionListener() {
        /**
         * socket连接成功
         * @param socketAddress
         */
        @Override
        public void onSocketConnSuccess(SocketAddress socketAddress) {
            super.onSocketConnSuccess(socketAddress);
            Log.d(TAG, "onSocketConnSuccess");
            MessageData messageData = Utils.getCommonMessageData(context);
            String message = new GsonBuilder().create().toJson(messageData);
            sendMessage(message);
        }

        /**
         * socket连接失败
         * @param socketAddress
         * @param isReconnect 是否需要重连
         */
        @Override
        public void onSocketConnFail(SocketAddress socketAddress, boolean isReconnect) {
            super.onSocketConnFail(socketAddress, isReconnect);
            Log.d(TAG, "onSocketConnFail isReconnect = " + isReconnect);
        }

        /**
         * socket断开连接
         * @param socketAddress
         * @param isReconnect 是否需要重连
         */
        @Override
        public void onSocketDisconnect(SocketAddress socketAddress, boolean isReconnect) {
            super.onSocketDisconnect(socketAddress, isReconnect);
            Log.d(TAG, "onSocketDisconnect");
        }

        /**
         * socket接收的数据
         * @param socketAddress
         * @param originReadData
         */
        @Override
        public void onSocketResponse(SocketAddress socketAddress, OriginReadData originReadData) {
            super.onSocketResponse(socketAddress, originReadData);
            Log.d(TAG, "onSocketResponse = " + originReadData.getBodyString());
            handleMessage(originReadData.getBodyString());
        }
    };

    /**
     * 发送一个的消息，
     */
    public void sendMessage(String message) {
        //发送
        EasySocket.getInstance().upMessage(message.getBytes());
    }

    public void connect() {
        //连接Socket
        EasySocket.getInstance().connect();
    }

    public void disconnect() {
        //断开当前的Socket连接，参数false表示当前断开不需要自动重连
        EasySocket.getInstance().disconnect(false);
    }

    public void destroyConnection() {
        EasySocket.getInstance().destroyConnection();
    }

    private void handleMessage(String message) {
        if (!TextUtils.isEmpty(message)) {
            try {
                MessageData messageData = new GsonBuilder().create().fromJson(message, new TypeToken<MessageData>() {
                }.getType());
                if (socketListener != null && messageData != null) {
                    if (messageData.vrMsgType == MessageData.MSG_TYPE_CLOSE_APP) {
                        socketListener.openApp(messageData.openAppPkgName);
                    } else if (messageData.vrMsgType == MessageData.MSG_TYPE_SCREEN_OUT) {
                        socketListener.startScreen();
                    }
                }
            } catch (Exception e) {
                Log.e(TAG,"handleMessage e = " + e);
                e.printStackTrace();
            }
        }
    }
}
