package com.penghaonan.homemonitorclient.cmd;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.hyphenate.EMMessageListener;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMImageMessageBody;
import com.hyphenate.chat.EMMessage;
import com.hyphenate.chat.EMTextMessageBody;
import com.penghaonan.appframework.AppDelegate;
import com.penghaonan.appframework.utils.UiUtils;
import com.penghaonan.homemonitorclient.R;
import com.penghaonan.homemonitorclient.base.BaseActivity;
import com.penghaonan.homemonitorclient.cmd.db.CmdLog;
import com.penghaonan.homemonitorclient.cmd.db.DbManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import terranovaproductions.newcomicreader.FloatingActionMenu;

import static com.penghaonan.homemonitorclient.cmd.CmdHelper.CMD_GET_PROFILE;

public class CmdActivity extends BaseActivity {

    public final static String EXTRAS_SERVER = "EXTRAS_SERVER";

    @BindView(R.id.root_view)
    ViewGroup mRootView;

    @BindView(R.id.recycler_view)
    RecyclerView mRecyclerView;

    private String mServerId;
    private CmdHelper mCmdHelper;
    private ProgressDialog mLoadingDialog;
    private List<CmdLog> mCmdLogList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cmd);
        ButterKnife.bind(this);
        mServerId = this.getIntent().getStringExtra(EXTRAS_SERVER);
        mCmdHelper = new CmdHelper(mServerId);
        mCmdLogList = DbManager.getCmdLogDao().loadAll();
        EMClient.getInstance().chatManager().addMessageListener(msgListener);
        sendMesaage(CMD_GET_PROFILE);
        mLoadingDialog = new ProgressDialog(this);
        mLoadingDialog.setMessage(getString(R.string.loading_server));
        mLoadingDialog.show();
    }


    private void sendMesaage(String content) {
        // 创建一条文本消息，content为消息文字内容，toChatUsername为对方用户或者群聊的id，后文皆是如此
        EMMessage message = EMMessage.createTxtSendMessage(content, mServerId);
        // 发送消息
        EMClient.getInstance().chatManager().sendMessage(message);
        if (CMD_GET_PROFILE.equals(content)) {
            return;
        }
        CmdLog log = new CmdLog();
        log.setServer(mServerId);
        log.setContent(content);
        log.setContentType(CmdLog.CONTENT_TYPE_TEXT);
        log.setLogType(CmdLog.LOG_TYPE_REQUEST);
        log.setTime(System.currentTimeMillis());
        DbManager.getCmdLogDao().save(log);
        //TODO 更新界面
        mCmdLogList.add(log);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EMClient.getInstance().chatManager().removeMessageListener(msgListener);
        mCmdHelper.release();
    }

    private void initCmdMenu() {
        FloatingActionMenu actionMenu = (FloatingActionMenu) LayoutInflater.from(this).inflate(
                R.layout.floating_action_menu, mRootView, false);

        //点击展开按钮
        FloatingActionButton actionButton = (FloatingActionButton) LayoutInflater.from(this).inflate(
                R.layout.view_floating_action_btn_mini, actionMenu, false);
        actionMenu.addView(actionButton);
        actionMenu.setmItemGap(UiUtils.dip2Px(AppDelegate.getApp(), 10));

        Collection<CommandData> cmds = mCmdHelper.getCmd();
        for (CommandData cmd : cmds) {
            actionButton = (FloatingActionButton) LayoutInflater.from(this).inflate(
                    R.layout.view_floating_action_btn_mini, actionMenu, false);
            actionButton.setContentDescription(cmd.description);
            actionButton.setTag(R.id.root_view, cmd);
            actionMenu.addView(actionButton);
        }

        actionMenu.setOnMenuItemClickListener(new FloatingActionMenu.OnMenuItemClickListener() {
            @Override
            public void onMenuItemClick(FloatingActionMenu floatingActionMenu, int i, FloatingActionButton floatingActionButton) {
                CommandData cmd = (CommandData) floatingActionButton.getTag(R.id.root_view);
                sendMesaage(cmd.command);
            }
        });
        mRootView.addView(actionMenu);
    }

    private void handleMsgReceived(EMMessage message) {
        CmdLog log = new CmdLog();
        log.setServer(mServerId);
        log.setLogType(CmdLog.LOG_TYPE_RESPONSE);
        log.setTime(System.currentTimeMillis());
        if (message.getBody() instanceof EMTextMessageBody) {
            EMTextMessageBody textBody = (EMTextMessageBody) message.getBody();
            String response = textBody.getMessage();

            if (TextUtils.isEmpty(response)) {
                return;
            }
            if (mCmdHelper.handleProfileResponse(textBody.getMessage())) {
                AppDelegate.post(new Runnable() {
                    @Override
                    public void run() {
                        initCmdMenu();
                        mLoadingDialog.dismiss();
                    }
                });
                return;
            } else {
                log.setContent(response);
                log.setContentType(CmdLog.CONTENT_TYPE_TEXT);
            }
        } else if (message.getBody() instanceof EMImageMessageBody) {
            EMImageMessageBody imgBody = (EMImageMessageBody) message.getBody();
            log.setContent(imgBody.getRemoteUrl());
            log.setContentType(CmdLog.CONTENT_TYPE_PIC);
        } else {
            return;
        }
        DbManager.getCmdLogDao().save(log);
        mCmdLogList.add(log);
        //TODO 更新界面
    }

    private EMMessageListener msgListener = new EMMessageListener() {

        @Override
        public void onMessageReceived(List<EMMessage> messages) {

            for (EMMessage message : messages) {
                String username;
                // 群组消息
                if (message.getChatType() == EMMessage.ChatType.GroupChat || message.getChatType() == EMMessage.ChatType.ChatRoom) {
                    username = message.getTo();
                } else {
                    // 单聊消息
                    username = message.getFrom();
                }
                // 如果是当前会话的消息，刷新聊天页面

                if (username.equals(mServerId)) {
                    handleMsgReceived(message);
                }
            }
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

    private class CmdViewHolder extends RecyclerView.ViewHolder {

        public CmdViewHolder(View itemView) {
            super(itemView);
        }
    }

    private class CmdAdapter extends RecyclerView.Adapter {

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return null;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        }

        @Override
        public int getItemCount() {
            return 0;
        }
    }
}
