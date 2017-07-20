package com.nhancv.kurentoandroid.main;

import android.app.Application;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.hannesdorfmann.mosby.mvp.MvpBasePresenter;
import com.nhancv.kurentoandroid.rtc_peer.kurento.models.response.ServerResponse;
import com.nhancv.kurentoandroid.rtc_peer.kurento.models.response.TypeResponse;
import com.nhancv.kurentoandroid.util.RxScheduler;
import com.nhancv.webrtcpeer.rtc_comm.ws.BaseSocketCallback;
import com.nhancv.webrtcpeer.rtc_comm.ws.DefaultSocketService;
import com.nhancv.webrtcpeer.rtc_comm.ws.SocketService;

import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by nhancao on 7/20/17.
 */

public class MainPresenter extends MvpBasePresenter<MainView> {
    private static final String STREAM_HOST = "wss://192.168.2.77:6008/one2one";

    private Application application;
    private SocketService socketService;
    private Gson gson;
    private String userName;

    public MainPresenter(Application application) {
        this.application = application;
        this.socketService = new DefaultSocketService(application);
        this.gson = new Gson();
    }

    public void connectServer() {

        socketService.connect(STREAM_HOST, new BaseSocketCallback() {
            @Override
            public void onOpen(ServerHandshake serverHandshake) {
                super.onOpen(serverHandshake);
                RxScheduler.runOnUi(o -> {
                    getView().logAndToast("Connected");
                });
            }

            @Override
            public void onMessage(String serverResponse_) {
                super.onMessage(serverResponse_);
                try {
                    ServerResponse serverResponse = gson.fromJson(serverResponse_, ServerResponse.class);

                    switch (serverResponse.getIdRes()) {
                        case REGISTER_RESPONSE:
                            if (serverResponse.getTypeRes() == TypeResponse.REJECTED) {
                                RxScheduler.runOnUi(o -> {
                                    if (isViewAttached()) {
                                        getView().logAndToast(serverResponse.getMessage());
                                        getView().registerStatus(false);
                                    }
                                });
                            } else {
                                RxScheduler.runOnUi(o -> {
                                    if (isViewAttached()) {
                                        getView().registerStatus(true);
                                    }
                                });
                            }
                            break;
                        case INCOMING_CALL:
                            RxScheduler.runOnUi(o -> {
                                if (isViewAttached()) {
                                    getView().incomingCalling(serverResponse.getFrom());
                                }
                            });
                            break;
                    }
                } catch (JsonSyntaxException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onClose(int i, String s, boolean b) {
                super.onClose(i, s, b);
                RxScheduler.runOnUi(o -> {
                    getView().logAndToast("Closed");
                });
            }
        });

    }

    public void register(String name) {
        try {
            this.userName = name;

            JSONObject obj = new JSONObject();
            obj.put("id", "register");
            obj.put("name", name);

            socketService.sendMessage(obj.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }


}
