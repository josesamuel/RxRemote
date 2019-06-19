package util.service;

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

import java.util.ArrayList;
import java.util.List;

import io.reactivex.remote.RemoteObservable;
import io.reactivex.remote.RemoteObservableListener;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import util.remoter.remoterservice.TestActivity;
import util.remoter.service.CustomData;
import util.remoter.service.ExtendedCustomData;
import util.remoter.service.FooParcelable;
import util.remoter.service.IEcho;
import util.remoter.service.IGen;
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
    private volatile boolean expectingOnError;
    private int eventsReceived;
    private Subscription subscription;


    ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.i(TAG, "OnService Connected ");
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
                Log.v(TAG, "Data 1st " + fooParcelable);
                Assert.assertFalse(expectingClose);
                eventsReceived++;
                Assert.assertEquals(counter, fooParcelable.getData());

                if (counter % 3 == 0) {
                    Assert.assertTrue(fooParcelable instanceof CustomData);
                } else if (counter % 3 == 1) {
                    Assert.assertTrue(fooParcelable instanceof ExtendedCustomData);
                } else {
                    Assert.assertTrue(fooParcelable instanceof ExtendedCustomData);
                }


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
    public void testIntObservableFromRxObservable() throws Exception {
        RemoteObservable<Integer> integerRemoteObservable = sampleService.getIntObservableCreatedFromRxObservable();
        Observable<Integer> integerObservable = integerRemoteObservable.getObservable();
        intObservableFromObservableTest(integerObservable);

        intObservableFromObservableTest(sampleService.getIntObservableCreatedFromRxObservable().getObservable());
    }


    @Test
    public void testIntObservable() throws Exception {
        RemoteObservable<Integer> integerRemoteObservable = sampleService.getIntObservable();
        Observable<Integer> integerObservable = integerRemoteObservable.getObservable();
        intObservableTest(integerObservable);

        integerRemoteObservable = sampleService.getIntObservable();
        integerObservable = integerRemoteObservable.getObservable();
        intObservableTest(integerObservable);
        integerRemoteObservable.close();
    }

    @Test
    public void testIntObservableLastData() throws Exception {
        RemoteObservable<Integer> integerRemoteObservable = sampleService.getIntObservableForClose();
        integerRemoteObservable.setDataListener(data -> {
            Assert.assertEquals(data, integerRemoteObservable.getData());
        });

        Assert.assertEquals(1, integerRemoteObservable.getData().intValue());
        Thread.sleep(2100);
        Assert.assertEquals(2, integerRemoteObservable.getData().intValue());
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


    private void intObservableFromObservableTest(Observable<Integer> observable) throws Exception {
        expectingClose = false;
        eventsReceived = 0;
        Subscription subscription1 = observable.subscribe(new Action1<Integer>() {
            int expected = 1;

            @Override
            public void call(Integer data) {
                Log.v(TAG, "Int data " + data.intValue());
                Assert.assertFalse(expectingClose);
                eventsReceived++;
                Assert.assertEquals(expected, data.intValue());
                expected++;
                if (expected == 4) {
                    expectingClose = true;
                }
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
        Thread.sleep(5000);
        Assert.assertEquals(3, eventsReceived);


        Assert.assertTrue(expectingClose);
        subscription1.unsubscribe();
    }


    @Test
    public void intObservableThatThrowsExceptionTest() throws Exception {

        RemoteObservable<Integer> integerRemoteObservable = sampleService.getIntObservableThatThrowsException();
        Observable<Integer> integerObservable = integerRemoteObservable.getObservable();

        expectingClose = false;
        eventsReceived = 0;
        Subscription subscription1 = integerObservable.subscribe(new Action1<Integer>() {
            int expected = 0;

            @Override
            public void call(Integer data) {
                Log.v(TAG, "Int data " + data.intValue());
                Assert.assertFalse(expectingClose);
                eventsReceived++;
                Assert.assertEquals(expected, data.intValue());
                expected++;
                if (eventsReceived == 2) {
                    expectingOnError = true;
                }
            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                Assert.assertTrue(expectingClose);
                expectingClose = false;
            }
        }, new Action0() {
            @Override
            public void call() {
                Log.v(TAG, "Int data onComplete");
                Assert.assertTrue(expectingClose);
            }
        });
        Thread.sleep(3000);
        Assert.assertEquals(2, eventsReceived);
        Assert.assertFalse(expectingClose);

        eventsReceived = 0;

        Subscription subscription2 = integerRemoteObservable.getObservable().subscribe(new Action1<Integer>() {
            int expected = 1;

            @Override
            public void call(Integer data) {
                eventsReceived++;
                Assert.assertEquals(expected, data.intValue());
                expectingOnError = true;
            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                Assert.assertTrue(expectingClose);
                expectingClose = false;
            }
        }, new Action0() {
            @Override
            public void call() {
                Log.v(TAG, "Int data onComplete");
                Assert.assertTrue(expectingClose);
            }
        });

        Thread.sleep(3000);
        Thread.sleep(3000);
        Assert.assertEquals(1, eventsReceived);
        Assert.assertFalse(expectingClose);

        subscription1.unsubscribe();
        subscription2.unsubscribe();
    }


    @Test
    public void testRemoterObservable() throws Exception {
        RemoteObservable<IEcho> remoteObservable = sampleService.getRemoterObservable();
        Observable<IEcho> observable = remoteObservable.getObservable();

        expectingClose = false;
        eventsReceived = 0;
        Subscription subscription1 = observable.subscribe(new Action1<IEcho>() {

            @Override
            public void call(IEcho data) {
                Assert.assertFalse(expectingClose);
                eventsReceived++;
                Log.v(TAG, "Remoter data " + data + " " + data.echo("Hello"));
                Assert.assertEquals("1", data.echo("1"));
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
                Log.v(TAG, "Remoter data onComplete");
                Assert.assertTrue(expectingClose);
            }
        });
        Thread.sleep(3000);
        Assert.assertEquals(1, eventsReceived);
    }

    @Test
    public void testGenericRemoterObservable() throws Exception {
        RemoteObservable<IGen<String>> remoteObservable = sampleService.getGenericRemoterObservable();
        Observable<IGen<String>> observable = remoteObservable.getObservable();

        expectingClose = false;
        eventsReceived = 0;
        Subscription subscription1 = observable.subscribe(new Action1<IGen<String>>() {

            @Override
            public void call(IGen<String> data) {
                Assert.assertFalse(expectingClose);
                eventsReceived++;
                Log.v(TAG, "Remoter data " + data + " " + data.echo("Hello"));
                Assert.assertEquals("1", data.echo("1"));
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
                Log.v(TAG, "Remoter data onComplete");
                Assert.assertTrue(expectingClose);
            }
        });
        Thread.sleep(3000);
        Assert.assertEquals(1, eventsReceived);
    }


    @Test
    public void testListOfStrings() throws Exception {
        RemoteObservable<List<String>> remoteObservable = sampleService.getRemoterObservableOfListOfStrings();
        Observable<List<String>> observable = remoteObservable.getObservable();

        expectingClose = false;
        eventsReceived = 0;
        Subscription subscription1 = observable.subscribe(new Action1<List<String>>() {

            @Override
            public void call(List<String> data) {
                Assert.assertFalse(expectingClose);
                eventsReceived++;
                Log.v(TAG, "List<String> data " + data);
                Assert.assertNotNull(data);
                Assert.assertEquals(2, data.size());
                for (int i = 1; i <= 2; i++) {
                    Assert.assertEquals(String.valueOf(i), data.get(i - 1));
                }
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
                Log.v(TAG, "List data onComplete");
                Assert.assertTrue(expectingClose);
            }
        });
        Thread.sleep(3000);
        Assert.assertEquals(1, eventsReceived);
    }

    @Test
    public void testListOfParceler() throws Exception {
        RemoteObservable<List<CustomData>> remoteObservable = sampleService.getRemoterObservableOfListOfParceler();
        remoteObservable.setDebug(true);
        Observable<List<CustomData>> observable = remoteObservable.getObservable();

        expectingClose = false;
        eventsReceived = 0;
        Subscription subscription1 = observable.subscribe(new Action1<List<CustomData>>() {

            @Override
            public void call(List<CustomData> data) {
                Assert.assertFalse(expectingClose);
                eventsReceived++;
                Log.v(TAG, "List<CustomData> data " + data);
                Assert.assertNotNull(data);
                Assert.assertEquals(2, data.size());
                for (int i = 1; i <= 2; i++) {
                    Assert.assertEquals(i, data.get(i - 1).getData());
                }
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
                Log.v(TAG, "List data onComplete");
                Assert.assertTrue(expectingClose);
            }
        });
        Thread.sleep(3000);
        Assert.assertEquals(1, eventsReceived);
    }


    @Test
    public void testClose() throws Exception {
        final RemoteObservable<Integer> remoteObservable = sampleService.getIntObservableForClose();
        remoteObservable.setDebug(true);
        Observable<Integer> observable = remoteObservable.getObservable();

        expectingClose = false;
        eventsReceived = 0;
        observable.subscribe(new Action1<Integer>() {

            @Override
            public void call(Integer data) {
                Assert.assertFalse(expectingClose);
                eventsReceived++;
                Log.v(TAG, "Int close data " + data);
                Assert.assertNotNull(data);
                Assert.assertEquals(1, data.intValue());
                expectingClose = true;
                remoteObservable.close();
            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                Log.e(TAG, "Exception :", throwable);
                Assert.fail("Unexpected observable exception");
            }
        }, new Action0() {
            @Override
            public void call() {
                Log.v(TAG, "Int close data onComplete");
                Assert.fail("Unexpected observable coplete");
            }
        });

        Log.v(TAG, "Sleeping");
        Thread.sleep(5000);
        Log.v(TAG, "Out of sleep");
        Assert.assertEquals(1, eventsReceived);
    }


    @Test
    public void testClose2() throws Exception {
        Log.v(TAG, "Test client unsubscribe ");
        final RemoteObservable<Integer> remoteObservable = sampleService.testForRemoteClose();
        remoteObservable.setDebug(true);
        Observable<Integer> observable = remoteObservable.getObservable();
        remoteObservable.setRemoteObservableListener(new RemoteObservableListener() {
            @Override
            public void onSubscribed() {
                Log.v(TAG, "client OnSubscribed");
            }

            @Override
            public void onUnsubscribe() {
                Log.v(TAG, "client onUnsubscribe");
            }

            @Override
            public void onClosed() {
                Log.v(TAG, "client onClosed");
            }
        });

        eventsReceived = 0;
        subscription = observable.subscribe(data -> {
            eventsReceived++;
            Log.v(TAG, "Int close data " + data);
            Assert.assertNotNull(data);
            Assert.assertEquals(1, data.intValue());
            Log.v(TAG, "unsubscribing");
            subscription.unsubscribe();
        }, throwable -> {
            Log.e(TAG, "Exception :", throwable);
            Assert.fail("Unexpected observable exception");
        }, () -> {
            Log.v(TAG, "Int close data onComplete");
            Assert.fail("Unexpected observable coplete");
        });

        Log.v(TAG, "Sleeping for 1 sec");
        Thread.sleep(1200);
        Log.v(TAG, "Closing client");
        remoteObservable.close();

        Log.v(TAG, "Sleeping");
        Thread.sleep(5000);
        Log.v(TAG, "Out of sleep");
        Assert.assertEquals(1, eventsReceived);
    }

    int expectedData;
    boolean expectingData;

    @Test
    public void testRemoteObserversFactory() throws Exception {
        Log.v(TAG, "Test RemoteObserversFactory");
        final RemoteObservable<Integer> remoteObservable1 = sampleService.testCreateRemoteObservers();
        final RemoteObservable<Integer> remoteObservable2 = sampleService.testCreateRemoteObservers();

        Observable<Integer> observable1 = remoteObservable1.getObservable();
        Observable<Integer> observable2 = remoteObservable2.getObservable();


        observable1.subscribe(data -> {
            if (expectingData) {
                eventsReceived++;
                Log.v(TAG, "Int  data " + data);
                Assert.assertNotNull(data);
                Assert.assertEquals(expectedData, data.intValue());
            }
        }, throwable -> {
            Log.e(TAG, "Exception :", throwable);
            Assert.fail("Unexpected observable exception");
        }, () -> {
            Log.v(TAG, "Observable complete");
            Assert.assertTrue("Complete ", expectingClose);
            eventsReceived++;
        });

        observable2.subscribe(data -> {
            if (expectingData) {
                eventsReceived++;
                Log.v(TAG, "Int  data " + data);
                Assert.assertNotNull(data);
                Assert.assertEquals(expectedData, data.intValue());
            }
        }, throwable -> {
            Log.e(TAG, "Exception :", throwable);
            Assert.fail("Unexpected observable exception");
        }, () -> {
            Log.v(TAG, "Observable complete");
            Assert.assertTrue("Complete ", expectingClose);
            eventsReceived++;
        });

        Thread.sleep(200);

        eventsReceived = 0;
        expectingData = true;
        expectedData = 10;
        sampleService.testSendRemoteObservers(expectedData);

        Thread.sleep(100);
        Assert.assertEquals(2, eventsReceived);

        expectedData = 20;
        sampleService.testSendRemoteObservers(expectedData);

        Thread.sleep(100);
        Assert.assertEquals(4, eventsReceived);

        expectingClose = true;
        sampleService.testSendCompletedRemoteObservers();
        Thread.sleep(100);
        Assert.assertEquals(6, eventsReceived);

        remoteObservable1.close();
        remoteObservable2.close();

    }
}

