package com.nhancv.kurentoandroid;

import android.graphics.Point;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.nhancv.webrtcpeerandroid.LooperExecutor;
import com.nhancv.webrtcpeerandroid.NMediaConfiguration;
import com.nhancv.webrtcpeerandroid.NPeerConnection;
import com.nhancv.webrtcpeerandroid.NWebRTCPeer;

import org.java_websocket.client.DefaultSSLWebSocketClientFactory;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.RendererCommon;
import org.webrtc.SessionDescription;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoRendererGui;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;


public class MainActivity extends AppCompatActivity implements NWebRTCPeer.Observer {
    private static final String TAG = MainActivity.class.getName();
    // Local preview screen position before call is connected.
    private static final int LOCAL_X_CONNECTING = 0;
    private static final int LOCAL_Y_CONNECTING = 0;
    private static final int LOCAL_WIDTH_CONNECTING = 100;
    private static final int LOCAL_HEIGHT_CONNECTING = 100;
    // Local preview screen position after call is connected.
    private static final int LOCAL_X_CONNECTED = 72;
    private static final int LOCAL_Y_CONNECTED = 72;
    private static final int LOCAL_WIDTH_CONNECTED = 25;
    private static final int LOCAL_HEIGHT_CONNECTED = 25;
    // Remote video screen position
    private static final int REMOTE_X = 0;
    private static final int REMOTE_Y = 0;
    private static final int REMOTE_WIDTH = 100;
    private static final int REMOTE_HEIGHT = 100;
    private static final String host = "wss://local.beesightsoft.com:7003/one2one";
    NWebRTCPeer nbmWebRTCPeer;
    NMediaConfiguration mediaConfiguration;
    LooperExecutor executor;
    WebSocketClient client;
    String connectionId = "test1";
    boolean coodinator;
    private RendererCommon.ScalingType scalingType = RendererCommon.ScalingType.SCALE_ASPECT_FILL;
    private KeyStore keyStore;
    private GLSurfaceView videoView;
    private VideoRenderer.Callbacks localRender;
    private VideoRenderer.Callbacks remoteRender;

