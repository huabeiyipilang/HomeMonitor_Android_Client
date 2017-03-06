package com.penghaonan.homemonitorclient.cmd.handler;

import com.penghaonan.homemonitorclient.cmd.transfer.CmdResponse;

public class TakePicHandler extends ACmdHandler {
    @Override
    public void onResponseReceived(CmdResponse response) {

    }

    @Override
    public boolean onImageReceived(String remoteUrl) {
        return true;
    }
}
