package util.remoter.aidlservice;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import io.reactivex.remote.RemoteObservable;
import sample.rxremote.IEchoAidl;
import util.remoter.service.ISampleService;
import util.remoter.service.ISampleService_Stub;
import io.reactivex.remote.RemoteObservables;


/**
 * Service that exposes impl for the remoter way
 */
public class SampleAidlService extends Service {

    private static final String TAG = SampleAidlService.class.getSimpleName();

    public SampleAidlService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.v(TAG, "Service Create");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.v(TAG, "Service Bind");
        return new IEchoAidl.Stub() {
            @Override
            public RemoteObservable getStringObservable() throws RemoteException {
                return RemoteObservables.<String>of("data").newObservable();
            }

            @Override
            public void echo(String string) {
                RemoteObservables.<String>of("data").onNext(string);
            }

        };
    }
}