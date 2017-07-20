package com.nhancv.kurentoandroid.rtc_peer.kurento;

import android.util.Log;

import com.nhancv.webrtcpeer.rtc_comm.ws.BaseSocketCallback;
import com.nhancv.webrtcpeer.rtc_comm.ws.SocketService;
import com.nhancv.webrtcpeer.rtc_peer.RTCClient;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

/**
 * Created by nhancao on 7/18/17.
 */

public class KurentoOne2OneRTCClient implements RTCClient {
    private static final String TAG = KurentoOne2OneRTCClient.class.getSimpleName();

    private SocketService socketService;
    private String fromPeer, toPeer;
    private boolean isHost;

    public KurentoOne2OneRTCClient(SocketService socketService, String fromPeer, String toPeer, boolean isHost) {
        this.socketService = socketService;
        this.fromPeer = fromPeer;
        this.toPeer = toPeer;
        this.isHost = isHost;
    }

    public void connectToRoom(String host, BaseSocketCallback socketCallback) {
        socketService.connect(host, socketCallback);
    }

    @Override
    public void sendOfferSdp(SessionDescription sdp) {
        try {
            if (isHost) {
                JSONObject obj = new JSONObject();
                obj.put("id", "call");
                obj.put("from", fromPeer);
                obj.put("to", toPeer);
                obj.put("sdpOffer", sdp.description);

                socketService.sendMessage(obj.toString());

            } else {
                JSONObject obj = new JSONObject();
                obj.put("id", "incomingCallResponse");
                obj.put("from", fromPeer);
                obj.put("callResponse", "accept");
                obj.put("sdpOffer", sdp.description);

                socketService.sendMessage(obj.toString());
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void sendAnswerSdp(SessionDescription sdp) {

    }

    @Override
    public void sendLocalIceCandidate(IceCandidate iceCandidate) {
        try {
            JSONObject obj = new JSONObject();
            obj.put("id", "onIceCandidate");
            JSONObject candidate = new JSONObject();
            candidate.put("candidate", iceCandidate.sdp);
            candidate.put("sdpMid", iceCandidate.sdpMid);
            candidate.put("sdpMLineIndex", iceCandidate.sdpMLineIndex);
            obj.put("candidate", candidate);

            socketService.sendMessage(obj.toString());

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void sendLocalIceCandidateRemovals(IceCandidate[] candidates) {
        Log.e(TAG, "sendLocalIceCandidateRemovals: ");
    }

}
