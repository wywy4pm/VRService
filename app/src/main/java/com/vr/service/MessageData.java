package com.vr.service;

import com.google.gson.Gson;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class MessageData {
    public static final String TAG_ANDROID = "vr-android";
    public String Tag;
    public String IP;
    public String DevSN;
    public int vrMsgType;
    public String openAppPkgName;
    public String imageBase64Data;
    public static final int MSG_TYPE_CLOSE_APP = 1;
    public static final int MSG_TYPE_SCREEN_OUT = 2;
    public static final int MSG_TYPE_SCREEN_OUT_STOP = 3;

    public MessageData(String tag, String IP, String devSN) {
        Tag = tag;
        this.IP = IP;
        DevSN = devSN;
    }

    public MessageData(String tag, String IP, String devSN, String imageBase64Data) {
        Tag = tag;
        this.IP = IP;
        DevSN = devSN;
        this.imageBase64Data = imageBase64Data;
    }

    public byte[] pack() {

        byte[] body = new Gson().toJson(this).getBytes();
        ByteBuffer bb = ByteBuffer.allocate(body.length + 4);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.putInt(body.length);
        bb.put(body);
        return bb.array();
    }
}
