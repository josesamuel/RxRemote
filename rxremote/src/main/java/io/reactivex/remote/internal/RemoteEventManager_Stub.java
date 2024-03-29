package io.reactivex.remote.internal;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.os.RemoteException;
import android.util.Log;

/**
 * Wraps a {@link RemoteEventManager} implementation and expose it as a remote {@link IBinder}
 * <p>
 * Autogenerated by <a href="https://bit.ly/Remoter">Remoter</a>
 *
 * @see RemoteEventManager_Proxy
 */
public class RemoteEventManager_Stub extends Binder implements RemoterStub {
    private static final String DESCRIPTOR = "io.reactivex.remote.internal.RemoteEventManager";

    private static final int REMOTER_EXCEPTION_CODE = -99999;

    private static final int TRANSACTION_subscribe_0 = IBinder.FIRST_CALL_TRANSACTION + 0;

    private static final int TRANSACTION_unsubscribe_1 = IBinder.FIRST_CALL_TRANSACTION + 1;

    private static final int TRANSACTION_close_2 = IBinder.FIRST_CALL_TRANSACTION + 2;

    private static final int TRANSACTION__getStubID = IBinder.FIRST_CALL_TRANSACTION + 3;

    private RemoteEventManager serviceImpl;

    private BinderWrapper binderWrapper;
    private String serviceName = "";

    /**
     * Initialize this {@link RemoteEventManager_Stub} with the given {@link RemoteEventManager} implementation
     *
     * @param serviceImpl An implementation of {@link RemoteEventManager}
     */
    public RemoteEventManager_Stub(RemoteEventManager serviceImpl) {
        this.serviceImpl = serviceImpl;
        this.serviceName = serviceImpl.getClass().getName();
        this.binderWrapper = new BinderWrapper(this);
        this.attachInterface(binderWrapper, DESCRIPTOR);
    }

    @Override
    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        try {
            switch (code) {
                case INTERFACE_TRANSACTION: {
                    reply.writeString(DESCRIPTOR);
                    return true;
                }
                case TRANSACTION_subscribe_0: {
                    data.enforceInterface(DESCRIPTOR);
                    RemoteEventListener arg_stb_0;
                    arg_stb_0 = new RemoteEventListener_Proxy(data.readStrongBinder());
                    serviceImpl.subscribe(arg_stb_0);
                    return true;
                }
                case TRANSACTION_unsubscribe_1: {
                    data.enforceInterface(DESCRIPTOR);
                    serviceImpl.unsubscribe();
                    return true;
                }
                case TRANSACTION_close_2: {
                    data.enforceInterface(DESCRIPTOR);
                    serviceImpl.close();
                    return true;
                }
                case TRANSACTION__getStubID: {
                    data.enforceInterface(DESCRIPTOR);
                    reply.writeNoException();
                    reply.writeInt(serviceImpl.hashCode());
                    return true;
                }
            }
        } catch (Throwable re) {
            if ((flags & FLAG_ONEWAY) == 0) {
                reply.setDataPosition(0);
                reply.writeInt(REMOTER_EXCEPTION_CODE);
                reply.writeString(re.getMessage());
                reply.writeSerializable(re);
            } else {
                Log.w(serviceName, "Binder call failed.", re);
            }
            return true;
        }
        return super.onTransact(code, data, reply, flags);
    }

    private Class getParcelerClass(Object object) {
        if (object != null) {
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
        return null;
    }

    private Object getParcelerObject(String pClassName, Parcel data) {
        try {
            Parcelable p = null;
            Creator creator = (Creator) Class.forName(pClassName + "$$Parcelable").getField("CREATOR").get(null);
            Object pWrapper = creator.createFromParcel(data);
            return pWrapper.getClass().getMethod("getParcel", (Class[]) null).invoke(pWrapper);
        } catch (Exception ignored) {
            return null;
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }

    @Override
    public void destroyStub() {
        try {
            finalize();
            this.attachInterface(null, DESCRIPTOR);
            binderWrapper.binder = null;
        } catch (Throwable t) {
        }
        serviceImpl = null;
    }

    private static class BinderWrapper implements IInterface {
        private IBinder binder;

        BinderWrapper(IBinder binder) {
            this.binder = binder;
        }

        @Override
        public IBinder asBinder() {
            return binder;
        }
    }
}
