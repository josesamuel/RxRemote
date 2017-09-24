package util.service;

import android.content.ComponentName;
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

import java.util.ArrayList;
import java.util.List;

import io.reactivex.remote.RemoteObservable;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import util.remoter.remoterservice.TestActivity;
import util.remoter.service.CustomData;
import util.remoter.service.FooParcelable;
import util.remoter.service.ISampleService;
import util.remoter.service.ISampleService_Proxy;

import static util.remoter.remoterservice.ServiceIntents.INTENT_AIDL_SERVICE;
import static util.remoter.remoterservice.ServiceIntents.INTENT_REMOTER_TEST_ACTIVITY;


/**
 * Tests the {@link RemoteObservable}
 */
public class RemoteObservableTest {

    private static final String TAG = RemoteObservableTest.class.getSimpleName();
    private Object objectLock = new Object();
    private ISampleService sampleService;
    private volatile boolean expectingClose;
    private int eventsReceived;


    ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            sampleService = new ISampleService_Proxy(iBinder);
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
            Intent intent = new Intent(INTENT_REMOTER_TEST_ACTIVITY);
            return intent;
        }
    };

    @Before
    public void setup() throws InterruptedException {
        synchronized (objectLock) {
            Intent remoterServiceIntent = new Intent(INTENT_AIDL_SERVICE);
            remoterServiceIntent.setClassName("util.remoter.aidlservice", INTENT_AIDL_SERVICE);

            mActivityRule.getActivity().startService(remoterServiceIntent);
            mActivityRule.getActivity().bindService(remoterServiceIntent, serviceConnection, 0);

            objectLock.wait();
            Log.i(TAG, "Service connected");
        }
    }

    @After
    public void teardown() {
        mActivityRule.getActivity().unbindService(serviceConnection);
    }


    @Test
    public void testParcelableObservable() throws Exception {
        testParcelable();
        testParcelable();
    }

    public void testParcelable() throws Exception {
        final RemoteObservable<FooParcelable> fooObservable = sampleService.getFooObservable();
        expectingClose = false;
        eventsReceived = 0;
        Assert.assertNotNull(fooObservable);
        Subscription subscription = fooObservable.getObservable().observeOn(Schedulers.newThread()).subscribe(new Action1<FooParcelable>() {
            int counter = 0;

            @Override
            public void call(FooParcelable fooParcelable) {
                Assert.assertFalse(expectingClose);
                eventsReceived++;
                Assert.assertEquals(counter, fooParcelable.getIntValue());
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

        Thread.sleep(5500);
        Assert.assertTrue(eventsReceived > 3);
        expectingClose = true;
        subscription.unsubscribe();

        Thread.sleep(1000);

        //this should receive the last event and the completed (matching server)
        expectingClose = true;
        eventsReceived = 0;
        Assert.assertNotNull(fooObservable);
        subscription = fooObservable.getObservable().observeOn(Schedulers.newThread()).subscribe(new Action1<FooParcelable>() {
            int counter = 4;

            @Override
            public void call(FooParcelable fooParcelable) {
                Assert.assertEquals(counter, fooParcelable.getIntValue());
            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                Assert.fail("Unexpected observable exception");
            }
        }, new Action0() {
            @Override
            public void call() {
                Assert.assertTrue(expectingClose);
                expectingClose = false;
            }
        });

        Thread.sleep(1000);
        subscription.unsubscribe();
        Assert.assertFalse(expectingClose);
    }


    @Test
    public void testParcelObservable() throws Exception {
        testParcel();
        testParcel();
    }

    public void testParcel() throws Exception {
        final RemoteObservable<CustomData> fooObservable = sampleService.getCDObservable();
        expectingClose = false;
        Assert.assertNotNull(fooObservable);
        eventsReceived = 0;
        Subscription subscription = fooObservable.getObservable().subscribe(new Action1<CustomData>() {
            int counter = 0;

            @Override
            public void call(CustomData fooParcelable) {
                Assert.assertFalse(expectingClose);
                eventsReceived++;
                Assert.assertEquals(counter, fooParcelable.getData());
                counter++;
                Log.v(TAG, "Data 1st " + fooParcelable);

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

        Thread.sleep(5500);
        Assert.assertTrue(eventsReceived > 3);
        expectingClose = true;
        subscription.unsubscribe();

        Thread.sleep(1000);

        //this should receive the last event and the completed (matching server)
        expectingClose = true;
        eventsReceived = 0;
        Assert.assertNotNull(fooObservable);
        subscription = fooObservable.getObservable().subscribe(new Action1<CustomData>() {
            int counter = 4;

            @Override
            public void call(CustomData fooParcelable) {
                Assert.assertEquals(counter, fooParcelable.getData());
            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                Assert.fail("Unexpected observable exception");
            }
        }, new Action0() {
            @Override
            public void call() {
                Log.v(TAG, "complete " + expectingClose);
                Assert.assertTrue(expectingClose);
                expectingClose = false;
            }
        });
        Thread.sleep(1000);
        subscription.unsubscribe();
        Assert.assertFalse(expectingClose);

    }

    @Test
    public void testIntObservable() throws Exception {
        final RemoteObservable<Integer> fooObservable = sampleService.getIntbservable();
        expectingClose = false;
        Assert.assertNotNull(fooObservable);
        eventsReceived = 0;
        Subscription subscription = fooObservable.getObservable().subscribe(new Action1<Integer>() {
            int counter = 0;

            @Override
            public void call(Integer data) {
                Assert.assertFalse(expectingClose);
                eventsReceived++;
                Assert.assertEquals(counter, data.intValue());
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

        Thread.sleep(5500);
        Assert.assertTrue(eventsReceived > 3);
        expectingClose = true;
        subscription.unsubscribe();
    }

    @Test
    public void testStringObservable() throws Exception {
        final RemoteObservable<String> fooObservable = sampleService.getStringbservable();
        expectingClose = false;
        Assert.assertNotNull(fooObservable);
        eventsReceived = 0;
        Subscription subscription = fooObservable.getObservable().subscribe(new Action1<String>() {
            int counter = 0;

            @Override
            public void call(String data) {
                Assert.assertFalse(expectingClose);
                eventsReceived++;
                Assert.assertEquals(String.valueOf(counter), data);
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

        Thread.sleep(5500);
        Assert.assertTrue(eventsReceived > 3);
        expectingClose = true;
        subscription.unsubscribe();
    }

}

