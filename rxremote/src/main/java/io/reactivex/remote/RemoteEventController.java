package io.reactivex.remote;

import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;

import java.lang.reflect.Constructor;

import io.reactivex.remote.internal.RemoteDataType;
import io.reactivex.remote.internal.RemoteEventListener;
import io.reactivex.remote.internal.RemoteEventManager;

/**
 * Use this class to send the data at the server side that needs to
 * be delivered to the {@link rx.Observable} at the client side.
 * <p>
 * To send a event, use {@link #sendEvent(Object)}
 * <p>
 * Use {@link #sendCompleted()} to inform the client {@link rx.Observable} that
 * the data stream is complete
 *
 * @param <T> Supported types are {@link String}, {@link Byte}, {@link Short}, {@link Integer}, {@link Long},
 *            {@link Float}, {@link Double}, {@link Parcelable},
 *            or any class annotated with <a href=\"https://github.com/johncarl81/parceler\">@Parcel</a>
 */
public class RemoteEventController<T> {

    private static final String TAG = "RemoteEventController";
    private static final boolean DEBUG = true;
    private boolean completed;
    private T lastEvent;
    private RemoteDataType dataType = RemoteDataType.UnKnown;
    private Object LOCK = new Object();
    private RemoteEventHandler remoteEventHandler = new RemoteEventHandler();


    public RemoteEventManager getRemoteEventManager() {
        return remoteEventHandler;
    }

    /**
     * Send the given data to the client observable
     *
     * @param data The data that needs to be send
     * @throws IllegalArgumentException If an unsupported type of data is passed.
     */
    public final void sendEvent(T data) throws IllegalArgumentException {
        if (!completed) {
            synchronized (LOCK) {
                this.lastEvent = data;
                this.dataType = getDataType(data);

                if (dataType == RemoteDataType.UnKnown) {
                    throw new IllegalArgumentException("Unsupported type " + data.getClass());
                }
                remoteEventHandler.sendEventToObservable(lastEvent, dataType);
            }
        }
    }

    /**
     * Generate an sendOnCompleted event at the client observable.
     */
    public final void sendCompleted() {
        synchronized (LOCK) {
            completed = true;
            remoteEventHandler.sendOnCompleted();
        }
    }

    /**
     * Override this to know when a client subscribed to the observable
     */
    public void onSubscribed() {

    }

    /**
     * Override this to know when client unsubscribed.
     */
    public void onUnSubscribed() {

    }

    /**
     * Returns the type of data
     */
    private RemoteDataType getDataType(T data) {
        if (data instanceof Byte) {
            return RemoteDataType.Byte;
        }
        if (data instanceof Short) {
            return RemoteDataType.Short;
        }
        if (data instanceof Integer) {
            return RemoteDataType.Integer;
        }
        if (data instanceof Long) {
            return RemoteDataType.Long;
        }
        if (data instanceof Float) {
            return RemoteDataType.Float;
        }
        if (data instanceof Double) {
            return RemoteDataType.Double;
        }
        if (data instanceof String) {
            return RemoteDataType.String;
        }
        if (data instanceof Character) {
            return RemoteDataType.Char;
        }
        if (data instanceof Parcelable) {
            return RemoteDataType.Parcelable;
        } else {
            try {
                Class.forName(data.getClass().getName() + "$$Parcelable");
                return RemoteDataType.Parceler;
            } catch (ClassNotFoundException ignored) {
                return RemoteDataType.UnKnown;
            }
        }
    }

    /**
     * Covert the data to {@link Parcelable}
     */
    private Parcelable toParcelable(T data) throws Exception {
        if (data instanceof Parcelable) {
            return (Parcelable) data;
        } else {
            Class parcelClass = Class.forName(data.getClass().getName() + "$$Parcelable");
            Constructor constructor = parcelClass.getConstructor(data.getClass());
            return (Parcelable) constructor.newInstance(data);
        }
    }

    class RemoteEventHandler implements RemoteEventManager {

        private RemoteEventListener listener;

        @Override
        public void subscribe(RemoteEventListener listener) {
            if (DEBUG) {
                Log.v(TAG, "onSubscribe " + completed + " " + lastEvent);
            }
            this.listener = listener;
            synchronized (LOCK) {
                if (!completed) {
                    RemoteEventController.this.onSubscribed();
                }
                if (lastEvent != null) {
                    sendEventToObservable(lastEvent, dataType);
                }
                if (completed) {
                    sendOnCompleted();
                }
            }
        }

        @Override
        public void unsubscribe() {
            if (DEBUG) {
                Log.v(TAG, "on unsubscribe" + lastEvent);
            }

            this.listener = null;
            RemoteEventController.this.onUnSubscribed();
        }


        /**
         * Sends the data to observable
         */
        void sendEventToObservable(T data, RemoteDataType dataType) {
            try {
                if (DEBUG) {
                    Log.v(TAG, "Sending event" + listener + " " + data);
                }

                if (this.listener != null) {
                    Bundle remoteData = new Bundle();
                    remoteData.putString(RemoteEventManager.REMOTE_DATA_TYPE, dataType.name());
                    switch (dataType) {
                        case Parcelable:
                        case Parceler:
                            remoteData.putParcelable(RemoteEventManager.REMOTE_DATA_KEY, toParcelable(data));
                            break;
                        case Byte:
                            remoteData.putByte(RemoteEventManager.REMOTE_DATA_KEY, (Byte) data);
                            break;
                        case Short:
                            remoteData.putShort(RemoteEventManager.REMOTE_DATA_KEY, (Short) data);
                            break;
                        case Integer:
                            remoteData.putInt(RemoteEventManager.REMOTE_DATA_KEY, (Integer) data);
                            break;
                        case Float:
                            remoteData.putFloat(RemoteEventManager.REMOTE_DATA_KEY, (Float) data);
                            break;
                        case Double:
                            remoteData.putDouble(RemoteEventManager.REMOTE_DATA_KEY, (Double) data);
                            break;
                        case String:
                            remoteData.putString(RemoteEventManager.REMOTE_DATA_KEY, (String) data);
                            break;
                        case Char:
                            remoteData.putChar(RemoteEventManager.REMOTE_DATA_KEY, (Character) data);
                            break;
                        case Long:
                            remoteData.putLong(RemoteEventManager.REMOTE_DATA_KEY, (Long) data);
                            break;
                    }
                    listener.onRemoteEvent(remoteData);
                }
            } catch (Exception ex) {
                completed = true;
                onUnSubscribed();
            }
        }

        /**
         * Send oncompleted
         */
        void sendOnCompleted() {
            try {
                if (DEBUG) {
                    Log.v(TAG, "Sending complete" + listener);
                }

                if (this.listener != null) {
                    listener.onCompleted();
                }
            } catch (Exception ex) {
                completed = true;
                onUnSubscribed();
            }
        }
    }

}
