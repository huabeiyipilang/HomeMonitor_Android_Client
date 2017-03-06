package com.penghaonan.homemonitorclient.cmd.handler;

public class CmdHandlerFactory {
    public static ACmdHandler createCmdHandler(String cmd) {
        if ("torch".equals(cmd)) {
            return new TorchHandler();
        }else if ("takepic".equals(cmd)) {
            return null;
        }else {
            return null;
        }
    }
}
