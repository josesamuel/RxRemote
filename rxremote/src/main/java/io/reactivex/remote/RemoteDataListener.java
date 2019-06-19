package io.reactivex.remote;

/**
 * Listener on the client side to get notified when the data is updated from server side.
 * Use this if not using the observable
 */
public interface RemoteDataListener<T> {

    /**
     * Called when a data update is received
     */
    void onData(T data);
}
