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
    public String rtmpUrl;
    public String normalUrl;
    public String noConfirmUrl;
    public int volumeIndex;
    public int powerValue;
    public int localPort;
    public static final int MSG_TYPE_CLOSE_APP = 1;
    public static final int MSG_TYPE_SCREEN_OUT = 2;
    public static final int MSG_TYPE_SCREEN_OUT_STOP = 3;
    public static final int MSG_TYPE_SHUTDOWN = 4;
    public static final int MSG_TYPE_REBOOT = 5;
    public static final int MSG_TYPE_VOLUME_SET = 6;
    public static final int MSG_TYPE_FREEZE_SCREEN = 7;
    public static final int MSG_TYPE_FREEZE_SCREEN_CANCEL = 8;

    public MessageData(String tag, String IP, String devSN) {
        Tag = tag;
        this.IP = IP;
        DevSN = devSN;
    }

    public MessageData(String tag, String IP, String devSN, String rtmpUrl, String normalUrl, String noConfirmUrl) {
        Tag = tag;
        this.IP = IP;
        DevSN = devSN;
        this.rtmpUrl = rtmpUrl;
        this.normalUrl = normalUrl;
        this.noConfirmUrl = noConfirmUrl;
    }

    public MessageData(String tag, String IP, String devSN, int powerValue) {
        Tag = tag;
        this.IP = IP;
        DevSN = devSN;
        this.powerValue = powerValue;
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
