package com.penghaonan.homemonitorclient.cmd;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.alibaba.fastjson.JSON;
import com.hyphenate.chat.EMClient;
import com.hyphenate.exceptions.EMNoActiveCallException;
import com.penghaonan.appframework.AppDelegate;
import com.penghaonan.appframework.utils.CollectionUtils;
import com.penghaonan.appframework.utils.Logger;
import com.penghaonan.appframework.utils.StringUtils;
import com.penghaonan.homemonitorclient.App;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CmdHelper implements App.ICallReceiverListener {
    public final static String CMD_GET_PROFILE = "getprofile";
    private SharedPreferences pref = AppDelegate.getApp().getSharedPreferences("cmd_cache", Context.MODE_PRIVATE);
    private String mServerName;
    private Map<String, CommandData> mCommands = new HashMap<>();
    private OnCmdChangedListener mListener;

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

    public interface OnCmdChangedListener {
        void onCmdChanged(Collection<CommandData> cmds);
    }

    public CmdHelper(String server) {
        mServerName = server;
        loadCmdFromCache();
        App.getInstance().addCallReceiverListener(this);
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

    public boolean handleProfileResponse(String response) {
        if (TextUtils.isEmpty(response)) {
            return false;
        }
        if (response.startsWith(CMD_GET_PROFILE)) {
            if (response.length() > CMD_GET_PROFILE.length() + 1) {
                String body = response.substring(CMD_GET_PROFILE.length() + 1);
                List<CommandData> newdatas = JSON.parseArray(body, CommandData.class);
                if (!CollectionUtils.isEmpty(newdatas)) {
                    pref.edit().putString(mServerName, body).apply();
                    updateCmds(newdatas);
                }
            }
            return true;
        } else {
            return false;
        }
    }

    private void updateCmds(List<CommandData> datas) {
        mCommands.clear();

//        CommandData cmd = new CommandData();
//        cmd.command = CMD_GET_PROFILE;
//        cmd.description = AppDelegate.getApp().getString(R.string.cmd_get_profile);
//        mCommands.put(cmd.command, cmd);

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
}
