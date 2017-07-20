package com.nhancv.kurentoandroid.viewer;

import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.hannesdorfmann.mosby.mvp.MvpActivity;
import com.nhancv.kurentoandroid.R;
import com.nhancv.webrtcpeer.rtc_plugins.ProxyRenderer;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;
import org.webrtc.EglBase;
import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoRenderer;

/**
 * Created by nhancao on 7/20/17.
 */
@EActivity(R.layout.activity_viewer)
public class ViewerActivity extends MvpActivity<ViewerView, ViewerPresenter> implements ViewerView {
    private static final String TAG = ViewerActivity.class.getSimpleName();

    @ViewById(R.id.vGLSurfaceViewCall)
    protected SurfaceViewRenderer vGLSurfaceViewCall;

    private EglBase rootEglBase;
    private ProxyRenderer remoteProxyRenderer;
    private Toast logToast;

    @AfterViews
    protected void init() {
        //config peer
        remoteProxyRenderer = new ProxyRenderer();
        rootEglBase = EglBase.create();

        vGLSurfaceViewCall.init(rootEglBase.getEglBaseContext(), null);
        vGLSurfaceViewCall.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
        vGLSurfaceViewCall.setEnableHardwareScaler(true);
        vGLSurfaceViewCall.setMirror(true);
        remoteProxyRenderer.setTarget(vGLSurfaceViewCall);

        presenter.initPeerConfig();
        presenter.startCall();
    }

    @Override
    public void disconnect() {
        remoteProxyRenderer.setTarget(null);
        if (vGLSurfaceViewCall != null) {
            vGLSurfaceViewCall.release();
            vGLSurfaceViewCall = null;
        }

        finish();
    }

    @NonNull
    @Override
    public ViewerPresenter createPresenter() {
        return new ViewerPresenter(getApplication());
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        presenter.disconnect();
    }

    @Override
    public void stopCommunication() {
        onBackPressed();
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
    public EglBase.Context getEglBaseContext() {
        return rootEglBase.getEglBaseContext();
    }

    @Override
    public VideoRenderer.Callbacks getRemoteProxyRenderer() {
        return remoteProxyRenderer;
    }

}
