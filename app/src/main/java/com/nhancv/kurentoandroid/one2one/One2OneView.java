package com.nhancv.kurentoandroid.one2one;

import com.hannesdorfmann.mosby.mvp.MvpView;

import org.webrtc.EglBase;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoRenderer;

/**
 * Created by nhancao on 7/20/17.
 */

public interface One2OneView extends MvpView {

    void logAndToast(String msg);

    void disconnect();

    VideoCapturer createVideoCapturer();

    EglBase.Context getEglBaseContext();

    VideoRenderer.Callbacks getLocalProxyRenderer();

    VideoRenderer.Callbacks getRemoteProxyRenderer();

    void setSwappedFeeds(boolean swappedFeed);

    void socketConnect(boolean success);

    void registerStatus(boolean success);

    void transactionToCalling(String fromPeer, String toPeer, boolean isHost);

    void incomingCalling(String fromPeer);

    void stopCalling();

    void startCallIng();
}
