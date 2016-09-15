package com.nhancv.kurentoandroid;

import android.util.Log;

import org.java_websocket.client.DefaultSSLWebSocketClientFactory;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;

import java.io.BufferedInputStream;
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
import java.util.HashMap;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import fi.vtt.nubomedia.jsonrpcwsandroid.JsonRpcNotification;
import fi.vtt.nubomedia.jsonrpcwsandroid.JsonRpcRequest;
import fi.vtt.nubomedia.jsonrpcwsandroid.JsonRpcResponse;
import fi.vtt.nubomedia.jsonrpcwsandroid.JsonRpcWebSocketClient;
import fi.vtt.nubomedia.utilitiesandroid.LooperExecutor;

/**
 * Created by nhancao on 9/15/16.
 */
public abstract class WSApi implements JsonRpcWebSocketClient.WebSocketConnectionEvents {
    private static final String TAG = WSApi.class.getName();
    protected JsonRpcWebSocketClient client = null;
    protected LooperExecutor executor = null;
    protected String wsUri = null;
    protected WebSocketClient.WebSocketClientFactory webSocketClientFactory = null;
    private KeyStore keyStore;

    /**
     * Constructor that initializes required instances and parameters for the API calls.
     * WebSocket connections are not established in the constructor. User is responsible
     * for opening, closing and checking if the connection is open through the corresponding
     * API calls.
     *
     * @param executor is the asynchronous UI-safe executor for tasks.
     * @param uri      is the web socket link to the room web services.
     */
    public WSApi(LooperExecutor executor, String uri) {
        this.executor = executor;
        this.wsUri = uri;
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

    /**
     * Opens a web socket connection to the predefined URI as provided in the constructor.
     * The method responds immediately, whether or not the connection is opened.
     * The method isWebSocketConnected() should be called to ensure that the connection is open.
     */
    public void connectWebSocket() {
        try {
            if (isWebSocketConnected()) {
                return;
            }
            // Switch to SSL web socket client factory if secure protocol detected
            String scheme = null;
            try {
                scheme = new URI(wsUri).getScheme();
                if (scheme.equals("https") || scheme.equals("wss")) {

                    // Create a TrustManager that trusts the CAs in our KeyStore
                    String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
                    TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
                    tmf.init(keyStore);

                    // Create an SSLContext that uses our TrustManager
                    SSLContext sslContext = SSLContext.getInstance("TLS");
                    sslContext.init(null, tmf.getTrustManagers(), null);
                    webSocketClientFactory = new DefaultSSLWebSocketClientFactory(sslContext);
                }
            } catch (URISyntaxException e) {
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (KeyStoreException e) {
                e.printStackTrace();
            } catch (KeyManagementException e) {
                e.printStackTrace();
            }

            URI uri = new URI(wsUri);
            client = new JsonRpcWebSocketClient(uri, this, executor);
            if (webSocketClientFactory != null) {
                client.setWebSocketFactory(webSocketClientFactory);
            }
            executor.execute(new Runnable() {
                public void run() {
                    client.connect();
                }
            });
        } catch (Exception exc) {
            Log.e(TAG, "connectWebSocket", exc);
        }
    }

    /**
     * Method to check if the web socket connection is connected.
     *
     * @return true if the connection state is connected, and false otherwise.
     */
    public boolean isWebSocketConnected() {
        if (client != null) {
            return (client.getConnectionState().equals(JsonRpcWebSocketClient.WebSocketConnectionState.CONNECTED));
        } else {
            return false;
        }
    }

    /**
     * Attempts to close the web socket connection asynchronously.
     */
    public void disconnectWebSocket() {
        try {
            if (client != null) {
                executor.execute(new Runnable() {
                    public void run() {
                        client.disconnect(false);
                    }
                });
            }
        } catch (Exception exc) {
            Log.e(TAG, "disconnectWebSocket", exc);
        } finally {
            ;
        }
    }

    /**
     * Send message from json object
     *
     * @param message
     */
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

    /**
     * @param method
     * @param namedParameters
     * @param id
     */
    protected void send(String method, HashMap<String, Object> namedParameters, int id) {

        try {
            final JsonRpcRequest request = new JsonRpcRequest();
            request.setMethod(method);
            if (namedParameters != null) {
                request.setNamedParams(namedParameters);
            }
            if (id >= 0) {
                request.setId(new Integer(id));
            }
            executor.execute(new Runnable() {
                public void run() {
                    if (isWebSocketConnected()) {

                        client.sendRequest(request);
                    }
                }
            });
        } catch (Exception exc) {
            Log.e(TAG, "send: " + method, exc);
        }
    }


    /* WEB SOCKET CONNECTION EVENTS */

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        Log.e(TAG, "onOpen: " + handshakedata);
    }

    @Override
    public void onRequest(JsonRpcRequest request) {
        Log.e(TAG, "onRequest: " + request);
    }

    @Override
    public void onResponse(JsonRpcResponse response) {
        Log.e(TAG, "onResponse: " + response);
    }

    @Override
    public void onNotification(JsonRpcNotification notification) {
        Log.e(TAG, "onNotification: " + notification);
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        Log.e(TAG, "onClose: " + code + " " + reason + " " + remote);
    }

    @Override
    public void onError(Exception e) {
        Log.e(TAG, "onError: " + e.getMessage(), e);
    }

}
