package com.nhancv.kurentoandroid.rtc_peer.kurento.models.response;

/**
 * Created by nhancao on 6/19/17.
 */

public enum TypeResponse {
    ACCEPTED("accepted"),
    REJECTED("rejected");

    private String id;

    TypeResponse(String id) {
        this.id = id;
    }

    public static TypeResponse getType(String type) {
        for (TypeResponse typeResponse : TypeResponse.values()) {
            if (type.equals(typeResponse.getId())) {
                return typeResponse;
            }
        }
        return REJECTED;
    }

    public String getId() {
        return id;
    }
}
