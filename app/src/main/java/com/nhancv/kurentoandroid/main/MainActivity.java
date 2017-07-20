package com.nhancv.kurentoandroid.main;

import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.hannesdorfmann.mosby.mvp.MvpActivity;
import com.nhancv.kurentoandroid.R;
import com.nhancv.kurentoandroid.broadcaster.BroadCasterActivity_;
import com.nhancv.kurentoandroid.one2one.One2OneActivity_;
import com.nhancv.kurentoandroid.viewer.ViewerActivity_;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;

/**
 * Created by nhancao on 9/18/16.
 */

@EActivity(R.layout.activity_main)
public class MainActivity extends MvpActivity<MainView, MainPresenter> implements MainView {
    private static final String TAG = MainActivity.class.getName();
    @ViewById(R.id.etInputName)
    protected EditText etInputName;
    @ViewById(R.id.etInputPeer)
    protected EditText etInputPeer;
    @ViewById(R.id.btRegister)
    protected Button btRegister;
    @ViewById(R.id.btOne2OneCall)
    protected Button btOne2OneCall;

    private Toast logToast;

    @AfterViews
    protected void init() {
        presenter.connectServer();
    }

    @Override
    protected void onResume() {
        super.onResume();
        btRegister.setEnabled(true);
        btOne2OneCall.setEnabled(false);
    }

    @Click(R.id.btBroadCaster)
    protected void btBroadCasterClick() {
        BroadCasterActivity_.intent(this).start();
    }

    @Click(R.id.btViewer)
    protected void btViewerClick() {
        ViewerActivity_.intent(this).start();
    }

    @Click(R.id.btRegister)
    protected void btRegisterClick() {
        if (!TextUtils.isEmpty(etInputName.getText().toString())) {
            presenter.register(etInputName.getText().toString());
        }
    }

    @Click(R.id.btOne2OneCall)
    protected void btOne2OneClick() {
        if (!TextUtils.isEmpty(etInputPeer.getText().toString())) {
            transactionToCalling(etInputPeer.getText().toString(), true);
        }
    }

    @NonNull
    @Override
    public MainPresenter createPresenter() {
        return new MainPresenter(getApplication());
    }

    @Override
    public void logAndToast(String msg) {
        Log.d(TAG, msg);
        if (logToast != null) {
            logToast.cancel();
        }
        logToast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
        logToast.show();
    }

    @Override
    public void registerStatus(boolean success) {
        btRegister.setEnabled(!success);
        btOne2OneCall.setEnabled(success);
    }

    @Override
    public void transactionToCalling(String name, boolean isHost) {
        One2OneActivity_.intent(this).userName(name).isHost(isHost).start();
    }

    @Override
    public void incomingCalling(String name) {
        transactionToCalling(name, false);
    }
}
