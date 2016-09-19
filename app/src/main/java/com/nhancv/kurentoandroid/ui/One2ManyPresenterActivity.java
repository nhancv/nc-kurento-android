package com.nhancv.kurentoandroid.ui;

import android.graphics.Point;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import com.nhancv.kurentoandroid.R;
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


public class One2ManyPresenterActivity extends AppCompatActivity implements NWebRTCPeer.Observer {
    private static final String TAG = One2ManyPresenterActivity.class.getName();
    private static final int LOCAL_X_CONNECTING = 0;
    private static final int LOCAL_Y_CONNECTING = 0;
    private static final int LOCAL_WIDTH_CONNECTING = 100;
    private static final int LOCAL_HEIGHT_CONNECTING = 100;
    private static final String host = "wss://local.beesightsoft.com:7002/one2many";
    @BindView(R.id.vGLSurfaceViewCall)
    GLSurfaceView vGLSurfaceViewCall;
    @BindView(R.id.btClose)
    Button btClose;
    private NWebRTCPeer nbmWebRTCPeer;
    private NMediaConfiguration mediaConfiguration;
    private LooperExecutor executor;
    private WebSocketClient client;
    private String connectionId = "presenter";
    private KeyStore keyStore;
    private VideoRenderer.Callbacks localRender;
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
                createWebRTCPeer();
            }

            @Override
            public void onMessage(String s) {
                Log.i(TAG, "onMessage: " + s);
                try {
                    JSONObject obj = new JSONObject(s);
                    String command = obj.getString("id");
                    if (command.equals("presenterResponse")) {
                        presenterResponse(obj);
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

    /**
     * Set trusted certificate from file
     *
     * @param inputFile
     */
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
        setContentView(R.layout.activity_one2many_presenter);
        ButterKnife.bind(this);
        executor = new LooperExecutor();
        executor.requestStart();
        initRTCComponent();
    }

    /**
     * Init view component for initialize calling
     */
    private void initRTCComponent() {
        mediaConfiguration = new NMediaConfiguration();
        vGLSurfaceViewCall.setPreserveEGLContextOnPause(true);
        vGLSurfaceViewCall.setEGLContextClientVersion(2);
        vGLSurfaceViewCall.setKeepScreenOn(true);
        VideoRendererGui.setView(vGLSurfaceViewCall, () -> {
            Point displaySize = new Point();
            getWindowManager().getDefaultDisplay().getSize(displaySize);
            connectWebSocket();
        });
        // local and remote render
        localRender = VideoRendererGui.create(
                LOCAL_X_CONNECTING, LOCAL_Y_CONNECTING,
                LOCAL_WIDTH_CONNECTING, LOCAL_HEIGHT_CONNECTING, scalingType, true);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (vGLSurfaceViewCall != null) {
            vGLSurfaceViewCall.onPause();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (vGLSurfaceViewCall != null) {
            vGLSurfaceViewCall.onResume();
        }
    }

    @OnClick(R.id.btClose)
    public void btCloseOnClick() {
        onBackPressed();
    }
    //Handle process

    private void presenterResponse(JSONObject obj) throws JSONException {
        String response = obj.getString("response");
        if (response.equals("accepted")) {
            String sdpAnswer = obj.getString("sdpAnswer");
            nbmWebRTCPeer.processAnswer(new SessionDescription(SessionDescription.Type.ANSWER, sdpAnswer), connectionId);
        } else {
            Log.e(TAG, "presenterResponse: rejected");
            runOnUiThread(() -> {
                Toast.makeText(One2ManyPresenterActivity.this, "rejected", Toast.LENGTH_SHORT).show();
                onBackPressed();
            });
        }
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
     * Handle create new webrtcpeer instance
     */
    public void createWebRTCPeer() {
        nbmWebRTCPeer = new NWebRTCPeer(mediaConfiguration, One2ManyPresenterActivity.this, localRender, One2ManyPresenterActivity.this);
//        nbmWebRTCPeer.setStreamMode(NWebRTCPeer.StreamMode.SEND_ONLY);
        nbmWebRTCPeer.generateOffer(connectionId, true);
    }

    /**
     * Stop webrtc peer
     *
     * @param messageSent
     */
    public void stop(boolean messageSent) {
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
            nbmWebRTCPeer.stopLocalMedia();
            nbmWebRTCPeer.close();
            nbmWebRTCPeer = null;
        }
    }

    /**
     * Check socked is connected
     *
     * @return
     */
    public boolean isWebSocketConnected() {
        return client != null && client.getConnection().isOpen();
    }

    /**
     * Send json message via socket
     *
     * @param message
     */
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
        try {
            JSONObject obj = new JSONObject();
            obj.put("id", "presenter");
            obj.put("sdpOffer", localSdpOffer.description);
            send(obj);
        } catch (JSONException e) {
            e.printStackTrace();
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
    }

    @Override
    public void onRemoteStreamAdded(MediaStream stream, NPeerConnection connection) {
    }

    @Override
    public void onRemoteStreamRemoved(MediaStream stream, NPeerConnection connection) {
    }

    @Override
    public void onPeerConnectionError(String error) {
    }

    @Override
    public void onDataChannel(DataChannel dataChannel, NPeerConnection connection) {
    }

    @Override
    public void onBufferedAmountChange(long l, NPeerConnection connection, DataChannel channel) {
    }

    @Override
    public void onStateChange(NPeerConnection connection, DataChannel channel) {
    }

    @Override
    public void onMessage(DataChannel.Buffer buffer, NPeerConnection connection, DataChannel channel) {
        Log.e(TAG, "onMessage: " + buffer);
    }

    @Override
    public void onBackPressed() {
        stop(true);
        client.close();
        super.onBackPressed();
    }


}
