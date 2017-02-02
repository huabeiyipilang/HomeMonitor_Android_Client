package com.penghaonan.homemonitorclient.activity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.hyphenate.EMMessageListener;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMConversation;
import com.hyphenate.chat.EMImageMessageBody;
import com.hyphenate.chat.EMMessage;
import com.hyphenate.chat.EMMessage.ChatType;
import com.hyphenate.chat.EMTextMessageBody;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.penghaonan.appframework.utils.UiUtils;
import com.penghaonan.homemonitorclient.Constant;
import com.penghaonan.homemonitorclient.R;
import com.penghaonan.homemonitorclient.base.BaseActivity;
import com.penghaonan.homemonitorclient.cmd.CmdHelper;
import com.penghaonan.homemonitorclient.cmd.CmdPanelView;
import com.penghaonan.homemonitorclient.cmd.CommandData;
import com.penghaonan.homemonitorclient.utils.EaseCommonUtils;

import java.util.List;

import butterknife.ButterKnife;
import butterknife.OnClick;

public class ChatActivity extends BaseActivity implements CmdPanelView.CmdListener {
    private ListView listView;
    private int chatType = 1;
    private String toChatUsername;
    private Button btn_send;
    private EditText et_content;
    private List<EMMessage> msgList;
    MessageAdapter adapter;
    private EMConversation conversation;
    protected int pagesize = 2;
    private CmdHelper mCmdHelper;

