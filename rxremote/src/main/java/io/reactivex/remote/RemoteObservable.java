package io.reactivex.remote;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Parcelable;
import android.util.Log;

import java.util.concurrent.Callable;

import io.reactivex.remote.internal.RemoteDataType;
import io.reactivex.remote.internal.RemoteEventListener;
import io.reactivex.remote.internal.RemoteEventManager;
import io.reactivex.remote.internal.RemoteEventManager_Proxy;
import io.reactivex.remote.internal.RemoteEventManager_Stub;
import io.reactivex.remote.internal.RemoteSubject;
import rx.Observable;

/**
 * {@link Observable} across android remote services.
 * <p>
 * This is a {@link Parcelable} which can be passed through remote service
 * aidl or <a href=\"https://bit.ly/Remoter\">Remoter</a> interfaces
 * and then get an {@link Observable} from this class at the client side.
 *
 * @param <T> Supported types are {@link String}, {@link Byte}, {@link Short}, {@link Integer}, {@link Long},
 *            {@link Float}, {@link Double}, {@link Boolean}, {@link Parcelable},
 *            or any class annotated with <a href=\"https://github.com/johncarl81/parceler\">@Parcel</a>
 * @author js
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
    private RemoteSubject<T> remoteSubject;
    private Callable<RemoteObservable<T>> reconnecter;

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
        return getRemoteSubject().asObservable();
    }

    /**
     * Sets a Callable to be used to reconnect if the connection with the remote
     * service dies.
     */
    public void setReconnecter(Callable<RemoteObservable<T>> reconnecter) {
        this.reconnecter = reconnecter;
    }

    /**
     * Initializes {@link RemoteSubject} as needed and returns
     */
    private synchronized RemoteSubject<T> getRemoteSubject() {
        if (remoteSubject == null) {
            final RemoteEventManager_Proxy remoteEventManager = new RemoteEventManager_Proxy(remoteEventBinder);
            remoteSubject = new RemoteSubject<T>() {
                RemoteEventListener remoteEventListener;
                IBinder.DeathRecipient deathRecipient = new IBinder.DeathRecipient() {
                    @Override
                    public void binderDied() {
                        //connection with service gone.
                        //Try reconnect if a reconnecter is provided.
                        if (reconnecter != null) {
                            int RECONNECT_DELAY = 1000;
                            final HandlerThread handlerThread = new HandlerThread("ObservableReconnect");
                            final Handler handler = new Handler(handlerThread.getLooper());
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        Log.i(TAG, "Attempting reconnection for RemoteObservable");
                                        remoteEventManager.unLinkToDeath(deathRecipient);
                                        RemoteObservable<T> reconectedObservable = reconnecter.call();
                                        if (reconectedObservable != null) {
                                            remoteEventBinder = reconectedObservable.remoteEventBinder;
                                            remoteEventManager.resetBinder(remoteEventBinder);
                                            remoteEventManager.linkToDeath(deathRecipient);
                                            //resubsribe if there are any subscribers
                                            if (remoteEventListener != null) {
                                                onFirstSubscribe();
                                            }
                                        }
                                    } catch (Exception ex) {
                                        Log.w(TAG, "Unable to reconnect", ex);
                                    }
                                    handler.getLooper().quit();
                                }
                            }, RECONNECT_DELAY);
                        } else {
                            Log.i(TAG, "RemoteObservable lost connection with remote service. No reconnector found");
                        }
                    }
                };

                @Override
                public void onInit() {
                    remoteEventManager.linkToDeath(deathRecipient);
                }

                @Override
                public void onFirstSubscribe() {
                    if (DEBUG) {
                        Log.v(TAG, "onFirst subscribe ");
                    }

                    remoteEventListener = new RemoteEventListener() {
                        @Override
                        public void onRemoteEvent(Bundle remoteData) {
                            remoteData.setClassLoader(this.getClass().getClassLoader());
                            RemoteDataType dataType = RemoteDataType.valueOf(remoteData.getString(RemoteEventManager.REMOTE_DATA_TYPE));
                            T data = getData(remoteData, dataType);
                            if (DEBUG) {
                                Log.v(TAG, "onData " + data);
                            }
                            remoteSubject.onNext(data);
                        }

                        @Override
                        public void onCompleted() {
                            synchronized (remoteEventManager) {
                                if (DEBUG) {
                                    Log.v(TAG, "onCompleted ");
                                }
                                remoteSubject.onCompleted();
                            }
                        }
                    };
                    try {
                        remoteEventManager.subscribe(remoteEventListener);
                    } catch (Exception ex) {
                        remoteSubject.onCompleted();
                    }
                }

                @Override
                public void onAllUnsubscribe() {
                    if (DEBUG) {
                        Log.v(TAG, "onAllUnsubscribe");
                    }
                    try {
                        remoteEventManager.unsubscribe();
                    } catch (Exception ignored) {
                    } finally {
                        remoteEventListener = null;
                    }
                }
            };
        }
        return remoteSubject;
    }

    /**
     * Reads and returns the correct type of data from the bundle
     */
    @SuppressWarnings("unchecked")
    private T getData(Bundle remoteData, RemoteDataType dataType) {
        if (DEBUG) {
            Log.v(TAG, "Parsing datatype " + dataType);
        }
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
    @SuppressWarnings("unchecked")
    private T getParcelerData(Bundle remoteData) {
        try {
            Object parcelerObject = remoteData.getParcelable(RemoteEventManager.REMOTE_DATA_KEY);
            return (T) parcelerObject.getClass().getMethod("getParcel", (Class[]) null).invoke(parcelerObject);
        } catch (Exception e) {
            if (DEBUG) {
                Log.w(TAG, "Parcel exception ", e);
            }
        }
        return null;
    }
}
