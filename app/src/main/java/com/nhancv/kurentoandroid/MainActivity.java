package com.nhancv.kurentoandroid;

import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

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

import fi.vtt.nubomedia.utilitiesandroid.LooperExecutor;
import fi.vtt.nubomedia.webrtcpeerandroid.NBMMediaConfiguration;
import fi.vtt.nubomedia.webrtcpeerandroid.NBMPeerConnection;
import fi.vtt.nubomedia.webrtcpeerandroid.NBMWebRTCPeer;

public class MainActivity extends AppCompatActivity implements NBMWebRTCPeer.Observer {
    private static final String TAG = MainActivity.class.getName();
    String host = "wss://192.168.1.59:7003/one2one";
    NBMWebRTCPeer nbmWebRTCPeer;
    NBMMediaConfiguration mediaConfiguration;
    LooperExecutor executor;
    WebSocketClient client;
    private KeyStore keyStore;
    private GLSurfaceView vsv;
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
                    sendRegister();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onMessage(String s) {
                final String message = s;

                try {
                    JSONObject obj = new JSONObject(s);
                    String command = obj.getString("id");
                    if (command.equals("incomingCall")) {
//                        {"id":"incomingCall","from":"test1"}
                        String from = obj.getString("from");
                        nbmWebRTCPeer.generateOffer(from, true);




                    }else if (command.equals("registerResponse")) {
//                        {"id":"registerResponse","response":"accepted"}

                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }


                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.e(TAG, "run: " + message);
                    }
                });
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

        vsv = (GLSurfaceView) findViewById(R.id.glview_call);
        vsv.setPreserveEGLContextOnPause(true);
        vsv.setKeepScreenOn(true);
        VideoRendererGui.setView(vsv, new Runnable() {
            @Override
            public void run() {
                executor = new LooperExecutor();
                executor.requestStart();

                localRender = VideoRendererGui.create(72, 72, 25, 25, RendererCommon.ScalingType.SCALE_ASPECT_FILL, false);
                mediaConfiguration = new NBMMediaConfiguration();
                nbmWebRTCPeer = new NBMWebRTCPeer(mediaConfiguration, MainActivity.this, localRender, MainActivity.this);
                nbmWebRTCPeer.initialize();
                nbmWebRTCPeer.enableVideo(true);
                nbmWebRTCPeer.startLocalMedia();
                connectWebSocket();


            }
        });

    }

    @Override
    public void onPause() {
        super.onPause();
        vsv.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        vsv.onResume();
    }

    public void sendRegister() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("id", "register");
        obj.put("name", "test");
        send(obj);
    }

    public boolean isWebSocketConnected() {
        return client != null && client.getConnection().isOpen();
    }

    protected void send(final JSONObject message) {
        executor.execute(new Runnable() {
            public void run() {
                if (isWebSocketConnected()) {
                    try {
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
    public void onLocalSdpOfferGenerated(SessionDescription localSdpOffer, NBMPeerConnection connection) {
        Log.e(TAG, "onLocalSdpOfferGenerated: " + localSdpOffer);
    }

    @Override
    public void onLocalSdpAnswerGenerated(SessionDescription localSdpAnswer, NBMPeerConnection connection) {
        Log.e(TAG, "onLocalSdpAnswerGenerated: " + localSdpAnswer);
    }

    @Override
    public void onIceCandidate(IceCandidate localIceCandidate, NBMPeerConnection connection) {
        Log.e(TAG, "onIceCandidate: " + localIceCandidate);
    }

    @Override
    public void onIceStatusChanged(PeerConnection.IceConnectionState state, NBMPeerConnection connection) {
        Log.e(TAG, "onIceStatusChanged: " + state);
    }

    @Override
    public void onRemoteStreamAdded(MediaStream stream, NBMPeerConnection connection) {
        Log.e(TAG, "onRemoteStreamAdded: ");
        nbmWebRTCPeer.attachRendererToRemoteStream(remoteRender, stream);
    }

    @Override
    public void onRemoteStreamRemoved(MediaStream stream, NBMPeerConnection connection) {
        Log.e(TAG, "onRemoteStreamRemoved: ");
    }

    @Override
    public void onPeerConnectionError(String error) {
        Log.e(TAG, "onPeerConnectionError: ");
    }

    @Override
    public void onDataChannel(DataChannel dataChannel, NBMPeerConnection connection) {
        Log.e(TAG, "onDataChannel: ");
    }

    @Override
    public void onBufferedAmountChange(long l, NBMPeerConnection connection, DataChannel channel) {
        Log.e(TAG, "onBufferedAmountChange: ");
    }

    @Override
    public void onStateChange(NBMPeerConnection connection, DataChannel channel) {
        Log.e(TAG, "onStateChange: ");
    }

    @Override
    public void onMessage(DataChannel.Buffer buffer, NBMPeerConnection connection, DataChannel channel) {
        Log.e(TAG, "onMessage: " + buffer);
    }


}
