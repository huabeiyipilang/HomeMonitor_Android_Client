package com.penghaonan.homemonitorclient.cmd;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ScrollView;

import com.penghaonan.appframework.utils.CollectionUtils;
import com.penghaonan.homemonitorclient.R;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class CmdPanelView extends ScrollView implements View.OnClickListener {

    public interface CmdListener {
        void OnCmdSelected(CommandData cmd);
    }

    private List<CommandData> mCmds = new ArrayList<>();
    private CmdListener mCmdListener;

    @BindView(R.id.root_view)
    ViewGroup mRootView;

    public CmdPanelView(Context context) {
        super(context);
        init();
    }

    public CmdPanelView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        inflate(getContext(), R.layout.view_cmd_panel, this);
        ButterKnife.bind(this);
    }

    public void updateCmds(Collection<CommandData> datas){
        mCmds.clear();
        CollectionUtils.addAll(mCmds, datas);
        updateUI();
    }

    public void setCmdListener(CmdListener mCmdListener) {
        this.mCmdListener = mCmdListener;
    }

    private void updateUI() {
        mRootView.removeAllViews();
        for (CommandData cmd : mCmds) {
            Button bt = new Button(getContext());
            bt.setText(cmd.description);
            bt.setTag(cmd);
            bt.setOnClickListener(this);
            mRootView.addView(bt);
        }
    }

    @Override
    public void onClick(View v) {
        if (v == null) {
            return;
        }
        CommandData cmd = (CommandData) v.getTag();
        if (cmd == null) {
            return;
        }
        if (mCmdListener != null) {
            mCmdListener.OnCmdSelected(cmd);
        }
    }
}