    @Override
    protected void onCreate(Bundle arg0) {
        super.onCreate(arg0);
        setContentView(R.layout.activity_chat);
        ButterKnife.bind(this);

        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(this).build();
        ImageLoader mImageLoader = ImageLoader.getInstance();
        mImageLoader.init(config);

        toChatUsername = this.getIntent().getStringExtra("username");
        mCmdHelper = new CmdHelper(toChatUsername);
        TextView tv_toUsername = (TextView) this.findViewById(R.id.tv_toUsername);
        tv_toUsername.setText(toChatUsername);
        listView = (ListView) this.findViewById(R.id.listView);
        btn_send = (Button) this.findViewById(R.id.btn_send);
        et_content = (EditText) this.findViewById(R.id.et_content);
        et_content.clearFocus();
        getAllMessage();
        msgList = conversation.getAllMessages();
        adapter = new MessageAdapter(msgList, ChatActivity.this);
        listView.setAdapter(adapter);
        listView.setSelection(listView.getCount() - 1);
        btn_send.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                String content = et_content.getText().toString().trim();
                if (TextUtils.isEmpty(content)) {
                    return;
                }
                setMesaage(content);
            }

        });
        EMClient.getInstance().chatManager().addMessageListener(msgListener);
    }

    protected void getAllMessage() {
        // 获取当前conversation对象

        conversation = EMClient.getInstance().chatManager().getConversation(toChatUsername,
                EaseCommonUtils.getConversationType(chatType), true);
        // 把此会话的未读数置为0
        conversation.markAllMessagesAsRead();
        // 初始化db时，每个conversation加载数目是getChatOptions().getNumberOfMessagesLoaded
        // 这个数目如果比用户期望进入会话界面时显示的个数不一样，就多加载一些
        final List<EMMessage> msgs = conversation.getAllMessages();
        int msgCount = msgs != null ? msgs.size() : 0;
        if (msgCount < conversation.getAllMsgCount() && msgCount < pagesize) {
            String msgId = null;
            if (msgs != null && msgs.size() > 0) {
                msgId = msgs.get(0).getMsgId();
            }
            conversation.loadMoreMsgFromDB(msgId, pagesize - msgCount);
        }

    }

    private void scrollToBottom() {
        if (msgList.size() > 0) {
            listView.smoothScrollToPosition(listView.getCount());
        }
    }

    private void setMesaage(String content) {

        // 创建一条文本消息，content为消息文字内容，toChatUsername为对方用户或者群聊的id，后文皆是如此
        EMMessage message = EMMessage.createTxtSendMessage(content, toChatUsername);
        // 如果是群聊，设置chattype，默认是单聊
        if (chatType == Constant.CHATTYPE_GROUP)
            message.setChatType(ChatType.GroupChat);
        // 发送消息
        EMClient.getInstance().chatManager().sendMessage(message);
        msgList.add(message);

        adapter.notifyDataSetChanged();
        scrollToBottom();
        et_content.setText("");
        et_content.clearFocus();
    }

    EMMessageListener msgListener = new EMMessageListener() {

        @Override
        public void onMessageReceived(List<EMMessage> messages) {

            for (EMMessage message : messages) {
                String username;
                // 群组消息
                if (message.getChatType() == ChatType.GroupChat || message.getChatType() == ChatType.ChatRoom) {
                    username = message.getTo();
                } else {
                    // 单聊消息
                    username = message.getFrom();
                }
                // 如果是当前会话的消息，刷新聊天页面
                if (username.equals(toChatUsername)) {
                    if (message.getBody() instanceof EMTextMessageBody) {
                        EMTextMessageBody textBody = (EMTextMessageBody) message.getBody();
                        mCmdHelper.handleResponse(textBody.getMessage());
                    }
                    msgList.addAll(messages);
                    adapter.notifyDataSetChanged();
                    scrollToBottom();
                }
            }

            // 收到消息
        }

        @Override
        public void onCmdMessageReceived(List<EMMessage> messages) {
            // 收到透传消息
        }

        @Override
        public void onMessageRead(List<EMMessage> messages) {

        }

        @Override
        public void onMessageDelivered(List<EMMessage> messages) {

        }

        @Override
        public void onMessageChanged(EMMessage message, Object change) {
            // 消息状态变动
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EMClient.getInstance().chatManager().removeMessageListener(msgListener);
    }

    @SuppressLint("InflateParams")
    private class MessageAdapter extends BaseAdapter {
        private List<EMMessage> msgs;
        private Context context;
        private LayoutInflater inflater;

        MessageAdapter(List<EMMessage> msgs, Context context_) {
            this.msgs = msgs;
            this.context = context_;
            inflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return msgs.size();
        }

        @Override
        public EMMessage getItem(int position) {
            return msgs.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getItemViewType(int position) {
            EMMessage message = getItem(position);
            if (message.direct() == EMMessage.Direct.RECEIVE) {
                if (message.getBody() instanceof EMTextMessageBody) {
                    return 0;
                } else if (message.getBody() instanceof EMImageMessageBody) {
                    return 1;
                }
            } else {
                if (message.getBody() instanceof EMTextMessageBody) {
                    return 2;
                } else if (message.getBody() instanceof EMImageMessageBody) {
                    return 3;
                }
            }
            return 2;
        }

        @Override
        public int getViewTypeCount() {
            return 4;
        }

        @SuppressLint("InflateParams")
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            EMMessage message = getItem(position);
            int viewType = getItemViewType(position);
            switch (viewType) {
                case 0:
                    if (convertView == null) {
                        convertView = inflater.inflate(R.layout.item_message_received, parent, false);
                        TextMsgViewHolder holder = new TextMsgViewHolder();
                        holder.tv = (TextView) convertView.findViewById(R.id.tv_chatcontent);
                        convertView.setTag(holder);
                    }
                    EMTextMessageBody txtBody0 = (EMTextMessageBody) message.getBody();
                    TextMsgViewHolder holder0 = (TextMsgViewHolder) convertView.getTag();
                    holder0.tv.setText(txtBody0.getMessage());
                    break;
                case 1:
                    if (convertView == null) {
                        convertView = inflater.inflate(R.layout.item_message_img_received, parent, false);
                        ImgMsgViewHolder holder = new ImgMsgViewHolder();
                        holder.iv = (ImageView) convertView.findViewById(R.id.tv_chatcontent);
                        convertView.setTag(holder);
                    }
                    EMImageMessageBody imgBody1 = (EMImageMessageBody) message.getBody();
                    ImgMsgViewHolder holder1 = (ImgMsgViewHolder) convertView.getTag();
                    ImageLoader.getInstance().displayImage(imgBody1.getRemoteUrl(), holder1.iv);
                    break;
                case 2:
                    if (convertView == null) {
                        convertView = inflater.inflate(R.layout.item_message_sent, parent, false);
                        TextMsgViewHolder holder = new TextMsgViewHolder();
                        holder.tv = (TextView) convertView.findViewById(R.id.tv_chatcontent);
                        convertView.setTag(holder);
                    }
                    EMTextMessageBody txtBody2 = (EMTextMessageBody) message.getBody();
                    TextMsgViewHolder holder2 = (TextMsgViewHolder) convertView.getTag();
                    holder2.tv.setText(txtBody2.getMessage());
                    break;
                case 3:
                    if (convertView == null) {
                        convertView = inflater.inflate(R.layout.item_message_img_sent, parent, false);
                        ImgMsgViewHolder holder = new ImgMsgViewHolder();
                        holder.iv = (ImageView) convertView.findViewById(R.id.tv_chatcontent);
                        convertView.setTag(holder);
                    }
                    EMImageMessageBody imgBody3 = (EMImageMessageBody) message.getBody();
                    ImgMsgViewHolder holder3 = (ImgMsgViewHolder) convertView.getTag();
                    ImageLoader.getInstance().displayImage(imgBody3.getRemoteUrl(), holder3.iv);
                    break;
            }

            return convertView;
        }

    }

    private static class TextMsgViewHolder {
        TextView tv;
    }

    private static class ImgMsgViewHolder {
        ImageView iv;
    }

    private CmdPanelView mPanelView;
    private AlertDialog mCmdDailog;

    @OnClick(R.id.btn_cmd)
    void onCmdClick() {
        if (mCmdDailog == null) {
            mPanelView = new CmdPanelView(this);
            mPanelView.setCmdListener(this);
            mCmdDailog = new AlertDialog.Builder(this).setView(mPanelView).create();
        }
        mPanelView.updateCmds(mCmdHelper.getCmd());
        mCmdDailog.show();
    }

    @Override
    public void OnCmdSelected(CommandData cmd) {
        mCmdDailog.dismiss();
        if (cmd != null && !TextUtils.isEmpty(cmd.command)) {
            setMesaage(cmd.command);
        }
    }
}
