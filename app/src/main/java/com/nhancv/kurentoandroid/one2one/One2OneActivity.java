package com.nhancv.kurentoandroid.one2one;

import android.support.annotation.NonNull;

import com.hannesdorfmann.mosby.mvp.MvpActivity;
import com.nhancv.kurentoandroid.R;

import org.androidannotations.annotations.EActivity;

/**
 * Created by nhancao on 7/20/17.
 */
@EActivity(R.layout.activity_one2one)
public class One2OneActivity extends MvpActivity<One2OneView, One2OnePresenter> implements One2OneView {

    @NonNull
    @Override
    public One2OnePresenter createPresenter() {
        return new One2OnePresenter();
    }
}
