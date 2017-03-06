package com.penghaonan.homemonitorclient.cmd.transfer;

import android.support.v4.util.LongSparseArray;
import android.text.TextUtils;

import com.alibaba.fastjson.JSON;
import com.hyphenate.EMMessageListener;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMImageMessageBody;
import com.hyphenate.chat.EMMessage;
import com.hyphenate.chat.EMTextMessageBody;
import com.penghaonan.appframework.AppDelegate;
import com.penghaonan.appframework.utils.CollectionUtils;
import com.penghaonan.appframework.utils.Logger;
import com.penghaonan.homemonitorclient.cmd.CommandData;
import com.penghaonan.homemonitorclient.cmd.handler.ACmdHandler;
import com.penghaonan.homemonitorclient.cmd.handler.CmdHandlerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CmdTransfer implements EMMessageListener {
    private static CmdTransfer ourInstance = new CmdTransfer();
    private LongSparseArray<ACmdHandler> mRequestArray = new LongSparseArray<>();

    private CmdTransfer() {
        EMClient.getInstance().chatManager().addMessageListener(this);
    }

    public static CmdTransfer getInstance() {
        return ourInstance;
    }

    /**
     * 发送命令
     */
    public CmdRequest sendCmd(String mServerId, CommandData cmd) {
        CmdRequest request = cmd.createRequest();
        request.id = System.currentTimeMillis();
        EMMessage message = EMMessage.createTxtSendMessage(JSON.toJSONString(request), mServerId);
        EMClient.getInstance().chatManager().sendMessage(message);
        return request;
    }

    /**
     * 发送命令
     */
    public long sendCmd(String serverId, CommandData cmd, CmdRequestListener listener) {
        CmdRequest request = cmd.createRequest();
        request.id = System.currentTimeMillis();
        EMMessage message = EMMessage.createTxtSendMessage(JSON.toJSONString(request), serverId);
        EMClient.getInstance().chatManager().sendMessage(message);

        ACmdHandler cmdHandler = CmdHandlerFactory.createCmdHandler(request.cmd);
        if (cmdHandler == null) {
            Logger.e("Cmd handler is null");
            return -1;
        }
        cmdHandler.setServerId(serverId);
        cmdHandler.setCmdData(cmd);
        cmdHandler.setListener(listener);
        cmdHandler.setRequest(request);
        mRequestArray.put(request.id, cmdHandler);
        return request.id;
    }

    @Override
    public void onMessageReceived(List<EMMessage> messages) {
        for (EMMessage message : messages) {
            String serverId;
            // 群组消息
            if (message.getChatType() == EMMessage.ChatType.GroupChat || message.getChatType() == EMMessage.ChatType.ChatRoom) {
//                username = message.getTo();
                continue;
            } else {
                // 单聊消息
                serverId = message.getFrom();
            }
            // 如果是当前会话的消息，刷新聊天页面
            if (!TextUtils.isEmpty(serverId)) {
                if (message.getBody() instanceof EMTextMessageBody) {
                    //处理文本信息
                    EMTextMessageBody textBody = (EMTextMessageBody) message.getBody();
                    String body = textBody.getMessage();
                    final CmdResponse response = JSON.parseObject(body, CmdResponse.class);
                    if (response == null) {
                        continue;
                    }
                    ACmdHandler handler = mRequestArray.get(response.id);
                    handler.onResponseReceived(response);
                } else if (message.getBody() instanceof EMImageMessageBody) {
                    //处理图片信息
                    EMImageMessageBody imgBody = (EMImageMessageBody) message.getBody();
                    final String imgUrl = imgBody.getRemoteUrl();
                    if (TextUtils.isEmpty(imgUrl)) {
                        continue;
                    }
                    ACmdHandler handler;
                    for (int i = 0; i < mRequestArray.size(); i ++) {
                        handler = mRequestArray.valueAt(i);
                        if (handler.onImageReceived(imgUrl)) {
                            return;
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onCmdMessageReceived(List<EMMessage> messages) {

    }

    @Override
    public void onMessageRead(List<EMMessage> messages) {

    }

    @Override
    public void onMessageDelivered(List<EMMessage> messages) {

    }

    @Override
    public void onMessageChanged(EMMessage message, Object change) {

    }
}
