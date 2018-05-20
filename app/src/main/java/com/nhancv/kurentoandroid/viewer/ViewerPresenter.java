package com.nhancv.kurentoandroid.viewer;

import android.app.Application;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.hannesdorfmann.mosby.mvp.MvpBasePresenter;
import com.nhancv.kurentoandroid.rtc_peer.kurento.KurentoViewerRTCClient;
import com.nhancv.kurentoandroid.rtc_peer.kurento.models.CandidateModel;
import com.nhancv.kurentoandroid.rtc_peer.kurento.models.response.ServerResponse;
import com.nhancv.kurentoandroid.rtc_peer.kurento.models.response.TypeResponse;
import com.nhancv.kurentoandroid.util.RxScheduler;
import com.nhancv.webrtcpeer.rtc_comm.ws.BaseSocketCallback;
import com.nhancv.webrtcpeer.rtc_comm.ws.DefaultSocketService;
import com.nhancv.webrtcpeer.rtc_comm.ws.SocketService;
import com.nhancv.webrtcpeer.rtc_peer.PeerConnectionClient;
import com.nhancv.webrtcpeer.rtc_peer.PeerConnectionParameters;
import com.nhancv.webrtcpeer.rtc_peer.SignalingEvents;
import com.nhancv.webrtcpeer.rtc_peer.SignalingParameters;
import com.nhancv.webrtcpeer.rtc_peer.StreamMode;
import com.nhancv.webrtcpeer.rtc_peer.config.DefaultConfig;
import com.nhancv.webrtcpeer.rtc_plugins.RTCAudioManager;

import org.java_websocket.handshake.ServerHandshake;
import org.webrtc.IceCandidate;
import org.webrtc.PeerConnection;
import org.webrtc.SessionDescription;
import org.webrtc.StatsReport;

import java.util.LinkedList;

import static android.content.ContentValues.TAG;

/**
 * Created by nhancao on 7/20/17.
 */

