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
import rx.Observable;
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

        Subscription subscription1 = fooObservable.getObservable().observeOn(Schedulers.newThread()).subscribe(new Action1<FooParcelable>() {
            int counter = 0;

            @Override
            public void call(FooParcelable fooParcelable) {
                Log.v(TAG, "onNext sub1 " + fooParcelable.getIntValue());
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

        Subscription subscription2 = fooObservable.getObservable().observeOn(Schedulers.newThread()).subscribe(new Action1<FooParcelable>() {
            int counter = 0;

            @Override
            public void call(FooParcelable fooParcelable) {
                Log.v(TAG, "onNext sub2 " + fooParcelable.getIntValue());
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
        Assert.assertTrue(eventsReceived > 6);
        subscription1.unsubscribe();
        Thread.sleep(5500);
        Assert.assertTrue(eventsReceived > 9);
        subscription2.unsubscribe();
        Thread.sleep(1000);
    }


    @Test
    public void testParcelObservable() throws Exception {
        testParcel();
        testParcel();
    }

    public void testParcel() throws Exception {
        final RemoteObservable<CustomData> fooObservable = sampleService.getCDObservable();
        Log.v(TAG, "CS observable " + fooObservable);
        expectingClose = false;
        Assert.assertNotNull(fooObservable);
        eventsReceived = 0;
        Subscription subscription1 = fooObservable.getObservable().observeOn(Schedulers.newThread()).subscribe(new Action1<CustomData>() {
            int counter = 0;

            @Override
            public void call(CustomData fooParcelable) {
                Assert.assertFalse(expectingClose);
                eventsReceived++;
                Assert.assertEquals(counter, fooParcelable.getData());
                counter++;
                Log.v(TAG, "Data 1st " + fooParcelable.getData());

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

        Log.v(TAG, "Sub1 created " + subscription1);

        Subscription subscription2 = fooObservable.getObservable().observeOn(Schedulers.newThread()).subscribe(new Action1<CustomData>() {
            int counter = 0;

            @Override
            public void call(CustomData fooParcelable) {
                Assert.assertFalse(expectingClose);
                eventsReceived++;
                Assert.assertEquals(counter, fooParcelable.getData());
                counter++;
                Log.v(TAG, "Data 2nd " + fooParcelable.getData());

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

        Log.v(TAG, "Sub2 created " + subscription2);

        Thread.sleep(5500);
        Assert.assertTrue(eventsReceived > 6);
        subscription1.unsubscribe();
        Thread.sleep(5500);
        Assert.assertTrue(eventsReceived > 9);
        subscription2.unsubscribe();
        Thread.sleep(1000);
    }


    @Test
    public void testIntObservable() throws Exception {
        RemoteObservable<Integer> integerRemoteObservable = sampleService.getIntbservable();
        Observable<Integer> integerObservable = integerRemoteObservable.getObservable();
        intObservableTest(integerObservable);

        integerRemoteObservable = sampleService.getIntbservable();
        integerObservable = integerRemoteObservable.getObservable();
        intObservableTest(integerObservable);
    }

    public void intObservableTest(Observable<Integer> observable) throws Exception {
        expectingClose = false;
        eventsReceived = 0;
        Subscription subscription1 = observable.subscribe(new Action1<Integer>() {
            int expected = 9;

            @Override
            public void call(Integer data) {
                Log.v(TAG, "Int data " + data.intValue());
                Assert.assertFalse(expectingClose);
                eventsReceived++;
                Assert.assertEquals(expected, data.intValue());
                expectingClose = true;
            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                Assert.fail("Unexpected observable exception");
            }
        }, new Action0() {
            @Override
            public void call() {
                Log.v(TAG, "Int data onComplete");
                Assert.assertTrue(expectingClose);
            }
        });
        Thread.sleep(3000);
        Assert.assertEquals(1, eventsReceived);

        Subscription subscription2 = observable.subscribe(new Action1<Integer>() {
            int expected = 9;

            @Override
            public void call(Integer data) {
                Assert.fail("Already closed");
            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                Assert.fail("Unexpected observable exception");
            }
        }, new Action0() {
            @Override
            public void call() {
                expectingClose = false;
            }
        });


        Assert.assertFalse(expectingClose);
        subscription1.unsubscribe();
        subscription2.unsubscribe();
    }


}

