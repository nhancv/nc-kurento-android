package com.nhancv.kurentoandroid.rtc_peer.kurento.models.response;

/**
 * Created by nhancao on 6/19/17.
 */

public enum IdResponse {

    PRESENTER_RESPONSE("presenterResponse"),
    ICE_CANDIDATE("iceCandidate"),
    VIEWER_RESPONSE("viewerResponse"),
    STOP_COMMUNICATION("stopCommunication"),
    CLOSE_ROOM_RESPONSE("closeRoomResponse"),

    UN_KNOWN("unknown");

    private String id;

    IdResponse(String id) {
        this.id = id;
    }

    public static IdResponse getIdRes(String idRes) {
        for (IdResponse idResponse : IdResponse.values()) {
            if (idRes.equals(idResponse.getId())) {
                return idResponse;
            }
        }
        return UN_KNOWN;
    }

    public String getId() {
        return id;
    }
}
