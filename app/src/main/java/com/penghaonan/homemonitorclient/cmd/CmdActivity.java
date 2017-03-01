package com.penghaonan.homemonitorclient.cmd;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.penghaonan.homemonitorclient.R;
import com.penghaonan.homemonitorclient.cmd.transfer.CmdRequest;
import com.penghaonan.homemonitorclient.cmd.transfer.CmdResponse;
import com.penghaonan.homemonitorclient.cmd.transfer.CmdTransfer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class CmdActivity extends BaseCmdActivity {

    public final static String EXTRAS_SERVER = "EXTRAS_SERVER";

    @BindView(R.id.root_view)
    ViewGroup mRootView;

    @BindView(R.id.grid_view)
    GridView mGridView;

    @BindView(R.id.view_torch_status)
    TextView mTorchStatusView;

    private CmdHelper mCmdHelper;
    private ProgressDialog mLoadingDialog;
    private CmdRequest mProfileRequest;
    private CmdAdapter mCmdAdapter;
    private boolean mTouchOn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cmd);
        ButterKnife.bind(this);

        mCmdHelper = new CmdHelper(mServerId);
        mCmdHelper.setOnCmdChangedListener(new CmdHelper.OnCmdChangedListener() {
            @Override
            public void onCmdChanged(Collection<CommandData> cmds) {
                mCmdAdapter.updateCmd(cmds);
            }
        });
        mProfileRequest = CmdTransfer.getInstance().sendCmd(mServerId, CmdHelper.getProfileCmd());
        mLoadingDialog = new ProgressDialog(this);
        mLoadingDialog.setMessage(getString(R.string.loading_server));
        mLoadingDialog.show();

        mCmdAdapter = new CmdAdapter();
        mGridView.setAdapter(mCmdAdapter);
        mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                CommandData data = mCmdAdapter.getItem(position);
                CmdTransfer.getInstance().sendCmd(mServerId, data);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCmdHelper.release();
    }

    @Override
    public void onResponseReceived(CmdResponse response) {
        if (mProfileRequest != null && response != null && response.id == mProfileRequest.id) {
            JSONObject object = JSON.parseObject(response.msg);
            mTouchOn = object.getBoolean("torch_status");
            updateTorchStatus();
            List<CommandData> newdatas = JSON.parseArray(object.getString("cmd_profile"), CommandData.class);
            mCmdHelper.handleProfileResponse(newdatas);
            mLoadingDialog.dismiss();
            mProfileRequest = null;
        }
    }

    private void updateTorchStatus() {
        mTorchStatusView.setText(getString(R.string.torch_status_info, (mTouchOn ? getString(R.string.on) : getString(R.string.off))));
    }

    @Override
    public void onImageReceived(String remoteUrl) {

    }

    private class CmdAdapter extends BaseAdapter {

        List<CommandData> cmds = new ArrayList<>();

        public void updateCmd(Collection<CommandData> cmds) {
            this.cmds.clear();
            this.cmds.addAll(cmds);
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return cmds.size();
        }

        @Override
        public CommandData getItem(int position) {
            return cmds.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder = null;
            if (convertView == null) {
                convertView = LayoutInflater.from(getApplicationContext()).inflate(R.layout.item_view_cmd, mGridView, false);
                holder = new ViewHolder();
                holder.titleView = (TextView) convertView.findViewById(R.id.tv_title);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            holder.titleView.setText(getItem(position).description);
            return convertView;
        }
    }

    private class ViewHolder {
        TextView titleView;
    }
}
