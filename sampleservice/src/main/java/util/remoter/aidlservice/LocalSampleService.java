package util.remoter.aidlservice;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import util.remoter.service.ISampleService;


/**
 * Service that exposes impl for the remoter way
 */
public class LocalSampleService extends Service {

    private static final String TAG = LocalSampleService.class.getSimpleName();
    private ISampleService serviceImpl = new SampleServiceImpl();

    public class LocalBinder extends Binder {
        public ISampleService getService() {
            return serviceImpl;
        }
    }

    public LocalSampleService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.v(TAG, "Local Service Create");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new LocalBinder();
    }
}