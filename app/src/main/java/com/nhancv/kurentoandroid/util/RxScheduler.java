package com.nhancv.kurentoandroid.util;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by nhancao on 7/20/17.
 */

public class RxScheduler {

    /**
     * Apply scheduler:
     * observable
     * .subscribeOn(Schedulers.computation())
     * .observeOn(AndroidSchedulers.mainThread());
     */
    public static <T> Observable.Transformer<T, T> applyLogicSchedulers() {
        return observable -> observable
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * Apply scheduler io for call api
     */
    public static <T> Observable.Transformer<T, T> applyIoSchedulers() {
        return observable -> observable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * Apply scheduler with newThread:
     * observable
     * .subscribeOn(Schedulers.newThread())
     * .observeOn(AndroidSchedulers.mainThread());
     */
    public static <T> Observable.Transformer<T, T> applyNewThreadSchedulers() {
        return observable -> observable
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * onNext with subscriber and item
     */
    public static <T> void onNext(Subscriber<T> subscriber, T item) {
        if (!subscriber.isUnsubscribed()) {
            subscriber.onNext(item);
        }
    }

    /**
     * onError with subscriber and exception
     */
    public static void onError(Subscriber<?> subscriber, Exception e) {
        if (!subscriber.isUnsubscribed()) {
            subscriber.onError(e);
        }
    }

    /**
     * onCompleted with subscriber
     */
    public static void onCompleted(Subscriber<?> subscriber) {
        if (!subscriber.isUnsubscribed()) {
            subscriber.onCompleted();
        }
    }

    /**
     * onStop with subscription
     */
    public static void onStop(Subscription subscription) {
        if (subscription != null && !subscription.isUnsubscribed()) {
            subscription.unsubscribe();
        }
    }


    /**
     * Run method in aSync mode
     *
     * @return Subscription
     */
    public static <T> Subscription aSyncTask(Observable.OnSubscribe<T> onSubscribe) {
        return aSyncTask(onSubscribe, null);
    }

    /**
     * Run method in aSync mode
     *
     * @return Subscription
     */
    public static <T> Subscription aSyncTask(Observable.OnSubscribe<T> onSubscribe,
                                             rx.functions.Action1<? super T> onNext) {
        return aSyncTask(onSubscribe, onNext, null);
    }

    /**
     * Run method in aSync mode
     */
    public static <T> Subscription aSyncTask(Observable.OnSubscribe<T> onSubscribe,
                                             rx.functions.Action1<? super T> onNext,
                                             rx.functions.Action1<Throwable> onError) {
        return aSyncTask(onSubscribe, onNext, onError, null);
    }

    /**
     * Run method in aSync mode
     */
    public static <T> Subscription aSyncTask(Observable.OnSubscribe<T> onSubscribe,
                                             rx.functions.Action1<? super T> onNext,
                                             rx.functions.Action1<Throwable> onError,
                                             rx.functions.Action0 onCompleted) {
        return safeSubscribe(Observable.create(onSubscribe).compose(RxScheduler.applyLogicSchedulers()),
                             onNext, onError, onCompleted);
    }

    /**
     * Run method in new thread with async mode
     */
    public static <T> Subscription aSyncTaskNewThread(Observable.OnSubscribe<T> onSubscribe) {
        return aSyncTaskNewThread(onSubscribe, null);
    }

    /**
     * Run method in new thread with async mode
     */
    public static <T> Subscription aSyncTaskNewThread(Observable.OnSubscribe<T> onSubscribe,
                                                      rx.functions.Action1<? super T> onNext) {
        return aSyncTaskNewThread(onSubscribe, onNext, null, null);
    }

    /**
     * Run method in new thread with async mode
     */
    public static <T> Subscription aSyncTaskNewThread(Observable.OnSubscribe<T> onSubscribe,
                                                      rx.functions.Action1<? super T> onNext,
                                                      rx.functions.Action1<Throwable> onError) {
        return aSyncTaskNewThread(onSubscribe, onNext, onError, null);
    }

    /**
     * Run method in new thread with async mode
     */
    public static <T> Subscription aSyncTaskNewThread(Observable.OnSubscribe<T> onSubscribe,
                                                      rx.functions.Action1<? super T> onNext,
                                                      rx.functions.Action1<Throwable> onError,
                                                      rx.functions.Action0 onCompleted) {
        return safeSubscribe(Observable.create(onSubscribe).compose(RxScheduler.applyNewThreadSchedulers()),
                             onNext, onError, onCompleted);
    }

    /**
     * Run method on Ui
     *
     * @return Subscription
     */
    public static Subscription runOnUi(rx.functions.Action1<? super Object> onNext) {
        return safeSubscribe(Observable
                                     .create(subscriber -> subscriber.onNext(true))
                                     .compose(RxScheduler.applyLogicSchedulers()),
                             onNext);
    }

    /**
     * Run method with sync mode
     *
     * @return Subscription
     */
    public static <T> Subscription syncTask(Observable.OnSubscribe<T> onSubscribe) {
        return safeSubscribe(Observable.create(onSubscribe));
    }

    /**
     * Safe Subscribe
     */
    public static <T> Subscription safeSubscribe(Observable<T> observable) {
        return safeSubscribe(observable, null);
    }

    /**
     * Safe Subscribe
     */
    public static <T> Subscription safeSubscribe(Observable<T> observable,
                                                 rx.functions.Action1<? super T> onNext) {
        return safeSubscribe(observable, onNext, null);
    }

    /**
     * Safe Subscribe
     */
    public static <T> Subscription safeSubscribe(Observable<T> observable,
                                                 rx.functions.Action1<? super T> onNext,
                                                 rx.functions.Action1<Throwable> onError) {
        return safeSubscribe(observable, onNext, onError, null);
    }

    /**
     * Safe Subscribe
     */
    public static <T> Subscription safeSubscribe(Observable<T> observable,
                                                 rx.functions.Action1<? super T> onNext,
                                                 rx.functions.Action1<Throwable> onError,
                                                 rx.functions.Action0 onCompleted) {
        if (onNext == null) onNext = t -> {
        };
        if (onError == null) onError = throwable -> {
        };
        if (onCompleted == null) onCompleted = () -> {
        };

        return observable.subscribe(onNext, onError, onCompleted);
    }

}
