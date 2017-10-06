package io.reactivex.remote;

import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
import android.util.Log;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import io.reactivex.remote.internal.RemoteDataType;
import io.reactivex.remote.internal.RemoteEventListener;
import io.reactivex.remote.internal.RemoteEventListener_Proxy;
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
 *            {@link Float}, {@link Double}, {@link Boolean}, {@link Parcelable},
 *            or any class annotated with <a href=\"https://github.com/johncarl81/parceler\">@Parcel</a>
 * @author js
 */
public class RemoteEventController<T> {

    private static final String TAG = "RemoteEventController";
    private boolean DEBUG = false;
    private boolean completed;
    private T lastEvent;
    private Exception lastException;
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
     * Generate an onCompleted event at the client observable.
     */
    public final void sendCompleted() {
        if (!completed) {
            synchronized (LOCK) {
                completed = true;
                remoteEventHandler.sendOnCompleted();
            }
        }
    }

    /**
     * Generate an onError event at the client observable.
     */
    public final void sendError(Exception exception) {
        if (!completed) {
            synchronized (LOCK) {
                lastException = exception;
                completed = true;
                remoteEventHandler.sendOnError(exception);
            }
        }
    }

    /**
     * Override this to know when <b>first</b> client subscribed to the observable
     */
    public void onSubscribed() {

    }

    /**
     * Override this to know when <b>ALL</b> clients have unsubscribed.
     */
    public void onUnSubscribed() {

    }

    /**
     * Enable or disable debug prints. Disabled by default
     */
    public void setDebug(boolean enable) {
        DEBUG = enable;
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
        if (data instanceof Boolean) {
            return RemoteDataType.Boolean;
        }
        if (data instanceof Parcelable) {
            return RemoteDataType.Parcelable;
        } else if (getParcelerClass(data) != null) {
            return RemoteDataType.Parceler;
        } else {
            return RemoteDataType.UnKnown;
        }
    }


    /**
     * Writes the @Parcel data
     */
    private void writeParceler(T data, Bundle bundle) throws Exception {
        Class parcelerClass = getParcelerClass(data);
        if (parcelerClass != null) {
            Class parcelClass = Class.forName(parcelerClass.getName() + "$$Parcelable");
            Constructor constructor = parcelClass.getConstructor(parcelerClass);
            Parcelable parcelable = (Parcelable) constructor.newInstance(data);
            bundle.putParcelable(RemoteEventManager.REMOTE_DATA_KEY, parcelable);
        }
    }

    /**
     * Finds the parceler class type
     */
    private Class getParcelerClass(Object object) {
        Class objClass = object.getClass();
        boolean found = false;
        while (!found && objClass != null) {
            try {
                Class.forName(objClass.getName() + "$$Parcelable");
                found = true;
            } catch (ClassNotFoundException ignored) {
                objClass = objClass.getSuperclass();
            }
        }
        return objClass;
    }

    class RemoteEventHandler implements RemoteEventManager {

        private RemoteEventListener listener;
        private IBinder.DeathRecipient deathRecipient;

        @Override
        public void subscribe(final RemoteEventListener listener) {
            if (DEBUG) {
                Log.v(TAG, "onSubscribe " + completed + " " + lastEvent);
            }
            synchronized (LOCK) {
                this.listener = listener;
                if (!completed) {
                    RemoteEventController.this.onSubscribed();
                    deathRecipient = new IBinder.DeathRecipient() {
                        @Override
                        public void binderDied() {
                            if (DEBUG) {
                                Log.v(TAG, "Binder dead");
                            }
                            unsubscribe();
                        }
                    };
                    ((RemoteEventListener_Proxy) listener).linkToDeath(deathRecipient);
                }
                if (lastEvent != null) {
                    sendEventToObservable(lastEvent, dataType);
                }
                if (lastException != null) {
                    sendOnError(lastException);
                } else if (completed) {
                    sendOnCompleted();
                }
            }
        }

        @Override
        public void unsubscribe() {
            if (listener != null) {
                if (DEBUG) {
                    Log.v(TAG, "on unsubscribe" + lastEvent);
                }
                RemoteEventController.this.onUnSubscribed();
                ((RemoteEventListener_Proxy) listener).unLinkToDeath(deathRecipient);
                listener = null;
                deathRecipient = null;
            }
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
                            remoteData.putParcelable(RemoteEventManager.REMOTE_DATA_KEY, (Parcelable) data);
                            break;
                        case Parceler:
                            writeParceler(data, remoteData);
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
                        case Boolean:
                            remoteData.putInt(RemoteEventManager.REMOTE_DATA_KEY, ((Boolean) data).booleanValue() ? 1 : 0);
                            break;

                    }
                    listener.onRemoteEvent(remoteData);
                }
            } catch (Exception ex) {
                if (!completed) {
                    completed = true;
                    onUnSubscribed();
                }
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
                    this.listener = null;
                }
            } catch (Exception ex) {
                if (!completed) {
                    completed = true;
                    onUnSubscribed();
                }
            }
        }

        /**
         * Send oncompleted
         */
        void sendOnError(Exception exception) {
            try {
                if (DEBUG) {
                    Log.v(TAG, "Sending onError" + listener);
                }

                if (this.listener != null) {
                    listener.onError(exception);
                    this.listener = null;
                }
            } catch (Exception ex) {
                if (!completed) {
                    completed = true;
                    onUnSubscribed();
                }
            }
        }

    }

}
