package com.nhancv.kurentoandroid.ui;

import android.content.DialogInterface;
import android.graphics.Point;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.gc.materialdesign.views.ButtonFlat;
import com.nhancv.kurentoandroid.util.ICollections;
import com.nhancv.kurentoandroid.util.NDialog;
import com.nhancv.kurentoandroid.R;
import com.nhancv.kurentoandroid.util.Utils;
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

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class One2ManyViewerActivity extends AppCompatActivity implements NWebRTCPeer.Observer {
    private static final String TAG = One2ManyViewerActivity.class.getName();
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
    boolean coordinator;
    @BindView(R.id.vGLSurfaceViewCall)
    GLSurfaceView vGLSurfaceViewCall;
    @BindView(R.id.vRegister)
    View vRegister;
    @BindView(R.id.vCall)
    View vCall;
    @BindView(R.id.btRegister)
    ButtonFlat btRegister;
    @BindView(R.id.btCall)
    ButtonFlat btCall;
    @BindView(R.id.etCallerId)
    EditText etCallerId;
    @BindView(R.id.etCalleeId)
    EditText etCalleeId;
    private NWebRTCPeer nbmWebRTCPeer;
    private NMediaConfiguration mediaConfiguration;
    private LooperExecutor executor;
    private WebSocketClient client;
    private String self, connectionId;
    private KeyStore keyStore;
    private VideoRenderer.Callbacks localRender;
    private VideoRenderer.Callbacks remoteRender;
    private Reg regState = Reg.NOT_REGISTERED;
    private Calling callState = Calling.NO_CALL;
    private RendererCommon.ScalingType scalingType = RendererCommon.ScalingType.SCALE_ASPECT_FILL;

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
                btRegister.setEnabled(true);
            }

            @Override
            public void onMessage(String s) {
                Log.i(TAG, "onMessage: " + s);
                try {
                    JSONObject obj = new JSONObject(s);
                    String command = obj.getString("id");
                    if (command.equals("registerResponse")) {
                        registerResponse(obj.getString("response"));
                    } else if (command.equals("callResponse")) {
                        callResponse(obj.getString("response"), obj.getString("sdpAnswer"), connectionId);

                    } else if (command.equals("incomingCall")) {
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
        } catch (IOException | NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
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
        setContentView(R.layout.activity_one2one);
        ButterKnife.bind(this);
        btRegister.setEnabled(false);
        connectWebSocket();
    }

    @OnClick(R.id.btRegister)
    public void btRegisterOnClick(View view) {
        Utils.hideKeyboard(view);
        if (Utils.validateInput(etCallerId)) {
            self = etCallerId.getText().toString();
            try {
                registerRequest(self);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    @OnClick(R.id.btCall)
    public void btCallOnClick(View view) {
        Utils.hideKeyboard(view);
        if (Utils.validateInput(etCalleeId)) {
            connectionId = etCalleeId.getText().toString();
            call();
        }
    }

    /**
     * Init view component for initialize calling
     */
    private void initRTCComponent() {
        executor = new LooperExecutor();
        executor.requestStart();

        mediaConfiguration = new NMediaConfiguration();
        vGLSurfaceViewCall.setPreserveEGLContextOnPause(true);
        vGLSurfaceViewCall.setEGLContextClientVersion(2);
        vGLSurfaceViewCall.setKeepScreenOn(true);

        VideoRendererGui.setView(vGLSurfaceViewCall, () -> {
            Point displaySize = new Point();
            getWindowManager().getDefaultDisplay().getSize(displaySize);
            //Visible call view
            vCall.setVisibility(View.VISIBLE);

        });
        // local and remote render
        remoteRender = VideoRendererGui.create(
                REMOTE_X, REMOTE_Y,
                REMOTE_WIDTH, REMOTE_HEIGHT, scalingType, false);
        localRender = VideoRendererGui.create(
                LOCAL_X_CONNECTING, LOCAL_Y_CONNECTING,
                LOCAL_WIDTH_CONNECTING, LOCAL_HEIGHT_CONNECTING, scalingType, true);

    }

    @Override
    public void onPause() {
        super.onPause();
        if (callState == Calling.IN_CALL) {
            vGLSurfaceViewCall.onPause();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (callState == Calling.IN_CALL) {
            vGLSurfaceViewCall.onResume();
        }
    }

    public void registerRequest(String callerId) throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("id", "register");
        obj.put("name", callerId);
        send(obj);
    }

    //Handle process

    /**
     * Handle register
     *
     * @param response
     */
    public void registerResponse(String response) {
        if (response.equals("accepted")) {
            regState = Reg.REGISTERED;
            initRTCComponent();
        } else {
            regState = Reg.NOT_REGISTERED;
        }
    }

    /**
     * Handle call response
     *
     * @param response
     * @param sdpAnswer
     * @param connectionId
     */
    public void callResponse(String response, String sdpAnswer, String connectionId) {
        Log.e(TAG, "callResponse: ");
        if (!response.equals("accepted")) {
            Log.e(TAG, "callResponse: Call not accepted by peer. Closing call");
            stop(true);
        } else {
            startCommunication(sdpAnswer, connectionId);
        }

    }

    /**
     * Handle when has incoming call
     *
     * @param from
     */
    public void incomingCall(String from) {
        coordinator = false;
        connectionId = from;

        // If busy just reject without disturbing user
        if (callState != Calling.NO_CALL) {
            try {
                JSONObject obj = new JSONObject();
                obj.put("id", "incomingCallResponse");
                obj.put("from", connectionId);
                obj.put("callResponse", "reject");
                obj.put("message", "busy");
                send(obj);
            } catch (JSONException e1) {
                e1.printStackTrace();
            }
            return;
        }
        NDialog.showConfirmDialog(this, "Incomming call", from + " call you?", new ICollections.IDialogConfirmButtonImpl() {
            @Override
            public void positive(DialogInterface dialog, View button, View rootView) {
                callState = Calling.PROCESSING_CALL;
                nbmWebRTCPeer = new NWebRTCPeer(mediaConfiguration, One2ManyViewerActivity.this, localRender, One2ManyViewerActivity.this);
                nbmWebRTCPeer.initialize();
                nbmWebRTCPeer.generateOffer(connectionId, true);
            }

            @Override
            public void negative(DialogInterface dialog, View button, View rootView) {
                try {
                    JSONObject obj = new JSONObject();
                    obj.put("id", "incomingCallResponse");
                    obj.put("from", connectionId);
                    obj.put("callResponse", "reject");
                    obj.put("message", "user declined");
                    send(obj);
                    stop(true);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }).show();
    }

    /**
     * Start communication
     *
     * @param sdpAnswer
     * @param connectionId
     */
    public void startCommunication(String sdpAnswer, String connectionId) {
        callState = Calling.IN_CALL;
        vRegister.setVisibility(View.GONE);
        vGLSurfaceViewCall.setVisibility(View.VISIBLE);
        nbmWebRTCPeer.processAnswer(new SessionDescription(SessionDescription.Type.ANSWER, sdpAnswer), connectionId);
    }

    /**
     * Stop communication
     */
    public void stopCommunication() {
        vRegister.setVisibility(View.VISIBLE);
        vGLSurfaceViewCall.setVisibility(View.GONE);

        stop(true);
    }

    /**
     * Handle ice candidate
     *
     * @param iceCandidate
     */
    public void iceCandidate(IceCandidate iceCandidate) {
        nbmWebRTCPeer.addRemoteIceCandidate(iceCandidate, connectionId);
    }

    /**
     * Handle call to connectionId
     */
    public void call() {
        callState = Calling.PROCESSING_CALL;
        coordinator = true;
        nbmWebRTCPeer = new NWebRTCPeer(mediaConfiguration, One2ManyViewerActivity.this, localRender, One2ManyViewerActivity.this);
        nbmWebRTCPeer.initialize();
        nbmWebRTCPeer.generateOffer(connectionId, true);
    }

    /**
     * Stop webrtc peer
     *
     * @param messageSent
     */
    public void stop(boolean messageSent) {
        callState = Calling.NO_CALL;
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
        executor.execute(() -> {
            if (isWebSocketConnected()) {
                try {
                    Log.i(TAG, "send: " + message.toString());
                    client.send(message.toString().getBytes("UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        });

    }

    @Override
    public void onLocalSdpOfferGenerated(SessionDescription localSdpOffer, NPeerConnection connection) {
        Log.e(TAG, "onLocalSdpOfferGenerated: " + localSdpOffer.type + " -" + localSdpOffer.description);
        if (coordinator) {
            try {
                JSONObject obj = new JSONObject();
                obj.put("id", "call");
                obj.put("from", self);
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

    enum Reg {
        REGISTERED,
        NOT_REGISTERED
    }

    enum Calling {
        NO_CALL,
        PROCESSING_CALL,
        IN_CALL
    }


}
