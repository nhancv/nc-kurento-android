/*
 *  Copyright 2013 The WebRTC Project Authors. All rights reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

package com.nhancv.kurentoandroid.rtc_peer.apprtc;

import com.nhancv.webrtcpeer.rtc_peer.RTCClient;

/**
 * AppRTCClient is the interface representing an AppRTC client.
 */
public interface AppRTCClient extends RTCClient {
    /**
     * Asynchronously connect to an AppRTC room URL using supplied connection
     * parameters. Once connection is established onConnectedToRoom()
     * callback with room parameters is invoked.
     */
    void connectToRoom(RoomConnectionParameters connectionParameters);

    /**
     * Disconnect from room.
     */
    void disconnectFromRoom();

    /**
     * Struct holding the connection parameters of an AppRTC room.
     */
    class RoomConnectionParameters {
        public final String roomUrl;
        public final String roomId;
        public final boolean loopback;
        public final String urlParameters;

        public RoomConnectionParameters(
                String roomUrl, String roomId, boolean loopback, String urlParameters) {
            this.roomUrl = roomUrl;
            this.roomId = roomId;
            this.loopback = loopback;
            this.urlParameters = urlParameters;
        }

        public RoomConnectionParameters(String roomUrl, String roomId, boolean loopback) {
            this(roomUrl, roomId, loopback, null /* urlParameters */);
        }
    }
}
