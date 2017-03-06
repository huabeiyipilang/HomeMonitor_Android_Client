package com.penghaonan.homemonitorclient.cmd.handler;

import com.penghaonan.homemonitorclient.cmd.CommandData;
import com.penghaonan.homemonitorclient.cmd.transfer.CmdRequest;
import com.penghaonan.homemonitorclient.cmd.transfer.CmdRequestListener;
import com.penghaonan.homemonitorclient.cmd.transfer.CmdResponse;

public abstract class ACmdHandler {
    private String serverId;
    private CommandData cmdData;
    private CmdRequestListener listener;
    private CmdRequest request;


    public abstract void onResponseReceived(CmdResponse response);

    /**
     * @return false不处理
     */
    public boolean onImageReceived(String remoteUrl){
        return false;
    }

    public String getServerId() {
        return serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    public CommandData getCmdData() {
        return cmdData;
    }

    public void setCmdData(CommandData cmdData) {
        this.cmdData = cmdData;
    }

    public CmdRequestListener getListener() {
        return listener;
    }

    public void setListener(CmdRequestListener listener) {
        this.listener = listener;
    }

    public CmdRequest getRequest() {
        return request;
    }

    public void setRequest(CmdRequest request) {
        this.request = request;
    }

}
