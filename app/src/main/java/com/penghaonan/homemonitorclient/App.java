package com.penghaonan.homemonitorclient;

import android.app.ActivityManager;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.hyphenate.EMCallBack;
import com.hyphenate.EMContactListener;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMOptions;
import com.nostra13.universalimageloader.cache.disc.impl.UnlimitedDiskCache;
import com.nostra13.universalimageloader.cache.memory.impl.WeakMemoryCache;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.penghaonan.appframework.AppDelegate;
import com.penghaonan.appframework.utils.StorageUtils;
import com.penghaonan.homemonitorclient.db.EaseUser;
import com.penghaonan.homemonitorclient.db.InviteMessage;
import com.penghaonan.homemonitorclient.db.InviteMessgeDao;
import com.penghaonan.homemonitorclient.db.Myinfo;
import com.penghaonan.homemonitorclient.db.UserDao;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class App extends Application {

    public static Context applicationContext;
    private static App instance;
    private String username = "";
    private Map<String, EaseUser> contactList;
    private InviteMessgeDao inviteMessgeDao;
    private UserDao userDao;
    private boolean sdkInited = false;

    /*
     * 第一步：sdk的一些参数配置 EMOptions 第二步：将配置参数封装类 传入SDK初始化
     */
    private List<IServerListChangedListener> mServerChangedListeners = new LinkedList<>();
    private List<ICallReceiverListener> mCallReceiverListeners = new LinkedList<>();

    public static App getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        AppDelegate.init(this);
        StorageUtils.setRootDir("HomeMonitor");
        applicationContext = this;
        instance = this;
        // 初始化环信sdk
        init(applicationContext);
        inviteMessgeDao = new InviteMessgeDao(this);
        initContactListener();

        IntentFilter callFilter = new IntentFilter(EMClient.getInstance().callManager().getIncomingCallBroadcastAction());
        registerReceiver(new CallReceiver(), callFilter);
        initImageLoader();
    }

    private void initImageLoader() {
        File imgCacheFile = new File(StorageUtils.getExternalFolder(), "imgcache");
        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(this)
                .diskCacheSize(10 * 1024 * 1024)
                .diskCache(new UnlimitedDiskCache(imgCacheFile))
                .memoryCache(new WeakMemoryCache())
                .build();
        ImageLoader mImageLoader = ImageLoader.getInstance();
        mImageLoader.init(config);
    }

    public void init(Context context) {
        // 第一步
        EMOptions options = initChatOptions();
        // 第二步
        boolean success = initSDK(context, options);
        if (success) {
            // 设为调试模式，打成正式包时，最好设为false，以免消耗额外的资源
            EMClient.getInstance().setDebugMode(true);
            // 初始化数据库
            initDbDao(context);
        }
    }

    private void initDbDao(Context context) {
        userDao = new UserDao(context);
    }

    public String getCurrentUserName() {
        if (TextUtils.isEmpty(username)) {
            username = Myinfo.getInstance(instance).getUserInfo(Constant.KEY_USERNAME);

        }
        return username;

    }

    public void setCurrentUserName(String username) {
        this.username = username;
        Myinfo.getInstance(instance).setUserInfo(Constant.KEY_USERNAME, username);
    }

    private EMOptions initChatOptions() {

        // 获取到EMChatOptions对象
        EMOptions options = new EMOptions();
        // 默认添加好友时，是不需要验证的，改成需要验证
        options.setAcceptInvitationAlways(false);
        // 设置是否需要已读回执
        options.setRequireAck(true);
        // 设置是否需要已送达回执
        options.setRequireDeliveryAck(false);
        return options;
    }

    public synchronized boolean initSDK(Context context, EMOptions options) {
        if (sdkInited) {
            return true;
        }

        int pid = android.os.Process.myPid();
        String processAppName = getAppName(pid);

        // 如果app启用了远程的service，此application:onCreate会被调用2次
        // 为了防止环信SDK被初始化2次，加此判断会保证SDK被初始化1次
        // 默认的app会在以包名为默认的process name下运行，如果查到的process name不是app的process
        // name就立即返回
        if (processAppName == null || !processAppName.equalsIgnoreCase(applicationContext.getPackageName())) {

            // 则此application::onCreate 是被service 调用的，直接返回
            return false;
        }
        if (options == null) {
            EMClient.getInstance().init(context, initChatOptions());
        } else {
            EMClient.getInstance().init(context, options);
        }
        sdkInited = true;
        return true;
    }

    /**
     * check the application process name if process name is not qualified, then
     * we think it is a service process and we will not init SDK
     *
     * @param pID
     * @return
     */
    @SuppressWarnings("rawtypes")
    private String getAppName(int pID) {
        String processName = null;
        ActivityManager am = (ActivityManager) applicationContext.getSystemService(Context.ACTIVITY_SERVICE);
        List l = am.getRunningAppProcesses();
        Iterator i = l.iterator();
        while (i.hasNext()) {
            ActivityManager.RunningAppProcessInfo info = (ActivityManager.RunningAppProcessInfo) (i.next());
            try {
                if (info.pid == pID) {

                    processName = info.processName;
                    return processName;
                }
            } catch (Exception e) {
            }
        }
        return processName;
    }

    public Map<String, EaseUser> getContactList() {

        if (contactList == null) {

            contactList = userDao.getContactList();

        }
        return contactList;

    }

    public void setContactList(Map<String, EaseUser> contactList) {

        this.contactList = contactList;

        userDao.saveContactList(new ArrayList<EaseUser>(contactList.values()));

    }

    /**
     * 退出登录
     *
     * @param unbindDeviceToken 是否解绑设备token(使用GCM才有)
     * @param callback          callback
     */
    public void logout(boolean unbindDeviceToken, final EMCallBack callback) {

        EMClient.getInstance().logout(unbindDeviceToken, new EMCallBack() {

            @Override
            public void onSuccess() {

                if (callback != null) {
                    callback.onSuccess();
                }

            }

            @Override
            public void onProgress(int progress, String status) {
                if (callback != null) {
                    callback.onProgress(progress, status);
                }
            }

            @Override
            public void onError(int code, String error) {

                if (callback != null) {
                    callback.onError(code, error);
                }
            }
        });
    }

    private void initContactListener() {
        //注册联系人变动监听
        EMClient.getInstance().contactManager().setContactListener(new EMContactListener() {


            @Override
            public void onContactAdded(final String username) {
                // 保存增加的联系人
                Map<String, EaseUser> localUsers = App.getInstance().getContactList();
                Map<String, EaseUser> toAddUsers = new HashMap<String, EaseUser>();
                EaseUser user = new EaseUser(username);
                // 添加好友时可能会回调added方法两次
                if (!localUsers.containsKey(username)) {
                    userDao.saveContact(user);
                }
                toAddUsers.put(username, user);
                localUsers.putAll(toAddUsers);
                AppDelegate.post(new Runnable() {

                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "增加联系人：+" + username, Toast.LENGTH_SHORT).show();
                        for (IServerListChangedListener listener : mServerChangedListeners) {
                            listener.onServerListChanged();
                        }
                    }


                });
            }

            @Override
            public void onContactDeleted(final String username) {
                // 被删除
                Map<String, EaseUser> localUsers = App.getInstance().getContactList();
                localUsers.remove(username);
                userDao.deleteContact(username);
                inviteMessgeDao.deleteMessage(username);
                AppDelegate.post(new Runnable() {

                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "删除联系人：+" + username, Toast.LENGTH_SHORT).show();
                        for (IServerListChangedListener listener : mServerChangedListeners) {
                            listener.onServerListChanged();
                        }
                    }


                });
            }

            @Override
            public void onContactInvited(final String username, String reason) {
                // 接到邀请的消息，如果不处理(同意或拒绝)，掉线后，服务器会自动再发过来，所以客户端不需要重复提醒
                List<InviteMessage> msgs = inviteMessgeDao.getMessagesList();

                for (InviteMessage inviteMessage : msgs) {
                    if (inviteMessage.getGroupId() == null && inviteMessage.getFrom().equals(username)) {
                        inviteMessgeDao.deleteMessage(username);
                    }
                }
                // 自己封装的javabean
                InviteMessage msg = new InviteMessage();
                msg.setFrom(username);
                msg.setTime(System.currentTimeMillis());
                msg.setReason(reason);

                // 设置相应status
                msg.setStatus(InviteMessage.InviteMesageStatus.BEINVITEED);
                notifyNewIviteMessage(msg);
                AppDelegate.post(new Runnable() {

                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "收到好友申请：+" + username, Toast.LENGTH_SHORT).show();
                    }


                });

            }

            @Override
            public void onFriendRequestAccepted(final String username) {
                List<InviteMessage> msgs = inviteMessgeDao.getMessagesList();
                for (InviteMessage inviteMessage : msgs) {
                    if (inviteMessage.getFrom().equals(username)) {
                        return;
                    }
                }
                // 自己封装的javabean
                InviteMessage msg = new InviteMessage();
                msg.setFrom(username);
                msg.setTime(System.currentTimeMillis());

                msg.setStatus(InviteMessage.InviteMesageStatus.BEAGREED);
                notifyNewIviteMessage(msg);
                AppDelegate.post(new Runnable() {

                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "好友申请同意：+" + username, Toast.LENGTH_SHORT).show();
                    }


                });
            }

            @Override
            public void onFriendRequestDeclined(String username) {
                Log.d(username, username + "拒绝了你的好友请求");
            }

            private void notifyNewIviteMessage(InviteMessage msg) {
                inviteMessgeDao.saveMessage(msg);
                //保存未读数，这里没有精确计算
                inviteMessgeDao.saveUnreadMessageCount(1);
                // 提示有新消息
                //响铃或其他操作
            }
        });
    }

    public void addServerListChangedListener(IServerListChangedListener listener) {
        if (!mServerChangedListeners.contains(listener)) {
            mServerChangedListeners.add(listener);
        }
    }

    public void removeServerListChangedListener(IServerListChangedListener listener) {
        if (mServerChangedListeners.contains(listener)) {
            mServerChangedListeners.remove(listener);
        }
    }

    public void addCallReceiverListener(ICallReceiverListener listener) {
        if (!mCallReceiverListeners.contains(listener)) {
            mCallReceiverListeners.add(listener);
        }
    }

    public void removeCallReceiverListener(ICallReceiverListener listener) {
        if (mCallReceiverListeners.contains(listener)) {
            mCallReceiverListeners.remove(listener);
        }
    }

    public interface IServerListChangedListener {
        void onServerListChanged();
    }

    public interface ICallReceiverListener {
        void onCallRing(String from, String type);
    }

    private class CallReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            // 拨打方username
            String from = intent.getStringExtra("from");
            // call type
            String type = intent.getStringExtra("type");
            //跳转到通话页面

            for (ICallReceiverListener listener : mCallReceiverListeners) {
                listener.onCallRing(from, type);
            }
        }
    }
}
