package io.reactivex.remote.internal;

/**
 * @hide
 *
 * Used internally by service side to call client
 */
public interface RemoteEventManager {

    String REMOTE_DATA_KEY = "RemoteData";
    String REMOTE_DATA_TYPE = "RemoteDataType";

    void subscribe(RemoteEventListener listener);

    void unsubscribe();
}
