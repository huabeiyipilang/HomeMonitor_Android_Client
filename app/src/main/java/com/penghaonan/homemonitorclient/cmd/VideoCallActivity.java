package com.penghaonan.homemonitorclient.cmd;

import android.os.Bundle;
import android.widget.TextView;

import com.hyphenate.chat.EMCallStateChangeListener;
import com.hyphenate.chat.EMClient;
import com.hyphenate.exceptions.EMNoActiveCallException;
import com.hyphenate.exceptions.HyphenateException;
import com.hyphenate.media.EMLocalSurfaceView;
import com.hyphenate.media.EMOppositeSurfaceView;
import com.penghaonan.appframework.AppDelegate;
import com.penghaonan.appframework.utils.Logger;
import com.penghaonan.homemonitorclient.R;
import com.penghaonan.homemonitorclient.base.BaseActivity;

import butterknife.BindView;
import butterknife.ButterKnife;

public class VideoCallActivity extends BaseActivity {

    @BindView(R.id.sv_remote)
    EMOppositeSurfaceView mRemoteView;

    @BindView(R.id.sv_local)
    EMLocalSurfaceView mLocalView;

    @BindView(R.id.tv_tips)
    TextView mTipsView;

    public static void startVideoCall() {
        AppDelegate.startActivity(VideoCallActivity.class, null);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_call);
        ButterKnife.bind(this);
        EMClient.getInstance().callManager().setSurfaceView(mLocalView, mRemoteView);
        EMClient.getInstance().callManager().addCallStateChangeListener(new EMCallStateChangeListener() {
            @Override
            public void onCallStateChanged(CallState callState, CallError error) {
                switch (callState) {
                    case CONNECTING: // 正在连接对方
                        showTips(getString(R.string.video_call_connecting));
                        break;
                    case CONNECTED: // 双方已经建立连接
                        try {
                            EMClient.getInstance().callManager().pauseVideoTransfer();
                            EMClient.getInstance().callManager().pauseVoiceTransfer();
                        } catch (HyphenateException e) {
                            Logger.e(e);
                        }
                        showTips("连接已建立");
                        break;

                    case ACCEPTED: // 电话接通成功
                        showTips("已接通");
                        break;
                    case DISCONNECTED: // 电话断了
                        showTips("通话结束");
                        AppDelegate.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                finish();
                            }
                        }, 1000);
                        break;
                    case NETWORK_UNSTABLE: //网络不稳定
                        if (error == CallError.ERROR_NO_DATA) {
                            //无通话数据
                        } else {
                        }
                        showTips("网络不稳定");
                        break;
                    case NETWORK_NORMAL: //网络恢复正常
                        showTips("网络恢复正常");
                        break;
                    default:
                        break;
                }

            }
        });
    }

    private void showTips(final String tips) {
        AppDelegate.post(new Runnable() {
            @Override
            public void run() {
                mTipsView.setText(tips);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            EMClient.getInstance().callManager().endCall();
        } catch (EMNoActiveCallException e) {
            Logger.e(e);
        }
    }
}
