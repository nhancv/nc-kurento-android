package com.nhancv.kurentoandroid.rtc_peer.kurento.models;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

/**
 * Created by nhancao on 6/19/17.
 */

public class CandidateModel implements Serializable {
    @SerializedName("sdpMid")
    private String sdpMid;
    @SerializedName("sdpMLineIndex")
    private int sdpMLineIndex;
    @SerializedName("candidate")
    private String sdp;

    public CandidateModel(String sdpMid, int sdpMLineIndex, String sdp) {
        this.sdpMid = sdpMid;
        this.sdpMLineIndex = sdpMLineIndex;
        this.sdp = sdp;
    }

    public String getSdpMid() {
        return sdpMid;
    }

    public int getSdpMLineIndex() {
        return sdpMLineIndex;
    }

    public String getSdp() {
        return sdp;
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}
