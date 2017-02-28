package com.penghaonan.homemonitorclient.cmd.transfer;

import android.text.TextUtils;

public class CmdRequest {
    /**
     * 请求id
     */
    public long id;
    /**
     * 命令
     */
    public String cmd;
    /**
     * 数据
     */
    public String data;
    /**
     * 语言
     */
    public String lang;


    /**
     * 有效请求
     */
    public boolean isValid() {
        return id > 0 && !TextUtils.isEmpty(cmd);
    }
}
