package com.vr.service;

public interface SocketListener {
    void openApp(String pkgName);

    void startScreen();

    void stopScreen();

    void shutDownAndReBoot(boolean isReboot);

    void setVolume(int volumeIndex);

    void setFreezeScreen(boolean freezeScreen);
}
