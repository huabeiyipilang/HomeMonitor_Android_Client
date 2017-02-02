package com.penghaonan.homemonitorclient.base;

import android.os.Bundle;
import android.support.annotation.Nullable;

import com.penghaonan.appframework.base.BaseFrameworkActivity;
import com.penghaonan.appframework.utils.UiUtils;
import com.penghaonan.homemonitorclient.R;

public class BaseActivity extends BaseFrameworkActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UiUtils.initActivityStatusNavigationBarColor(this, R.color.colorPrimary, R.color.colorPrimary);
    }
}
