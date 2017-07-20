package com.nhancv.kurentoandroid.rtc_peer.kurento.models;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

/**
 * Created by nhancao on 6/19/17.
 */

public class IdModel implements Serializable {
    @SerializedName("id")
    protected String id;

    public static IdModel create(String id) {
        IdModel idModel = new IdModel();
        idModel.setId(id);
        return idModel;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}
