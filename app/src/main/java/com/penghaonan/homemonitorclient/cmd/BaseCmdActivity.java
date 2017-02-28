package com.penghaonan.homemonitorclient.cmd;

import android.os.Bundle;
import android.support.annotation.Nullable;

import com.penghaonan.homemonitorclient.base.BaseActivity;
import com.penghaonan.homemonitorclient.cmd.transfer.CmdTransfer;

public abstract class BaseCmdActivity extends BaseActivity implements CmdTransfer.CmdListener {
    public final static String EXTRAS_SERVER = "EXTRAS_SERVER";

    protected String mServerId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mServerId = this.getIntent().getStringExtra(EXTRAS_SERVER);
        CmdTransfer.getInstance().addCmdListener(mServerId, this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        CmdTransfer.getInstance().removeCmdListener(mServerId, this);
    }
}
