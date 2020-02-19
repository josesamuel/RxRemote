package io.reactivex.remote;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Parcelable;
import android.util.Log;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import io.reactivex.remote.internal.LocalEventListener;
import io.reactivex.remote.internal.RemoteDataType;
import io.reactivex.remote.internal.RemoteEventListener;
import io.reactivex.remote.internal.RemoteEventManager;
import io.reactivex.remote.internal.RemoteEventManager_Proxy;
import io.reactivex.remote.internal.RemoteEventManager_Stub;
import io.reactivex.remote.internal.RemoteSubject;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;

/**
 * {@link Observable} across android remote services.
 * <p>
 * This is a {@link Parcelable} which can be passed through remote service
 * aidl or <a href=\"https://bit.ly/Remoter\">Remoter</a> interfaces
 * and then get an {@link Observable} from this class at the client side.
 * <p>
 * At the client side either use {@link #getObservable()} to get an {@link Observable},
 * or use {@link #getData(boolean)} to directly get the last data and register for updates
 * using {@link #setDataListener(RemoteDataListener)}
 * <p>
 * {@link RemoteObservable} can be created using the factory class {@link RemoteObservables}
 * <p>
 * or manually from either an {@link Observable} using {@link #RemoteObservable(Observable)}
 * or {@link RemoteEventController} using {@link #RemoteObservable(RemoteEventController)}
 *
 * @param <T> Supported types are {@link String}, {@link Byte}, {@link Short}, {@link Integer}, {@link Long},
 *            {@link Float}, {@link Double}, {@link Boolean}, {@link Parcelable},
 *            or any class annotated with <a href=\"https://github.com/johncarl81/parceler\">@Parcel</a>
 * @author js
 * @see RemoteObservables
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
    private boolean DEBUG = false;

    private IBinder remoteEventBinder;
    private RemoteSubject<T> remoteSubject;
    private RemoteSubject<T> localSubject;
    private Callable<RemoteObservable<T>> reconnecter;
    private RemoteEventController<T> remoteEventController;
    private T lastData;
    private boolean dataReceived;
    private final Object dataLock = new Object();
    private Subscription internalSubscription;
    private RemoteDataListener<T> dataListener;
    private boolean closed;


    //*************************************************************

    /**
     * Initialize at the service side  with the {@link RemoteEventController}
     *
     * @param remoteController {@link RemoteEventController} used for generating the events
     */
    public RemoteObservable(RemoteEventController<T> remoteController) {
        this.remoteEventController = remoteController;
        this.remoteEventBinder = new RemoteEventManager_Stub(remoteController.getRemoteEventManager());
    }

    /**
     * Initialize at the service side  with an {@link Observable<T>}
     *
     * @param sourceObservable {@link Observable<T>} whose data needs to be delivered remotely
     */
    public RemoteObservable(Observable<T> sourceObservable) {
        this.remoteEventController = new RemoteEventController<>(sourceObservable);
        this.remoteEventBinder = new RemoteEventManager_Stub(remoteEventController.getRemoteEventManager());
    }

    /**
     * Set the listener to get notified when remote end closes connection.
     * Use in the service side to get notified about client connections
     */
    public RemoteObservable<T> setRemoteObservableListener(RemoteObservableListener remoteObservableListener) {
        if (remoteEventController != null) {
            remoteEventController.setRemoteObservableListener(remoteObservableListener);
        }
        return this;
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
        if (closed) {
            throw new IllegalStateException("Already closed");
        }
        if (remoteEventController != null) {
            return getLocalObservable();
        }
        return getRemoteSubject().asObservable();
    }

    /**
     * Same as {@link #getObservable()}, but to be used if this {@link RemoteObservable} is a local instance.
     *
     * @throws IllegalStateException if caled on a {@link RemoteObservable} that is not from a local service
     */
    private Observable<T> getLocalObservable() {
        if (closed) {
            throw new IllegalStateException("Already closed");
        }
        return getLocalSubject().asObservable();
    }

    /**
     * Returns the last data received, blocking until one is available.
     *
     * @see #getData(boolean)
     */
    public T getData() {
        return getData(true);
    }

    /**
     * Returns the last data received, optionally blocking till the data is received
     *
     * @param wait If true, this call will block until a data is available
     */
    public T getData(boolean wait) {
        synchronized (dataLock) {
            if (!dataReceived && wait) {
                registerInternalObserver();
                try {
                    if (!dataReceived) {
                        dataLock.wait();
                    }
                } catch (InterruptedException ignored) {
                }
            }
        }
        return lastData;
    }

    /**
     * Sets a listener to get notified of data.
     * This is another way to get the data other than the {@link #getObservable()}
     *
     * @param dataListener {@link RemoteDataListener} to get notified
     */
    public void setDataListener(RemoteDataListener<T> dataListener) {
        this.dataListener = dataListener;
    }

    private void registerInternalObserver() {
        if (internalSubscription == null) {
            internalSubscription = getObservable().subscribe(new Action1<T>() {
                @Override
                public void call(T t) {
                }
            }, new Action1<Throwable>() {
                @Override
                public void call(Throwable throwable) {
                }
            });
        }
    }


    /**
     * Close this {@link RemoteObservable}
     * No further events will be delivered
     */
    public void close() {
        if (!closed) {
            if (internalSubscription != null) {
                internalSubscription.unsubscribe();
                internalSubscription = null;
            }
            if (remoteSubject != null) {
                remoteSubject.close();
            }
            if (localSubject != null) {
                localSubject.close();
            }

            remoteEventBinder = null;
            remoteSubject = null;
            localSubject = null;
            remoteEventController = null;
            lastData = null;
            dataReceived = false;
            closed = true;
        }
    }


    /**
     * Sets a Callable to be used to reconnect if the connection with the remote
     * service dies.
     */
    public void setReconnecter(Callable<RemoteObservable<T>> reconnecter) {
        this.reconnecter = reconnecter;
    }

    /**
     * Enable or disable debug prints. Disabled by default
     */
    public void setDebug(boolean enable) {
        DEBUG = enable;
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
                                        remoteEventManager.unlinkToDeath(deathRecipient);
                                        RemoteObservable<T> reconectedObservable = reconnecter.call();
                                        if (reconectedObservable != null) {
                                            reconectedObservable.getObservable().subscribe(remoteSubject);
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
                public void close() {
                    super.close();
                    reconnecter = null;
                    remoteEventManager.unlinkToDeath(deathRecipient);
                    remoteEventManager.unsubscribe();
                    remoteEventManager.close();
                    remoteEventManager.destroyProxy();
                    remoteEventListener = null;
                }

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
                        @SuppressWarnings("unchecked")
                        public void onRemoteEvent(Bundle remoteData) {
                            remoteData.setClassLoader(this.getClass().getClassLoader());
                            RemoteDataType dataType = RemoteDataType.valueOf(remoteData.getString(RemoteEventManager.REMOTE_DATA_TYPE));
                            T data = (T) getData(remoteData, dataType, "");
                            if (DEBUG) {
                                Log.v(TAG, "onData " + data);
                            }
                            onDataReceived(data);
                            remoteSubject.onNext(data);
                        }

                        @Override
                        public void onCompleted() {
                            if (DEBUG) {
                                Log.v(TAG, "onCompleted ");
                            }
                            remoteSubject.onCompleted();
                        }

                        @Override
                        public void onError(Exception exception) {
                            remoteSubject.onError(exception);
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
     * Initializes {@link RemoteSubject} as needed and returns
     */
    private synchronized RemoteSubject<T> getLocalSubject() {
        if (localSubject == null) {
            if (remoteEventController == null) {
                throw new IllegalStateException("getLocalObservable can only be called on a local RemoteObservable");
            }

            final RemoteEventManager remoteEventManager = remoteEventController.getRemoteEventManager();
            localSubject = new RemoteSubject<T>() {
                RemoteEventListener remoteEventListener;

                @Override
                public void onFirstSubscribe() {
                    if (DEBUG) {
                        Log.v(TAG, "onFirst subscribe ");
                    }

                    remoteEventListener = new LocalEventListener() {

                        @Override
                        @SuppressWarnings("unchecked")
                        public void onLocalEvent(Object localData) {
                            T data = (T) localData;
                            onDataReceived(data);
                            localSubject.onNext(data);
                        }

                        @Override
                        @SuppressWarnings("unchecked")
                        public void onRemoteEvent(Bundle remoteData) {
                            remoteData.setClassLoader(this.getClass().getClassLoader());
                            RemoteDataType dataType = RemoteDataType.valueOf(remoteData.getString(RemoteEventManager.REMOTE_DATA_TYPE));
                            T data = (T) getData(remoteData, dataType, "");
                            if (DEBUG) {
                                Log.v(TAG, "onData " + data);
                            }
                            onDataReceived(data);
                            localSubject.onNext(data);
                        }

                        @Override
                        public void onCompleted() {
                            if (DEBUG) {
                                Log.v(TAG, "onCompleted ");
                            }
                            localSubject.onCompleted();
                        }

                        @Override
                        public void onError(Exception exception) {
                            localSubject.onError(exception);
                        }
                    };
                    try {
                        remoteEventManager.subscribe(remoteEventListener);
                    } catch (Exception ex) {
                        localSubject.onCompleted();
                    }
                }

                @Override
                public void close() {
                    super.close();
                    reconnecter = null;
                    remoteEventManager.unsubscribe();
                    remoteEventManager.close();
                    remoteEventListener = null;
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
        return localSubject;
    }

    /**
     * Called when a data is received
     */
    private void onDataReceived(T data) {
        synchronized (dataLock) {
            lastData = data;
            dataReceived = true;
            dataLock.notifyAll();
        }
        if (dataListener != null) {
            dataListener.onData(data);
        }
    }


    /**
     * Reads and returns the correct type of data from the bundle
     */
    @SuppressWarnings("unchecked")
    private Object getData(Bundle remoteData, RemoteDataType dataType, String keyPrefix) {
        if (DEBUG) {
            Log.v(TAG, "Parsing datatype " + dataType);
        }
        switch (dataType) {
            case Parcelable:
                return remoteData.getParcelable(RemoteEventManager.REMOTE_DATA_KEY + keyPrefix);
            case Double:
                return remoteData.getDouble(RemoteEventManager.REMOTE_DATA_KEY + keyPrefix);
            case Float:
                return remoteData.getFloat(RemoteEventManager.REMOTE_DATA_KEY + keyPrefix);
            case Integer:
                return remoteData.getInt(RemoteEventManager.REMOTE_DATA_KEY + keyPrefix);
            case Long:
                return remoteData.getLong(RemoteEventManager.REMOTE_DATA_KEY + keyPrefix);
            case Byte:
                return remoteData.getByte(RemoteEventManager.REMOTE_DATA_KEY + keyPrefix);
            case Char:
                return remoteData.getChar(RemoteEventManager.REMOTE_DATA_KEY + keyPrefix);
            case Short:
                return remoteData.getShort(RemoteEventManager.REMOTE_DATA_KEY + keyPrefix);
            case String:
                return remoteData.getString(RemoteEventManager.REMOTE_DATA_KEY + keyPrefix);
            case Boolean:
                return (remoteData.getInt(RemoteEventManager.REMOTE_DATA_KEY + keyPrefix) == 1);
            case Parceler:
                return getParcelerData(remoteData, keyPrefix);
            case Remoter:
                return getRemoterData(remoteData, keyPrefix);
            case List:
                return getListData(remoteData, keyPrefix);

        }
        return null;
    }

    /**
     * Reads and returns the parceler data from bundle
     */
    @SuppressWarnings("unchecked")
    private T getParcelerData(Bundle remoteData, String keyPrefix) {
        try {
            Object parcelerObject = remoteData.getParcelable(RemoteEventManager.REMOTE_DATA_KEY + keyPrefix);
            return (T) parcelerObject.getClass().getMethod("getParcel", (Class[]) null).invoke(parcelerObject);
        } catch (Exception e) {
            if (DEBUG) {
                Log.w(TAG, "Parcel exception ", e);
            }
        }
        return null;
    }

    /**
     * Reads and returns the parceler data from bundle
     */
    @SuppressWarnings("unchecked")
    private T getRemoterData(Bundle remoteData, String keyPrefix) {
        try {
            String remoterInterface = remoteData.getString(RemoteEventManager.REMOTE_DATA_EXTRA + keyPrefix);
            Class parcelClass = Class.forName(remoterInterface + "_Proxy");
            Constructor constructor = parcelClass.getConstructor(IBinder.class);
            return (T) constructor.newInstance(remoteData.getBinder(RemoteEventManager.REMOTE_DATA_KEY + keyPrefix));
        } catch (Exception e) {
            if (DEBUG) {
                Log.w(TAG, "Parcel exception ", e);
            }
        }
        return null;
    }

    /**
     * Reads and returns the list data
     */
    @SuppressWarnings("unchecked")
    private List getListData(Bundle remoteData, String keyPrefix) {
        List list = new ArrayList();
        int size = remoteData.getInt(RemoteEventManager.REMOTE_DATA_LIST_SIZE + keyPrefix, 0);
        for (int i = 0; i < size; i++) {
            RemoteDataType dataType = RemoteDataType.valueOf(remoteData.getString(RemoteEventManager.REMOTE_DATA_TYPE + keyPrefix + i));
            list.add(getData(remoteData, dataType, keyPrefix + i));
        }
        return list;
    }
}
