package com.penghaonan.homemonitorclient.cmd.transfer;

public interface CmdRequestListener {
    void onProgress(String msg);

    void onImageReceived(String url);

    void onFinished(int code, String msg);
}
