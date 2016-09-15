package com.nhancv.kurentoandroid;

import android.app.Application;

import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.SessionDescription;
import org.webrtc.VideoRenderer;

import java.io.IOException;
import java.util.HashMap;

import fi.vtt.nubomedia.jsonrpcwsandroid.JsonRpcNotification;
import fi.vtt.nubomedia.jsonrpcwsandroid.JsonRpcRequest;
import fi.vtt.nubomedia.jsonrpcwsandroid.JsonRpcResponse;
import fi.vtt.nubomedia.utilitiesandroid.LooperExecutor;
import fi.vtt.nubomedia.webrtcpeerandroid.NBMMediaConfiguration;
import fi.vtt.nubomedia.webrtcpeerandroid.NBMPeerConnection;
import fi.vtt.nubomedia.webrtcpeerandroid.NBMWebRTCPeer;

/**
 * Created by nhancao on 9/14/16.
 */
public class App extends Application implements NBMWebRTCPeer.Observer {
    VideoRenderer.Callbacks localRender;
    NBMWebRTCPeer nbmWebRTCPeer;
    NBMMediaConfiguration mediaConfiguration;
    VideoRenderer.Callbacks remoteRender;
    LooperExecutor executor;
    WSApi wsApi;
    String wsUri = "ws://192.168.1.59:8080/jsonrpc";

    @Override
    public void onCreate() {
        super.onCreate();

        executor = new LooperExecutor();
        executor.requestStart();
        wsApi = new WSApi(executor, wsUri) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                try {
                    sendRegister();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                super.onOpen(handshakedata);
            }

            @Override
            public void onRequest(JsonRpcRequest request) {
                super.onRequest(request);
            }

            @Override
            public void onResponse(JsonRpcResponse response) {
                super.onResponse(response);
            }

            @Override
            public void onNotification(JsonRpcNotification notification) {
                super.onNotification(notification);
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                super.onClose(code, reason, remote);
            }

            @Override
            public void onError(Exception e) {
                super.onError(e);
            }
        };
        try {
            wsApi.setTrustedCertificate(getAssets().open("server.crt"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        wsApi.connectWebSocket();

//        localRender = VideoRendererGui.create(72,72,25,25,RendererCommon.ScalingType.SCALE_ASPECT_FILL,false);
//        mediaConfiguration = new NBMMediaConfiguration();
//        nbmWebRTCPeer = new NBMWebRTCPeer(mediaConfiguration, this, localRender, this);
//        nbmWebRTCPeer.initialize();
    }

    public void sendRegister() throws JSONException {

        HashMap<String, Object> namedParameters = new HashMap<String, Object>();
        namedParameters.put("id", "register");
        namedParameters.put("name", "test");
        wsApi.send("echo", namedParameters, 0x00);
    }


    @Override
    public void onLocalSdpOfferGenerated(SessionDescription localSdpOffer, NBMPeerConnection connection) {
        nbmWebRTCPeer.generateOffer("connectionId", true);
    }

    @Override
    public void onLocalSdpAnswerGenerated(SessionDescription localSdpAnswer, NBMPeerConnection connection) {

    }

    @Override
    public void onIceCandidate(IceCandidate localIceCandidate, NBMPeerConnection connection) {

    }

    @Override
    public void onIceStatusChanged(PeerConnection.IceConnectionState state, NBMPeerConnection connection) {

    }

    @Override
    public void onRemoteStreamAdded(MediaStream stream, NBMPeerConnection connection) {
        nbmWebRTCPeer.attachRendererToRemoteStream(remoteRender, stream);
    }

    @Override
    public void onRemoteStreamRemoved(MediaStream stream, NBMPeerConnection connection) {

    }

    @Override
    public void onPeerConnectionError(String error) {

    }

    @Override
    public void onDataChannel(DataChannel dataChannel, NBMPeerConnection connection) {

    }

    @Override
    public void onBufferedAmountChange(long l, NBMPeerConnection connection, DataChannel channel) {

    }

    @Override
    public void onStateChange(NBMPeerConnection connection, DataChannel channel) {

    }

    @Override
    public void onMessage(DataChannel.Buffer buffer, NBMPeerConnection connection, DataChannel channel) {

    }


}
