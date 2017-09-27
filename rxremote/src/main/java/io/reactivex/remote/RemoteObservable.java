package io.reactivex.remote;

import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
import android.util.Log;

import io.reactivex.remote.internal.RemoteDataType;
import io.reactivex.remote.internal.RemoteEventListener;
import io.reactivex.remote.internal.RemoteEventManager;
import io.reactivex.remote.internal.RemoteEventManager_Proxy;
import io.reactivex.remote.internal.RemoteEventManager_Stub;
import rx.Observable;
import rx.functions.Action0;
import rx.subjects.PublishSubject;

/**
 * {@link Observable} across android remote services.
 * <p>
 * This is a {@link Parcelable} which can be passed through remoter service
 * aidl or <a href=\"https://bit.ly/Remoter\">Remoter</a> interfaces
 * and then get an {@link Observable} from this class at the client side.
 *
 * @param <T> Supported types are {@link String}, {@link Byte}, {@link Short}, {@link Integer}, {@link Long},
 *            {@link Float}, {@link Double}, {@link Boolean}, {@link Parcelable},
 *            or any class annotated with <a href=\"https://github.com/johncarl81/parceler\">@Parcel</a>
 */
public class RemoteObservable<T> implements Parcelable {

    public static final Creator<RemoteObservable> CREATOR = new Creator<RemoteObservable>() {
        @Override
        public RemoteObservable createFromParcel(android.os.Parcel in) {
            return new RemoteObservable(in);
        }

        @Override
        public RemoteObservable[] newArray(int size) {
            return new RemoteObservable[size];
        }
    };
    private static final String TAG = "RemoteObservable";
    private static final boolean DEBUG = false;

    private IBinder remoteEventBinder;

    //*************************************************************

    /**
     * Initialize at the service side  with the {@link RemoteEventController}
     *
     * @param remoteController {@link RemoteEventController} used for generating the events
     */
    public RemoteObservable(RemoteEventController<T> remoteController) {
        this.remoteEventBinder = new RemoteEventManager_Stub(remoteController.getRemoteEventManager());
    }

    /**
     * Internally used for unparcelling
     */
    private RemoteObservable(android.os.Parcel in) {
        remoteEventBinder = in.readStrongBinder();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(android.os.Parcel dest, int flags) {
        dest.writeStrongBinder(remoteEventBinder);
    }

    /**
     * Returns an {@link Observable} which will receive the data send from the service side.
     */
    public Observable<T> getObservable() {
        final RemoteEventManager remoteEventManager = new RemoteEventManager_Proxy(remoteEventBinder);
        final PublishSubject<T> publishSubject = PublishSubject.create();
        return publishSubject.mergeWith(Observable.<T>empty().doOnCompleted(new Action0() {
            @Override
            public void call() {
                try {
                    if (DEBUG) {
                        Log.v(TAG, "onSubscribe ");
                    }

                    remoteEventManager.subscribe(new RemoteEventListener() {
                        @Override
                        public void onRemoteEvent(Bundle remoteData) {
                            remoteData.setClassLoader(this.getClass().getClassLoader());
                            RemoteDataType dataType = RemoteDataType.valueOf(remoteData.getString(RemoteEventManager.REMOTE_DATA_TYPE));
                            T data = getData(remoteData, dataType);
                            if (DEBUG) {
                                Log.v(TAG, "onData " + data);
                            }

                            publishSubject.onNext(data);
                        }

                        @Override
                        public void onCompleted() {
                            synchronized (remoteEventManager) {
                                if (DEBUG) {
                                    Log.v(TAG, "onCompleted ");
                                }
                                publishSubject.onCompleted();
                            }
                        }
                    });
                } catch (Exception ex) {
                    publishSubject.onCompleted();
                }
            }
        })).doOnUnsubscribe(new Action0() {
            @Override
            public void call() {
                try {
                    if (DEBUG) {
                        Log.v(TAG, "unsubscribed ");
                    }
                    remoteEventManager.unsubscribe();
                } catch (Exception ex) {
                    publishSubject.onCompleted();
                }
            }
        }).asObservable();

    }

    /**
     * Reads and returns the correct type of data from the bundle
     */
    private T getData(Bundle remoteData, RemoteDataType dataType) {
        switch (dataType) {
            case Parcelable:
                return (T) remoteData.getParcelable(RemoteEventManager.REMOTE_DATA_KEY);
            case Double:
                return (T) (Double) remoteData.getDouble(RemoteEventManager.REMOTE_DATA_KEY);
            case Float:
                return (T) (Float) remoteData.getFloat(RemoteEventManager.REMOTE_DATA_KEY);
            case Integer:
                return (T) (Integer) remoteData.getInt(RemoteEventManager.REMOTE_DATA_KEY);
            case Long:
                return (T) (Long) remoteData.getLong(RemoteEventManager.REMOTE_DATA_KEY);
            case Byte:
                return (T) (Byte) remoteData.getByte(RemoteEventManager.REMOTE_DATA_KEY);
            case Char:
                return (T) (Character) remoteData.getChar(RemoteEventManager.REMOTE_DATA_KEY);
            case Short:
                return (T) (Short) remoteData.getShort(RemoteEventManager.REMOTE_DATA_KEY);
            case String:
                return (T) remoteData.getString(RemoteEventManager.REMOTE_DATA_KEY);
            case Boolean:
                return (T) (Boolean) (remoteData.getInt(RemoteEventManager.REMOTE_DATA_KEY) == 1);
            case Parceler:
                return getParcelerData(remoteData);
        }
        return null;
    }

    /**
     * Reads and returns the parceler data from bundle
     */
    private T getParcelerData(Bundle remoteData) {
        try {
            Object parcelerObject = remoteData.getParcelable(RemoteEventManager.REMOTE_DATA_KEY);
            return (T) parcelerObject.getClass().getMethod("getParcel", (Class) null).invoke(parcelerObject);
        } catch (Exception exception) {
        }
        return null;
    }
}
