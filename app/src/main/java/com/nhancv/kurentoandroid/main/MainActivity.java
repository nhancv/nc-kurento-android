package com.nhancv.kurentoandroid.main;

import android.support.annotation.NonNull;

import com.hannesdorfmann.mosby.mvp.MvpActivity;
import com.nhancv.kurentoandroid.R;
import com.nhancv.kurentoandroid.broadcaster.BroadCasterActivity_;
import com.nhancv.kurentoandroid.one2one.One2OneActivity_;
import com.nhancv.kurentoandroid.viewer.ViewerActivity_;

import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;

/**
 * Created by nhancao on 9/18/16.
 */

@EActivity(R.layout.activity_main)
public class MainActivity extends MvpActivity<MainView, MainPresenter> implements MainView {
    private static final String TAG = MainActivity.class.getName();

    @Click(R.id.btBroadCaster)
    protected void btBroadCasterClick() {
        BroadCasterActivity_.intent(this).start();
    }

    @Click(R.id.btViewer)
    protected void btViewerClick() {
        ViewerActivity_.intent(this).start();
    }

    @Click(R.id.btOne2One)
    protected void btOne2OneClick() {
        One2OneActivity_.intent(this).start();
    }

    @NonNull
    @Override
    public MainPresenter createPresenter() {
        return new MainPresenter(getApplication());
    }

}
