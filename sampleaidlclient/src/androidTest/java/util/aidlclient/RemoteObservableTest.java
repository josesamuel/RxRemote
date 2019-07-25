package util.aidlclient;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.test.rule.ActivityTestRule;
import android.util.Log;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import io.reactivex.remote.RemoteObservable;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import sample.rxremote.IEchoAidl;

import static util.aidlclient.ServiceIntents.INTENT_AIDL_SERVICE;
import static util.aidlclient.ServiceIntents.INTENT_AIDL_TEST_ACTIVITY;


/**
 * Tests the {@link RemoteObservable}
 */
public class RemoteObservableTest {

    private static final String TAG = RemoteObservableTest.class.getSimpleName();
    private Object objectLock = new Object();
    private IEchoAidl echoService;
    private volatile String expectedData;
    private int eventsReceived;


    ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.i(TAG, "OnService Connected ");
            echoService = IEchoAidl.Stub.asInterface(iBinder);
            synchronized (objectLock) {
                objectLock.notify();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

        }
    };


    @Rule
    public ActivityTestRule<TestActivity> mActivityRule = new ActivityTestRule<TestActivity>(TestActivity.class) {
        @Override
        protected Intent getActivityIntent() {
            Intent intent = new Intent(INTENT_AIDL_TEST_ACTIVITY);
            return intent;
        }
    };

    @Before
    public void setup() throws InterruptedException {
        synchronized (objectLock) {
            Log.i(TAG, "Connecting to service ");
            Intent remoterServiceIntent = new Intent(INTENT_AIDL_SERVICE);
            remoterServiceIntent.setClassName("util.remoter.aidlservice", INTENT_AIDL_SERVICE);

            //mActivityRule.getActivity().startService(remoterServiceIntent);
            mActivityRule.getActivity().bindService(remoterServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE);

            objectLock.wait();
            Log.i(TAG, "Service connected");
        }
    }

    @After
    public void teardown() {
        mActivityRule.getActivity().unbindService(serviceConnection);
    }


    @Test
    public void testEcho() throws Exception {
        RemoteObservable<String> stringObservable = echoService.getStringObservable();

        Assert.assertNotNull(stringObservable);

        expectedData = "1";
        echoService.echo(expectedData);
        Thread.sleep(100);


        Subscription subscription1 = stringObservable.getObservable().observeOn(Schedulers.newThread()).subscribe(new Action1<String>() {
            int counter = 0;

            @Override
            public void call(String string) {
                Log.v(TAG, "onNext sub1 " + string);
                eventsReceived++;
                Assert.assertEquals(expectedData, string);
                counter++;

            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                Assert.fail("Unexpected observable exception");
            }
        }, new Action0() {
            @Override
            public void call() {
                Assert.fail("Not expected");
            }
        });




        expectedData = "2";
        echoService.echo(expectedData);
        Thread.sleep(50);

        expectedData = "3";
        echoService.echo(expectedData);
        Thread.sleep(50);


        Assert.assertTrue(eventsReceived >= 3);
        subscription1.unsubscribe();

    }

}

