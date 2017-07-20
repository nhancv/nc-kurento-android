package com.nhancv.kurentoandroid.one2one;

import android.Manifest;
import android.media.AudioManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.hannesdorfmann.mosby.mvp.MvpActivity;
import com.nhancv.kurentoandroid.R;
import com.nhancv.npermission.NPermission;
import com.nhancv.webrtcpeer.rtc_plugins.ProxyRenderer;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;
import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.EglBase;
import org.webrtc.Logging;
import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoRenderer;

/**
 * Created by nhancao on 7/20/17.
 */
@EActivity(R.layout.activity_one2one)
public class One2OneActivity extends MvpActivity<One2OneView, One2OnePresenter>
        implements One2OneView, NPermission.OnPermissionResult {

    private static final String TAG = One2OneActivity.class.getSimpleName();

    @ViewById(R.id.llInputRegister)
    protected LinearLayout llInputRegister;
    @ViewById(R.id.etInputName)
    protected EditText etInputName;
    @ViewById(R.id.etInputPeer)
    protected EditText etInputPeer;
    @ViewById(R.id.btRegister)
    protected Button btRegister;
    @ViewById(R.id.btOne2OneCall)
    protected Button btOne2OneCall;

    @ViewById(R.id.vGLSurfaceViewCallFull)
    protected SurfaceViewRenderer vGLSurfaceViewCallFull;
    @ViewById(R.id.vGLSurfaceViewCallPip)
    protected SurfaceViewRenderer vGLSurfaceViewCallPip;

    private NPermission nPermission;
    private EglBase rootEglBase;
    private ProxyRenderer localProxyRenderer;
    private ProxyRenderer remoteProxyRenderer;
    private Toast logToast;
    private boolean isGranted;
    private boolean isSwappedFeeds;

    @AfterViews
    protected void init() {

        nPermission = new NPermission(true);

        setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);

        //config peer
        localProxyRenderer = new ProxyRenderer();
        remoteProxyRenderer = new ProxyRenderer();
        rootEglBase = EglBase.create();

        vGLSurfaceViewCallFull.init(rootEglBase.getEglBaseContext(), null);
        vGLSurfaceViewCallFull.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
        vGLSurfaceViewCallFull.setEnableHardwareScaler(true);
        vGLSurfaceViewCallFull.setMirror(true);

        vGLSurfaceViewCallPip.init(rootEglBase.getEglBaseContext(), null);
        vGLSurfaceViewCallPip.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
        vGLSurfaceViewCallPip.setEnableHardwareScaler(true);
        vGLSurfaceViewCallPip.setMirror(true);
        vGLSurfaceViewCallPip.setZOrderMediaOverlay(true);

        // Swap feeds on pip view click.
        vGLSurfaceViewCallPip.setOnClickListener(view -> setSwappedFeeds(!isSwappedFeeds));

        setSwappedFeeds(true);
        presenter.connectServer();
    }

    @Override
    public void setSwappedFeeds(boolean isSwappedFeeds) {
        Logging.d(TAG, "setSwappedFeeds: " + isSwappedFeeds);
        this.isSwappedFeeds = isSwappedFeeds;
        localProxyRenderer.setTarget(isSwappedFeeds ? vGLSurfaceViewCallFull : vGLSurfaceViewCallPip);
        remoteProxyRenderer.setTarget(isSwappedFeeds ? vGLSurfaceViewCallPip : vGLSurfaceViewCallFull);
        vGLSurfaceViewCallFull.setMirror(isSwappedFeeds);
        vGLSurfaceViewCallPip.setMirror(!isSwappedFeeds);
    }

    @Override
    public void socketConnect(boolean success) {
        llInputRegister.setVisibility(success ? View.VISIBLE : View.GONE);
    }

    @Override
    public void disconnect() {
        localProxyRenderer.setTarget(null);
        if (vGLSurfaceViewCallFull != null) {
            vGLSurfaceViewCallFull.release();
            vGLSurfaceViewCallFull = null;
        }

        finish();
    }

    @Override
    public void onResume() {
        super.onResume();

        btRegister.setEnabled(true);
        btOne2OneCall.setEnabled(false);

    }

    public void startCall() {
        if (Build.VERSION.SDK_INT < 23 || isGranted) {
            presenter.startCall();
        } else {
            nPermission.requestPermission(this, Manifest.permission.CAMERA);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        nPermission.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onPermissionResult(String permission, boolean isGranted) {
        switch (permission) {
            case Manifest.permission.CAMERA:
                this.isGranted = isGranted;
                if (!isGranted) {
                    nPermission.requestPermission(this, Manifest.permission.CAMERA);
                } else {
                    //nPermission.requestPermission(this, Manifest.permission.RECORD_AUDIO);

                    presenter.startCall();

                }
                break;
            default:
                break;
        }
    }

    @NonNull
    @Override
    public One2OnePresenter createPresenter() {
        return new One2OnePresenter(getApplication());
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
            transactionToCalling(etInputName.getText().toString(), etInputPeer.getText().toString(), true);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        presenter.disconnect();
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
    public VideoCapturer createVideoCapturer() {
        VideoCapturer videoCapturer;
        if (useCamera2()) {
            if (!captureToTexture()) {
                return null;
            }
            videoCapturer = createCameraCapturer(new Camera2Enumerator(this));
        } else {
            videoCapturer = createCameraCapturer(new Camera1Enumerator(captureToTexture()));
        }
        if (videoCapturer == null) {
            return null;
        }
        return videoCapturer;
    }

    @Override
    public EglBase.Context getEglBaseContext() {
        return rootEglBase.getEglBaseContext();
    }

    @Override
    public VideoRenderer.Callbacks getLocalProxyRenderer() {
        return localProxyRenderer;
    }

    @Override
    public VideoRenderer.Callbacks getRemoteProxyRenderer() {
        return remoteProxyRenderer;
    }

    @Override
    public void registerStatus(boolean success) {
        btRegister.setEnabled(!success);
        btOne2OneCall.setEnabled(success);
    }

    @Override
    public void transactionToCalling(String fromPeer, String toPeer, boolean isHost) {
        presenter.initPeerConfig(fromPeer, toPeer, isHost);
        startCall();
    }

    @Override
    public void incomingCalling(String fromPeer) {
        transactionToCalling(fromPeer, etInputName.getText().toString(), false);
    }

    @Override
    public void stopCalling() {
        llInputRegister.setVisibility(View.VISIBLE);
    }

    @Override
    public void startCallIng() {
        llInputRegister.setVisibility(View.GONE);
    }

    private boolean useCamera2() {
        return Camera2Enumerator.isSupported(this) && presenter.getDefaultConfig().isUseCamera2();
    }

    private boolean captureToTexture() {
        return presenter.getDefaultConfig().isCaptureToTexture();
    }

    private VideoCapturer createCameraCapturer(CameraEnumerator enumerator) {
        final String[] deviceNames = enumerator.getDeviceNames();
        // First, try to find front facing camera
        for (String deviceName : deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        // Front facing camera not found, try something else
        for (String deviceName : deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        return null;
    }

}
