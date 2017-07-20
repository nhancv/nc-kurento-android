package com.nhancv.kurentoandroid.main;

import com.hannesdorfmann.mosby.mvp.MvpView;

/**
 * Created by nhancao on 7/20/17.
 */

public interface MainView extends MvpView {

    void logAndToast(String msg);

    void registerStatus(boolean success);

    void transactionToCalling(String name, boolean isHost);

    void incomingCalling(String name);

}
