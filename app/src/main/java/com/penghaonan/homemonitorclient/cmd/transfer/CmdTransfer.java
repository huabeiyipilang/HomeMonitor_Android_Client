package com.penghaonan.homemonitorclient.cmd.transfer;

import android.text.TextUtils;

import com.alibaba.fastjson.JSON;
import com.hyphenate.EMMessageListener;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMImageMessageBody;
import com.hyphenate.chat.EMMessage;
import com.hyphenate.chat.EMTextMessageBody;
import com.penghaonan.appframework.AppDelegate;
import com.penghaonan.appframework.utils.CollectionUtils;
import com.penghaonan.homemonitorclient.cmd.CommandData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CmdTransfer implements EMMessageListener {
    private static CmdTransfer ourInstance = new CmdTransfer();
    private Map<String, Set<CmdListener>> mListenerMap = new HashMap<>();

    private CmdTransfer() {
        EMClient.getInstance().chatManager().addMessageListener(this);
    }

    public static CmdTransfer getInstance() {
        return ourInstance;
    }

    /**
     * 注册对应服务器的监听
     */
    public void addCmdListener(String serverId, CmdListener listener) {
        Set<CmdListener> listenerSet = mListenerMap.get(serverId);
        if (listenerSet == null) {
            listenerSet = new HashSet<>();
            mListenerMap.put(serverId, listenerSet);
        }
        listenerSet.add(listener);
    }

    /**
     * 取消对应服务器的监听
     */
    public void removeCmdListener(String serverId, CmdListener listener) {
        Set<CmdListener> listenerSet = mListenerMap.get(serverId);
        if (listenerSet != null) {
            listenerSet.remove(listener);
        }
    }

    /**
     * 发送命令
     */
    public CmdRequest sendCmd(String mServerId, CommandData cmd) {
        CmdRequest request = JSON.parseObject(cmd.command, CmdRequest.class);
        request.id = System.currentTimeMillis();
        EMMessage message = EMMessage.createTxtSendMessage(JSON.toJSONString(request), mServerId);
        EMClient.getInstance().chatManager().sendMessage(message);
        return request;
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
                    Set<CmdListener> listenerSet = mListenerMap.get(serverId);
                    if (!CollectionUtils.isEmpty(listenerSet)) {
                        for (final CmdListener listener : listenerSet) {
                            AppDelegate.post(new Runnable() {
                                @Override
                                public void run() {
                                    listener.onResponseReceived(response);
                                }
                            });
                        }
                    }
                } else if (message.getBody() instanceof EMImageMessageBody) {
                    //处理图片信息
                    EMImageMessageBody imgBody = (EMImageMessageBody) message.getBody();
                    final String imgUrl = imgBody.getRemoteUrl();
                    if (TextUtils.isEmpty(imgUrl)) {
                        continue;
                    }
                    Set<CmdListener> listenerSet = mListenerMap.get(serverId);
                    if (!CollectionUtils.isEmpty(listenerSet)) {
                        for (final CmdListener listener : listenerSet) {
                            AppDelegate.post(new Runnable() {
                                @Override
                                public void run() {
                                    listener.onImageReceived(imgUrl);
                                }
                            });
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

    public interface CmdListener {
        void onResponseReceived(CmdResponse response);

        void onImageReceived(String remoteUrl);
    }
}
