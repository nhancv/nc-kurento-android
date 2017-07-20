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

public class KurentoViewerRTCClient implements RTCClient {
    private static final String TAG = KurentoViewerRTCClient.class.getSimpleName();

    private SocketService socketService;

    public KurentoViewerRTCClient(SocketService socketService) {
        this.socketService = socketService;
    }

    public void connectToRoom(String host, BaseSocketCallback socketCallback) {
        socketService.connect(host, socketCallback);
    }

    @Override
    public void sendOfferSdp(SessionDescription sdp) {
        try {
            JSONObject obj = new JSONObject();
            obj.put("id", "viewer");
            obj.put("sdpOffer", sdp.description);

            socketService.sendMessage(obj.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void sendAnswerSdp(SessionDescription sdp) {
        Log.e(TAG, "sendAnswerSdp: ");
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
