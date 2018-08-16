package io.reactivex.remote.internal;

/**
 * @hide
 *
 * Used internally by service side to call client
 */
public interface RemoteEventManager {

    String REMOTE_DATA_KEY = "RemoteData";
    String REMOTE_DATA_TYPE = "RemoteDataType";
    String REMOTE_DATA_EXTRA = "RemoteDataExtra";
    String REMOTE_DATA_LIST_SIZE = "ListSize";

    void subscribe(RemoteEventListener listener);

    void unsubscribe();

    void close();
}