public class ViewerPresenter extends MvpBasePresenter<ViewerView>
        implements SignalingEvents, PeerConnectionClient.PeerConnectionEvents {

    private static final String STREAM_HOST = "wss://192.168.1.3:6008/one2many";

    private Application application;
    private SocketService socketService;
    private Gson gson;

    private PeerConnectionClient peerConnectionClient;
    private KurentoViewerRTCClient rtcClient;
    private PeerConnectionParameters peerConnectionParameters;
    private DefaultConfig defaultConfig;
    private RTCAudioManager audioManager;
    private SignalingParameters signalingParameters;
    private boolean iceConnected;

    public ViewerPresenter(Application application) {
        this.application = application;
        this.socketService = new DefaultSocketService(application);
        this.gson = new Gson();
    }

    public void initPeerConfig() {
        rtcClient = new KurentoViewerRTCClient(socketService);
        defaultConfig = new DefaultConfig();
        peerConnectionParameters = defaultConfig.createPeerConnectionParams(StreamMode.RECV_ONLY);
        peerConnectionClient = PeerConnectionClient.getInstance();
        peerConnectionClient.createPeerConnectionFactory(
                application.getApplicationContext(), peerConnectionParameters, this);
    }

    public void disconnect() {
        if (rtcClient != null) {
            rtcClient = null;
        }
        if (peerConnectionClient != null) {
            peerConnectionClient.close();
            peerConnectionClient = null;
        }

        if (audioManager != null) {
            audioManager.stop();
            audioManager = null;
        }

        if (socketService != null) {
            socketService.close();
        }

        if (isViewAttached()) {
            getView().disconnect();
        }
    }

    public void startCall() {
        if (rtcClient == null) {
            Log.e(TAG, "AppRTC client is not allocated for a call.");
            return;
        }

        rtcClient.connectToRoom(STREAM_HOST, new BaseSocketCallback() {
            @Override
            public void onOpen(ServerHandshake serverHandshake) {
                super.onOpen(serverHandshake);
                RxScheduler.runOnUi(o -> {
                    if (isViewAttached()) {
                        getView().logAndToast("Socket connected");
                    }
                });
                SignalingParameters parameters = new SignalingParameters(
                        new LinkedList<PeerConnection.IceServer>() {
                            {
                                add(new PeerConnection.IceServer("stun:stun.l.google.com:19302"));
                            }
                        }, true, null, null, null, null, null);
                onSignalConnected(parameters);
            }

            @Override
            public void onMessage(String serverResponse_) {
                super.onMessage(serverResponse_);
                try {
                    ServerResponse serverResponse = gson.fromJson(serverResponse_, ServerResponse.class);

                    switch (serverResponse.getIdRes()) {
                        case VIEWER_RESPONSE:
                            if (serverResponse.getTypeRes() == TypeResponse.REJECTED) {
                                RxScheduler.runOnUi(o -> {
                                    if (isViewAttached()) {
                                        getView().logAndToast(serverResponse.getMessage());
                                    }
                                });
                            } else {
                                SessionDescription sdp = new SessionDescription(SessionDescription.Type.ANSWER,
                                                                                serverResponse.getSdpAnswer());
                                onRemoteDescription(sdp);
                            }
                            break;
                        case ICE_CANDIDATE:
                            CandidateModel candidateModel = serverResponse.getCandidate();
                            onRemoteIceCandidate(
                                    new IceCandidate(candidateModel.getSdpMid(), candidateModel.getSdpMLineIndex(),
                                                     candidateModel.getSdp()));
                            break;
                        case STOP_COMMUNICATION:
                            RxScheduler.runOnUi(o -> {
                                if (isViewAttached()) {
                                    getView().stopCommunication();
                                }
                            });
                            break;

                    }
                } catch (JsonSyntaxException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onClose(int i, String s, boolean b) {
                super.onClose(i, s, b);
                RxScheduler.runOnUi(o -> {
                    if (isViewAttached()) {
                        getView().logAndToast("Socket closed");
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                super.onError(e);
                RxScheduler.runOnUi(o -> {
                    if (isViewAttached()) {
                        getView().logAndToast(e.getMessage());
                    }
                });
            }

        });

        // Create and audio manager that will take care of audio routing,
        // audio modes, audio device enumeration etc.
        audioManager = RTCAudioManager.create(application.getApplicationContext());
        // Store existing audio settings and change audio mode to
        // MODE_IN_COMMUNICATION for best possible VoIP performance.
        Log.d(TAG, "Starting the audio manager...");
        audioManager.start((audioDevice, availableAudioDevices) ->
                                   Log.d(TAG, "onAudioManagerDevicesChanged: " + availableAudioDevices + ", "
                                              + "selected: " + audioDevice));
    }

    public DefaultConfig getDefaultConfig() {
        return defaultConfig;
    }

    private void callConnected() {
        if (peerConnectionClient == null) {
            Log.w(TAG, "Call is connected in closed or error state");
            return;
        }
        // Enable statistics callback.
        peerConnectionClient.enableStatsEvents(true, 1000);
    }

    @Override
    public void onSignalConnected(SignalingParameters params) {
        RxScheduler.runOnUi(o -> {
            if (isViewAttached()) {
                signalingParameters = params;
                peerConnectionClient
                        .createPeerConnection(getView().getEglBaseContext(), null,
                                              getView().getRemoteProxyRenderer(), null,
                                              signalingParameters);

                if (signalingParameters.initiator) {
                    if (isViewAttached()) getView().logAndToast("Creating OFFER...");
                    // Create offer. Offer SDP will be sent to answering client in
                    // PeerConnectionEvents.onLocalDescription event.
                    peerConnectionClient.createOffer();
                } else {
                    if (params.offerSdp != null) {
                        peerConnectionClient.setRemoteDescription(params.offerSdp);
                        if (isViewAttached()) getView().logAndToast("Creating ANSWER...");
                        // Create answer. Answer SDP will be sent to offering client in
                        // PeerConnectionEvents.onLocalDescription event.
                        peerConnectionClient.createAnswer();
                    }
                    if (params.iceCandidates != null) {
                        // Add remote ICE candidates from room.
                        for (IceCandidate iceCandidate : params.iceCandidates) {
                            peerConnectionClient.addRemoteIceCandidate(iceCandidate);
                        }
                    }
                }
            }
        });
    }

    @Override
    public void onRemoteDescription(SessionDescription sdp) {
        RxScheduler.runOnUi(o -> {
            if (peerConnectionClient == null) {
                Log.e(TAG, "Received remote SDP for non-initilized peer connection.");
                return;
            }
            peerConnectionClient.setRemoteDescription(sdp);
            if (!signalingParameters.initiator) {
                if (isViewAttached()) getView().logAndToast("Creating ANSWER...");
                // Create answer. Answer SDP will be sent to offering client in
                // PeerConnectionEvents.onLocalDescription event.
                peerConnectionClient.createAnswer();
            }
        });
    }

    @Override
    public void onRemoteIceCandidate(IceCandidate candidate) {
        RxScheduler.runOnUi(o -> {
            if (peerConnectionClient == null) {
                Log.e(TAG, "Received ICE candidate for a non-initialized peer connection.");
                return;
            }
            peerConnectionClient.addRemoteIceCandidate(candidate);
        });
    }

    @Override
    public void onRemoteIceCandidatesRemoved(IceCandidate[] candidates) {
        RxScheduler.runOnUi(o -> {
            if (peerConnectionClient == null) {
                Log.e(TAG, "Received ICE candidate removals for a non-initialized peer connection.");
                return;
            }
            peerConnectionClient.removeRemoteIceCandidates(candidates);
        });
    }

    @Override
    public void onChannelClose() {
        RxScheduler.runOnUi(o -> {
            if (isViewAttached()) getView().logAndToast("Remote end hung up; dropping PeerConnection");
            disconnect();
        });
    }

    @Override
    public void onChannelError(String description) {
        Log.e(TAG, "onChannelError: " + description);
    }

    @Override
    public void onLocalDescription(SessionDescription sdp) {
        RxScheduler.runOnUi(o -> {
            if (rtcClient != null) {
                if (signalingParameters.initiator) {
                    rtcClient.sendOfferSdp(sdp);
                } else {
                    rtcClient.sendAnswerSdp(sdp);
                }
            }
            if (peerConnectionParameters.videoMaxBitrate > 0) {
                Log.d(TAG, "Set video maximum bitrate: " + peerConnectionParameters.videoMaxBitrate);
                peerConnectionClient.setVideoMaxBitrate(peerConnectionParameters.videoMaxBitrate);
            }
        });
    }

    @Override
    public void onIceCandidate(IceCandidate candidate) {
        RxScheduler.runOnUi(o -> {
            if (rtcClient != null) {
                rtcClient.sendLocalIceCandidate(candidate);
            }
        });
    }

    @Override
    public void onIceCandidatesRemoved(IceCandidate[] candidates) {
        RxScheduler.runOnUi(o -> {
            if (rtcClient != null) {
                rtcClient.sendLocalIceCandidateRemovals(candidates);
            }
        });
    }

    @Override
    public void onIceConnected() {
        RxScheduler.runOnUi(o -> {
            iceConnected = true;
            callConnected();
        });
    }

    @Override
    public void onIceDisconnected() {
        RxScheduler.runOnUi(o -> {
            if (isViewAttached()) getView().logAndToast("ICE disconnected");
            iceConnected = false;
            disconnect();
        });
    }

    @Override
    public void onPeerConnectionClosed() {

    }

    @Override
    public void onPeerConnectionStatsReady(StatsReport[] reports) {
        RxScheduler.runOnUi(o -> {
            if (iceConnected) {
                Log.e(TAG, "run: " + reports);
            }
        });
    }

    @Override
    public void onPeerConnectionError(String description) {
        Log.e(TAG, "onPeerConnectionError: " + description);
    }
}
