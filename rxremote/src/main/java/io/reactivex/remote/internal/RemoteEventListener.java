package io.reactivex.remote.internal;


import android.os.Bundle;

/**
 * @hide Used internally by {@link io.reactivex.remote.RemoteObservable} to receive events
 */
public interface RemoteEventListener {

    /**
     * Called when remote servie sends a data
     */
    void onRemoteEvent(Bundle remoteData);

    /**
     * Called when remote service notifies data stream is complete
     */
    void onCompleted();

    /**
     * Called when remote service notifies of an error.
     */
    void onError(Exception exception);
}
