package io.reactivex.remote.internal;

import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

/**
 * Used internally
 * <p>
 * Wraps a {@link RemoteEventListener} implementation and expose it as a remote {@link IBinder}
 * <p>
 * Autogenerated by <a href="https://bit.ly/Remoter">Remoter</a>
 *
 * @see RemoteEventListener_Proxy
 */
public class RemoteEventListener_Stub extends Binder {
    private static final String DESCRIPTOR = "polycom.conference.RemoteEventListener";

    private static final int TRANSACTION_onRemoteEvent_0 = android.os.IBinder.FIRST_CALL_TRANSACTION + 0;

    private static final int TRANSACTION_onCompleted_1 = android.os.IBinder.FIRST_CALL_TRANSACTION + 1;

    private RemoteEventListener serviceImpl;

    /**
     * Initialize this {@link RemoteEventListener_Stub} with the given {@link RemoteEventListener} implementation
     *
     * @param serviceImpl An implementation of {@link RemoteEventListener}
     */
    public RemoteEventListener_Stub(RemoteEventListener serviceImpl) {
        this.serviceImpl = serviceImpl;
        this.attachInterface(new IInterface() {
                                 public IBinder asBinder() {
                                     return RemoteEventListener_Stub.this;
                                 }
                             }
                , DESCRIPTOR);
    }

    @Override
    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        try {
            switch (code) {
                case INTERFACE_TRANSACTION: {
                    reply.writeString(DESCRIPTOR);
                    return true;
                }
                case TRANSACTION_onRemoteEvent_0: {
                    data.enforceInterface(DESCRIPTOR);
                    Bundle arg_stb_0;
                    if (data.readInt() != 0) {
                        arg_stb_0 = android.os.Bundle.CREATOR.createFromParcel(data);
                    } else {
                        arg_stb_0 = null;
                    }
                    serviceImpl.onRemoteEvent(arg_stb_0);
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_onCompleted_1: {
                    data.enforceInterface(DESCRIPTOR);
                    serviceImpl.onCompleted();
                    reply.writeNoException();
                    return true;
                }
            }
        } catch (Exception re) {
            throw new RemoteException();
        }
        return super.onTransact(code, data, reply, flags);
    }
}