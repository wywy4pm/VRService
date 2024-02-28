package com.vr.service;

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
}
