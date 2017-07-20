package com.nhancv.kurentoandroid.broadcaster;

import android.Manifest;
import android.media.AudioManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.hannesdorfmann.mosby.mvp.MvpActivity;
import com.nhancv.kurentoandroid.R;
import com.nhancv.npermission.NPermission;
import com.nhancv.webrtcpeer.rtc_plugins.ProxyRenderer;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;
import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.EglBase;
import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoRenderer;

/**
 * Created by nhancao on 7/20/17.
 */
@EActivity(R.layout.activity_broadcaster)
public class BroadCasterActivity extends MvpActivity<BroadCasterView, BroadCasterPresenter>
        implements BroadCasterView, NPermission.OnPermissionResult {
    private static final String TAG = BroadCasterActivity.class.getSimpleName();

    @ViewById(R.id.vGLSurfaceViewCall)
    protected SurfaceViewRenderer vGLSurfaceViewCall;

    private NPermission nPermission;
    private EglBase rootEglBase;
    private ProxyRenderer localProxyRenderer;
    private Toast logToast;
    private boolean isGranted;

    @AfterViews
    protected void init() {

        nPermission = new NPermission(true);

        setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);

        //config peer
        localProxyRenderer = new ProxyRenderer();
        rootEglBase = EglBase.create();

        vGLSurfaceViewCall.init(rootEglBase.getEglBaseContext(), null);
        vGLSurfaceViewCall.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
        vGLSurfaceViewCall.setEnableHardwareScaler(true);
        vGLSurfaceViewCall.setMirror(true);
        localProxyRenderer.setTarget(vGLSurfaceViewCall);

        presenter.initPeerConfig();
    }

    @Override
    public void disconnect() {
        localProxyRenderer.setTarget(null);
        if (vGLSurfaceViewCall != null) {
            vGLSurfaceViewCall.release();
            vGLSurfaceViewCall = null;
        }

        finish();
    }

    @Override
    public void onResume() {
        super.onResume();
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
    public BroadCasterPresenter createPresenter() {
        return new BroadCasterPresenter(getApplication());
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
