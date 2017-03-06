package com.penghaonan.homemonitorclient.cmd;

import com.alibaba.fastjson.JSON;
import com.penghaonan.homemonitorclient.cmd.transfer.CmdRequest;

public class CommandData {
    public String command;
    public String description;
    public long updateTime;

    public CmdRequest createRequest() {
        return JSON.parseObject(command, CmdRequest.class);
    }
}
