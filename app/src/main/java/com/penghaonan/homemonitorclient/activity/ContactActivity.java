package com.penghaonan.homemonitorclient.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.hyphenate.chat.EMClient;
import com.penghaonan.appframework.utils.DoubleEventHelper;
import com.penghaonan.homemonitorclient.App;
import com.penghaonan.homemonitorclient.R;
import com.penghaonan.homemonitorclient.base.BaseActivity;
import com.penghaonan.homemonitorclient.db.EaseUser;
import com.penghaonan.homemonitorclient.utils.EaseCommonUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ContactActivity extends BaseActivity implements App.IServerListChangedListener {

    protected List<EaseUser> contactList = new ArrayList<>();
    protected ListView listView;
    private Map<String, EaseUser> contactsMap;
    private ContactAdapter adapter;
    private DoubleEventHelper mDoubleEventHelper = new DoubleEventHelper(1000, R.string.double_click_exite);

    protected void onCreate(Bundle arg0) {
        super.onCreate(arg0);
        setContentView(R.layout.activity_contact);
        this.findViewById(R.id.btn_add).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                startActivity(new Intent(ContactActivity.this, AddContactActivity.class));
            }

        });
        listView = (ListView) this.findViewById(R.id.listView);
        updateContactList();
        listView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                startActivity(new Intent(ContactActivity.this, ChatActivity.class).putExtra("username", adapter.getItem(arg2).getUsername()));
                finish();
            }

        });

        App.getInstance().addServerListChangedListener(this);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        App.getInstance().removeServerListChangedListener(this);
    }

    private void updateContactList() {
        getContactList();
        if (adapter == null) {
            getContactList();
            adapter = new ContactAdapter(this, contactList);
            listView.setAdapter(adapter);
        } else {
            adapter.setUsers(contactList);
            adapter.notifyDataSetChanged();
        }
    }

    /**
     * 获取联系人列表，并过滤掉黑名单和排序
     */
    protected void getContactList() {
        contactList.clear();
        // 获取联系人列表
        contactsMap = App.getInstance().getContactList();
        if (contactsMap == null) {
            return;
        }
        synchronized (this.contactsMap) {
            Iterator<Map.Entry<String, EaseUser>> iterator = contactsMap.entrySet().iterator();
            List<String> blackList = EMClient.getInstance().contactManager().getBlackListUsernames();
            while (iterator.hasNext()) {
                Map.Entry<String, EaseUser> entry = iterator.next();
                // 兼容以前的通讯录里的已有的数据显示，加上此判断，如果是新集成的可以去掉此判断
                if (!entry.getKey().equals("item_new_friends") && !entry.getKey().equals("item_groups")
                        && !entry.getKey().equals("item_chatroom") && !entry.getKey().equals("item_robots")) {
                    if (!blackList.contains(entry.getKey())) {
                        // 不显示黑名单中的用户
                        EaseUser user = entry.getValue();
                        EaseCommonUtils.setUserInitialLetter(user);
                        contactList.add(user);
                    }
                }
            }
        }

        // 排序
        Collections.sort(contactList, new Comparator<EaseUser>() {

            @Override
            public int compare(EaseUser lhs, EaseUser rhs) {
                if (lhs.getInitialLetter().equals(rhs.getInitialLetter())) {
                    return lhs.getNick().compareTo(rhs.getNick());
                } else {
                    if ("#".equals(lhs.getInitialLetter())) {
                        return 1;
                    } else if ("#".equals(rhs.getInitialLetter())) {
                        return -1;
                    }
                    return lhs.getInitialLetter().compareTo(rhs.getInitialLetter());
                }

            }
        });

    }

    @Override
    public void onServerListChanged() {
        updateContactList();
    }

    class ContactAdapter extends BaseAdapter {
        private Context context;
        private List<EaseUser> users;
        private LayoutInflater inflater;

        public ContactAdapter(Context context_, List<EaseUser> users) {

            this.context = context_;
            this.users = users;
            inflater = LayoutInflater.from(context);

        }

        public void setUsers(List<EaseUser> users) {
            this.users = users;
        }

        @Override
        public int getCount() {
            // TODO Auto-generated method stub
            return users.size();
        }

        @Override
        public EaseUser getItem(int position) {
            // TODO Auto-generated method stub
            return users.get(position);
        }

        @Override
        public long getItemId(int position) {
            // TODO Auto-generated method stub
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            if (convertView == null) {

                convertView = inflater.inflate(R.layout.item_contact, parent, false);

            }

            TextView tv_name = (TextView) convertView.findViewById(R.id.tv_name);
            tv_name.setText(getItem(position).getUsername());

            return convertView;
        }
    }

    @Override
    public void onBackPressed() {
        if (mDoubleEventHelper.onEvent()) {
            super.onBackPressed();
        }
    }
}