    private void connectWebSocket() {
        URI uri;
        try {
            uri = new URI(host);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }

        client = new WebSocketClient(uri) {
            @Override
            public void onOpen(ServerHandshake serverHandshake) {
                Log.i("Websocket", "Opened");
                try {
                    registerRequest();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onMessage(String s) {
                Log.i(TAG, "onMessage: " + s);
                try {
                    JSONObject obj = new JSONObject(s);
                    String command = obj.getString("id");
                    if (command.equals("registerResponse")) {
//                        {"id":"registerResponse","response":"accepted"}
                        registerResponse(obj.getString("response"));
                    } else if (command.equals("callResponse")) {
                        callResponse(obj.getString("response"), obj.getString("sdpAnswer"), connectionId);

                    } else if (command.equals("incomingCall")) {
//                        {"id":"incomingCall","from":"test1"}
                        String from = obj.getString("from");
                        incomingCall(from);

                    } else if (command.equals("startCommunication")) {
                        startCommunication(obj.getString("sdpAnswer"), connectionId);

                    } else if (command.equals("stopCommunication")) {
                        stopCommunication();

                    } else if (command.equals("iceCandidate")) {

                        JSONObject candidate = new JSONObject(obj.getString("candidate"));
                        String sdpMid = candidate.getString("sdpMid");
                        int sdpMLineIndex = candidate.getInt("sdpMLineIndex");
                        String sdp = candidate.getString("candidate");

                        iceCandidate(new IceCandidate(sdpMid, sdpMLineIndex, sdp));

                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onClose(int i, String s, boolean b) {
                Log.i("Websocket", "Closed " + s);
            }

            @Override
            public void onError(Exception e) {
                Log.i("Websocket", "Error " + e.getMessage());
            }
        };

        try {
            String scheme = uri.getScheme();
            if (scheme.equals("https") || scheme.equals("wss")) {
                setTrustedCertificate(getAssets().open("server.crt"));
                // Create a TrustManager that trusts the CAs in our KeyStore
                String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
                TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
                tmf.init(keyStore);

                // Create an SSLContext that uses our TrustManager
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, tmf.getTrustManagers(), null);
                WebSocketClient.WebSocketClientFactory webSocketClientFactory = new DefaultSSLWebSocketClientFactory(sslContext);
                client.setWebSocketFactory(webSocketClientFactory);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }
        client.connect();
    }

    public void setTrustedCertificate(InputStream inputFile) {
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            InputStream caInput = new BufferedInputStream(inputFile);
            Certificate ca = cf.generateCertificate(caInput);

            // Create a KeyStore containing our trusted CAs
            String keyStoreType = KeyStore.getDefaultType();
            keyStore = KeyStore.getInstance(keyStoreType);
            keyStore.load(null, null);
            keyStore.setCertificateEntry("ca", ca);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();


    }

    private void initView() {

        executor = new LooperExecutor();
        executor.requestStart();

        mediaConfiguration = new NMediaConfiguration();
        videoView = (GLSurfaceView) findViewById(R.id.glview_call);
        assert videoView != null;
        videoView.setPreserveEGLContextOnPause(true);
        videoView.setEGLContextClientVersion(2);
        videoView.setKeepScreenOn(true);

        VideoRendererGui.setView(videoView, new Runnable() {
            @Override
            public void run() {
                init();
            }
        });
        // local and remote render
        remoteRender = VideoRendererGui.create(
                REMOTE_X, REMOTE_Y,
                REMOTE_WIDTH, REMOTE_HEIGHT, scalingType, false);
        localRender = VideoRendererGui.create(
                LOCAL_X_CONNECTING, LOCAL_Y_CONNECTING,
                LOCAL_WIDTH_CONNECTING, LOCAL_HEIGHT_CONNECTING, scalingType, true);

    }

    private void init() {
        Point displaySize = new Point();
        getWindowManager().getDefaultDisplay().getSize(displaySize);
        connectWebSocket();
    }

    @Override
    public void onPause() {
        super.onPause();
        videoView.onPause();
    }

    //Handle process

    @Override
    public void onResume() {
        super.onResume();
        videoView.onResume();
    }

    public void registerRequest() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("id", "register");
        obj.put("name", "test");
        send(obj);
    }

    public void registerResponse(String response) {
        if (response.equals("accepted")) {
//            REGISTERED
            call();
        } else {
//            NOT_REGISTERED
        }
    }

    public void callResponse(String response, String sdpAnswer, String connectionId) {
        Log.e(TAG, "callResponse: ");
        if (!response.equals("accepted")) {
            Log.e(TAG, "callResponse: Call not accepted by peer. Closing call");
            stop(true);
        } else {
            startCommunication(sdpAnswer, connectionId);
        }

    }

    public void incomingCall(String from) {
        Log.e(TAG, "incomingCall: ");
        coodinator = false;
        connectionId = from;

        try {
            // If bussy just reject without disturbing user
            // TODO: 9/15/16 handle != NO_CALL state
//        if (callState != NO_CALL) {
//            JSONObject obj = new JSONObject();
//            obj.put("id", "incomingCallResponse");
//            obj.put("from", connectionId);
//            obj.put("callResponse", "reject");
//            obj.put("message", "bussy");
//            send(obj);
//            return;
//        }
            // TODO: 9/15/16 Set PROCESSING_CALL
            //Confirm accept true
            nbmWebRTCPeer = new NWebRTCPeer(mediaConfiguration, MainActivity.this, localRender, MainActivity.this);
            nbmWebRTCPeer.initialize();

            nbmWebRTCPeer.generateOffer(connectionId, true);
            //Confirm accept false
//            JSONObject obj = new JSONObject();
//            obj.put("id", "incomingCallResponse");
//            obj.put("from", connectionId);
//            obj.put("callResponse", "reject");
//            obj.put("message", "user declined");
//            send(obj);
//            stop(true);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void startCommunication(String sdpAnswer, String connectionId) {
        // TODO: 9/15/16 IN_CALL state
        nbmWebRTCPeer.processAnswer(new SessionDescription(SessionDescription.Type.ANSWER, sdpAnswer), connectionId);
    }

    public void stopCommunication() {
        stop(true);
    }

    public void iceCandidate(IceCandidate iceCandidate) {
        nbmWebRTCPeer.addRemoteIceCandidate(iceCandidate, connectionId);
    }

    public void call() {
        // TODO: 9/15/16 PROCESSING_CALL
        Log.e(TAG, "call: ");

        coodinator = true;
        nbmWebRTCPeer = new NWebRTCPeer(mediaConfiguration, MainActivity.this, localRender, MainActivity.this);
        nbmWebRTCPeer.initialize();
        nbmWebRTCPeer.generateOffer(connectionId, true);
    }

    public void stop(boolean messageSent) {
        // TODO: 9/15/16 NO_CALL state
        Log.e(TAG, "stop: ");
        if (nbmWebRTCPeer != null) {
            if (!messageSent) {
                try {
                    JSONObject msg = new JSONObject();
                    msg.put("id", "stop");
                    send(msg);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            client.close();
            nbmWebRTCPeer.stopLocalMedia();
            nbmWebRTCPeer.close();
            nbmWebRTCPeer = null;
        }
    }


    public boolean isWebSocketConnected() {
        return client != null && client.getConnection().isOpen();
    }

    protected void send(final JSONObject message) {
        executor.execute(new Runnable() {
            public void run() {
                if (isWebSocketConnected()) {
                    try {
                        Log.i(TAG, "send: " + message.toString());
                        client.send(message.toString().getBytes("UTF-8"));
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

    }

    @Override
    protected void onStop() {
        super.onStop();

    }

    @Override
    public void onLocalSdpOfferGenerated(SessionDescription localSdpOffer, NPeerConnection connection) {
        Log.e(TAG, "onLocalSdpOfferGenerated: " + localSdpOffer.type + " -" + localSdpOffer.description);
        if (coodinator) {
            try {
                JSONObject obj = new JSONObject();
                obj.put("id", "call");
                obj.put("from", "test");
                obj.put("to", connectionId);
                obj.put("sdpOffer", localSdpOffer.description);
                send(obj);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            try {
                JSONObject obj = new JSONObject();
                obj.put("id", "incomingCallResponse");
                obj.put("from", connectionId);
                obj.put("callResponse", "accept");
                obj.put("sdpOffer", localSdpOffer.description);
                send(obj);
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
    }

    @Override
    public void onLocalSdpAnswerGenerated(SessionDescription localSdpAnswer, NPeerConnection connection) {
//        Log.e(TAG, "onLocalSdpAnswerGenerated: " + localSdpAnswer.type + " -" + localSdpAnswer.description);
    }

    @Override
    public void onIceCandidate(IceCandidate localIceCandidate, NPeerConnection connection) {
        Log.e(TAG, "onIceCandidate: " + localIceCandidate);
        try {
            JSONObject obj = new JSONObject();
            obj.put("id", "onIceCandidate");
            JSONObject candidate = new JSONObject();
            candidate.put("candidate", localIceCandidate.sdp);
            candidate.put("sdpMid", localIceCandidate.sdpMid);
            candidate.put("sdpMLineIndex", localIceCandidate.sdpMLineIndex);
            obj.put("candidate", candidate);
            send(obj);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onIceStatusChanged(PeerConnection.IceConnectionState state, NPeerConnection connection) {
        Log.e(TAG, "onIceStatusChanged: " + state);
    }

    @Override
    public void onRemoteStreamAdded(MediaStream stream, NPeerConnection connection) {
        Log.e(TAG, "onRemoteStreamAdded: ");
        nbmWebRTCPeer.attachRendererToRemoteStream(remoteRender, stream);
        VideoRendererGui.update(remoteRender,
                REMOTE_X, REMOTE_Y,
                REMOTE_WIDTH, REMOTE_HEIGHT, scalingType, false);
        VideoRendererGui.update(localRender,
                LOCAL_X_CONNECTED, LOCAL_Y_CONNECTED,
                LOCAL_WIDTH_CONNECTED, LOCAL_HEIGHT_CONNECTED,
                scalingType, true);
    }

    @Override
    public void onRemoteStreamRemoved(MediaStream stream, NPeerConnection connection) {
        Log.e(TAG, "onRemoteStreamRemoved: ");
        VideoRendererGui.update(localRender,
                LOCAL_X_CONNECTING, LOCAL_Y_CONNECTING,
                LOCAL_WIDTH_CONNECTING, LOCAL_HEIGHT_CONNECTING,
                scalingType, true);

    }

    @Override
    public void onPeerConnectionError(String error) {
        Log.e(TAG, "onPeerConnectionError: ");
    }

    @Override
    public void onDataChannel(DataChannel dataChannel, NPeerConnection connection) {
        Log.e(TAG, "onDataChannel: ");
    }

    @Override
    public void onBufferedAmountChange(long l, NPeerConnection connection, DataChannel channel) {
        Log.e(TAG, "onBufferedAmountChange: ");
    }

    @Override
    public void onStateChange(NPeerConnection connection, DataChannel channel) {
        Log.e(TAG, "onStateChange: ");
    }

    @Override
    public void onMessage(DataChannel.Buffer buffer, NPeerConnection connection, DataChannel channel) {
        Log.e(TAG, "onMessage: " + buffer);
    }


}
