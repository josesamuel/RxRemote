package io.reactivex.remote.internal;


import android.os.Bundle;

/**
 * @hide Used internally by {@link io.reactivex.remote.RemoteObservable} to receive events
 */
public interface LocalEventListener extends RemoteEventListener{

    /**
     * Called when remote servie sends a data
     */
    void onLocalEvent(Object localData);

}
