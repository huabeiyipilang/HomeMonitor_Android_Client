package com.penghaonan.homemonitorclient.cmd.db;

import com.penghaonan.appframework.AppDelegate;

import org.greenrobot.greendao.database.Database;

public class DbManager {
    private final static DbManager sInstance = new DbManager();

    private DaoSession mDaoSession;

    private DbManager() {
        DaoMaster.DevOpenHelper helper = new DaoMaster.DevOpenHelper(AppDelegate.getApp(), "cmd_db");
        Database db = helper.getWritableDb();
        mDaoSession = new DaoMaster(db).newSession();
    }

    public static CmdLogDao getCmdLogDao() {
        return sInstance.mDaoSession.getCmdLogDao();
    }

}
