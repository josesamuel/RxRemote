package io.reactivex.remote.internal;


import android.os.Bundle;

import remoter.annotations.Oneway;
import remoter.annotations.Remoter;

/**
 * @hide Used internally by {@link io.reactivex.remote.RemoteObservable} to receive events
 */
//@Remoter
public interface RemoteEventListener {

    /**
     * Called when remote servie sends a data
     */
    @Oneway
    void onRemoteEvent(Bundle remoteData);

    /**
     * Called when remote service notifies data stream is complete
     */
    @Oneway
    void onCompleted();

    /**
     * Called when remote service notifies of an error.
     */
    @Oneway
    void onError(Exception exception);
}
