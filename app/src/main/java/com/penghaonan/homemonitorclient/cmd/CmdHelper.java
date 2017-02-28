package com.penghaonan.homemonitorclient.cmd;

import android.content.Context;
import android.content.SharedPreferences;

import com.alibaba.fastjson.JSON;
import com.hyphenate.chat.EMClient;
import com.hyphenate.exceptions.EMNoActiveCallException;
import com.penghaonan.appframework.AppDelegate;
import com.penghaonan.appframework.utils.CollectionUtils;
import com.penghaonan.appframework.utils.Logger;
import com.penghaonan.appframework.utils.StringUtils;
import com.penghaonan.homemonitorclient.App;
import com.penghaonan.homemonitorclient.cmd.transfer.CmdRequest;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CmdHelper implements App.ICallReceiverListener {
    public static CommandData sGetProfileCmd;
    private SharedPreferences pref = AppDelegate.getApp().getSharedPreferences("cmd_cache", Context.MODE_PRIVATE);
    private String mServerName;
    private Map<String, CommandData> mCommands = new HashMap<>();
    private OnCmdChangedListener mListener;

    public CmdHelper(String server) {
        mServerName = server;
        loadCmdFromCache();
        App.getInstance().addCallReceiverListener(this);
    }

    public static CommandData getProfileCmd() {
        if (sGetProfileCmd == null) {
            CmdRequest request = new CmdRequest();
            request.cmd = "getprofile";
            sGetProfileCmd = new CommandData();
            sGetProfileCmd.command = JSON.toJSONString(request);
        }
        return sGetProfileCmd;
    }

    @Override
    public void onCallRing(String from, String type) {
        if (StringUtils.isEquals(from, mServerName)) {
            try {
                EMClient.getInstance().callManager().answerCall();
                VideoCallActivity.startVideoCall();
            } catch (EMNoActiveCallException e) {
                Logger.e(e);
            }
        }
    }

    public String getCmdDescription(String cmd) {
        return mCommands.get(cmd).description;
    }

    public void setOnCmdChangedListener(OnCmdChangedListener listener) {
        mListener = listener;
    }

    public Collection<CommandData> getCmd() {
        return mCommands.values();
    }

    private void loadCmdFromCache() {
        String cache = pref.getString(mServerName, null);
        List<CommandData> newdatas = JSON.parseArray(cache, CommandData.class);
        updateCmds(newdatas);
    }

    public void handleProfileResponse(List<CommandData> newdatas) {
        if (!CollectionUtils.isEmpty(newdatas)) {
            pref.edit().putString(mServerName, JSON.toJSONString(newdatas)).apply();
            updateCmds(newdatas);
        }
    }

    private void updateCmds(List<CommandData> datas) {
        mCommands.clear();

        if (!CollectionUtils.isEmpty(datas)) {
            for (CommandData data : datas) {
                mCommands.put(data.command, data);
            }
        }
        if (mListener != null) {
            mListener.onCmdChanged(mCommands.values());
        }
    }

    public void release() {
        App.getInstance().removeCallReceiverListener(this);
    }

    public interface OnCmdChangedListener {
        void onCmdChanged(Collection<CommandData> cmds);
    }
}
