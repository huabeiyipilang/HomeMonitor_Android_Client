package com.penghaonan.homemonitorclient.cmd;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;

import com.github.piasy.biv.BigImageViewer;
import com.github.piasy.biv.loader.fresco.FrescoImageLoader;
import com.github.piasy.biv.view.BigImageView;
import com.penghaonan.appframework.AppDelegate;
import com.penghaonan.homemonitorclient.R;

import butterknife.BindView;
import butterknife.ButterKnife;


public class ImageViewrActivity extends Activity {

    private final static String EXTRAS_URL = "EXTRAS_URL";

    static {
        BigImageViewer.initialize(FrescoImageLoader.with(AppDelegate.getApp()));
    }

    @BindView(R.id.biv_img)
    BigImageView bigImageView;

    public static void showImage(Activity activity, String url) {
        Intent intent = new Intent(activity, ImageViewrActivity.class);
        intent.putExtra(EXTRAS_URL, url);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_viewr);
        ButterKnife.bind(this);

        String url = getIntent().getStringExtra(EXTRAS_URL);
        if (TextUtils.isEmpty(url)) {
            finish();
            return;
        } else {
            bigImageView.showImage(Uri.parse(url));
        }
    }
}
