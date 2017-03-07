package com.penghaonan.homemonitorclient.cmd;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.hyphenate.EMMessageListener;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMImageMessageBody;
import com.hyphenate.chat.EMMessage;
import com.hyphenate.chat.EMTextMessageBody;
import com.hyphenate.exceptions.HyphenateException;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.penghaonan.appframework.AppDelegate;
import com.penghaonan.appframework.utils.Logger;
import com.penghaonan.appframework.utils.UiUtils;
import com.penghaonan.homemonitorclient.R;
import com.penghaonan.homemonitorclient.base.BaseActivity;
import com.penghaonan.homemonitorclient.cmd.db.CmdLog;
import com.penghaonan.homemonitorclient.cmd.db.DbManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import terranovaproductions.newcomicreader.FloatingActionMenu;

import static com.penghaonan.homemonitorclient.cmd.CmdHelper.CMD_GET_PROFILE;

public class CmdActivity extends BaseActivity {

    public final static String EXTRAS_SERVER = "EXTRAS_SERVER";
    private final static int TYPE_REQUEST_CMD = getTypeId(CmdLog.LOG_TYPE_REQUEST, CmdLog.CONTENT_TYPE_TEXT);
    private final static int TYPE_RESPONSE_TEXT = getTypeId(CmdLog.LOG_TYPE_RESPONSE, CmdLog.CONTENT_TYPE_TEXT);
    private final static int TYPE_RESPONSE_IMG = getTypeId(CmdLog.LOG_TYPE_RESPONSE, CmdLog.CONTENT_TYPE_PIC);
    private static DisplayImageOptions sWallpaperListOptions;
    @BindView(R.id.root_view)
    ViewGroup mRootView;
    @BindView(R.id.recycler_view)
    RecyclerView mRecyclerView;
    private String mServerId;
    private CmdHelper mCmdHelper;
    private ProgressDialog mLoadingDialog;
    private List<CmdLog> mCmdLogList = new ArrayList<>();
    private CmdAdapter mAdapter;
    private EMMessageListener msgListener = new EMMessageListener() {

        @Override
        public void onMessageReceived(List<EMMessage> messages) {

            for (final EMMessage message : messages) {
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
                    AppDelegate.post(new Runnable() {
                        @Override
                        public void run() {
                            handleMsgReceived(message);
                        }
                    });
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

    private static int getTypeId(int logType, int contentType) {
        return logType * 10 + contentType;
    }

    private static DisplayImageOptions getImgOptions() {
        if (sWallpaperListOptions == null) {
            synchronized (CmdActivity.class) {
                if (sWallpaperListOptions == null) {
                    sWallpaperListOptions = new DisplayImageOptions.Builder()
                            .cacheInMemory(false)
                            .cacheOnDisk(true)
                            .bitmapConfig(Bitmap.Config.RGB_565)
                            .build();
                }
            }
        }
        return sWallpaperListOptions;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cmd);
        ButterKnife.bind(this);

        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setAdapter(mAdapter = new CmdAdapter());
        int spacingInPixels = getResources().getDimensionPixelSize(R.dimen.cmd_item_h_divider);
        mRecyclerView.addItemDecoration(new SpaceItemDecoration(spacingInPixels));

        mServerId = this.getIntent().getStringExtra(EXTRAS_SERVER);
        ((TextView) findViewById(R.id.tv_server)).setText(mServerId);
        mCmdHelper = new CmdHelper(mServerId);
        mCmdLogList = DbManager.getCmdLogDao().loadAll();
        EMClient.getInstance().chatManager().addMessageListener(msgListener);
        sendMesaage(CMD_GET_PROFILE);
        mLoadingDialog = new ProgressDialog(this);
        mLoadingDialog.setMessage(getString(R.string.loading_server));
        mLoadingDialog.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI(false);
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

        mCmdLogList.add(log);
        updateUI(true);
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
        actionMenu.setBackground(new ColorDrawable(0x33000000));

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
        updateUI(true);
    }

    private void updateUI(boolean anim) {
        mAdapter.notifyDataSetChanged();
        if (anim) {
            mRecyclerView.smoothScrollToPosition(mCmdLogList.size() - 1);
        } else {
            mRecyclerView.scrollToPosition(mCmdLogList.size() - 1);
        }
    }

    @OnClick(R.id.btn_del)
    void onDeleteServerClick() {
        try {
            EMClient.getInstance().contactManager().deleteContact(mServerId);
            finish();
        } catch (HyphenateException e) {
            Logger.e(e);
        }
    }

    class CmdRequestHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.tv_cmd_title)
        TextView titleView;
        @BindView(R.id.tv_cmd_str)
        TextView cmdView;
        @BindView(R.id.tv_cmd_time)
        TextView timeView;

        public CmdRequestHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    class CmdResponseTextHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.iv_txt)
        TextView textView;

        public CmdResponseTextHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    class CmdResponseImgHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.iv_pic)
        ImageView imgView;

        public CmdResponseImgHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    private class CmdAdapter extends RecyclerView.Adapter {

        private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", AppDelegate.getApp().getResources().getConfiguration().locale);

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == TYPE_REQUEST_CMD) {
                return new CmdRequestHolder(LayoutInflater.from(
                        CmdActivity.this).inflate(R.layout.item_view_cmdlist_cmd, parent,
                        false));
            } else if (viewType == TYPE_RESPONSE_TEXT) {
                return new CmdResponseTextHolder(LayoutInflater.from(
                        CmdActivity.this).inflate(R.layout.item_view_cmdlist_response_txt, parent,
                        false));
            } else if (viewType == TYPE_RESPONSE_IMG) {
                return new CmdResponseImgHolder(LayoutInflater.from(
                        CmdActivity.this).inflate(R.layout.item_view_cmdlist_response_img, parent,
                        false));
            } else {
                return null;
            }
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            CmdLog cmdLog = mCmdLogList.get(position);
            if (holder instanceof CmdRequestHolder) {
                CmdRequestHolder hd = (CmdRequestHolder) holder;
                hd.titleView.setText(mCmdHelper.getCmdDescription(cmdLog.getContent()));
                hd.cmdView.setText(cmdLog.getContent());
                hd.timeView.setText(sdf.format(new Date(cmdLog.getTime())));
            } else if (holder instanceof CmdResponseTextHolder) {
                CmdResponseTextHolder hd = (CmdResponseTextHolder) holder;
                hd.textView.setText(cmdLog.getContent());
            } else if (holder instanceof CmdResponseImgHolder) {
                CmdResponseImgHolder hd = (CmdResponseImgHolder) holder;
                ImageLoader.getInstance().displayImage(cmdLog.getContent(), hd.imgView, getImgOptions());
            }
        }

        @Override
        public int getItemCount() {
            return mCmdLogList.size();
        }

        @Override
        public int getItemViewType(int position) {
            CmdLog cmdLog = mCmdLogList.get(position);
            return getTypeId(cmdLog.getLogType(), cmdLog.getContentType());
        }
    }

    private class SpaceItemDecoration extends RecyclerView.ItemDecoration {

        private int space;

        public SpaceItemDecoration(int space) {
            this.space = space;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {

            if (parent.getChildPosition(view) != 0)
                outRect.top = space;
        }
    }
}
