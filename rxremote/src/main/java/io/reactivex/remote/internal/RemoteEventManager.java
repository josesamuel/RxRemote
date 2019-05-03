package io.reactivex.remote.internal;

import remoter.annotations.Oneway;
import remoter.annotations.Remoter;

/**
 * @hide
 *
 * Used internally by service side to call client
 */
@Remoter
public interface RemoteEventManager {

    String REMOTE_DATA_KEY = "RemoteData";
    String REMOTE_DATA_TYPE = "RemoteDataType";
    String REMOTE_DATA_EXTRA = "RemoteDataExtra";
    String REMOTE_DATA_LIST_SIZE = "ListSize";

    @Oneway
    void subscribe(RemoteEventListener listener);

    @Oneway
    void unsubscribe();

    @Oneway
    void close();
}
