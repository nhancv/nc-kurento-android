package com.nhancv.kurentoandroid.viewer;

import android.support.annotation.NonNull;

import com.hannesdorfmann.mosby.mvp.MvpActivity;
import com.nhancv.kurentoandroid.R;

import org.androidannotations.annotations.EActivity;

/**
 * Created by nhancao on 7/20/17.
 */
@EActivity(R.layout.activity_viewer)
public class ViewerActivity extends MvpActivity<ViewerView, ViewerPresenter> implements ViewerView {

    @NonNull
    @Override
    public ViewerPresenter createPresenter() {
        return new ViewerPresenter();
    }
}
