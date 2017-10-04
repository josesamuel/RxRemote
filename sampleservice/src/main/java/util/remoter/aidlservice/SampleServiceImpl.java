package util.remoter.aidlservice;

import android.util.Log;

import io.reactivex.remote.RemoteEventController;
import io.reactivex.remote.RemoteObservable;
import util.remoter.service.CustomData;
import util.remoter.service.ExtendedCustomData;
import util.remoter.service.FooParcelable;
import util.remoter.service.ISampleService;

public class SampleServiceImpl implements ISampleService {


    private static final String TAG = "RemoteObservablesrc";
    RemoteEventController<Integer> intDataEventController = new RemoteEventController<>(Integer.class);

    SampleServiceImpl() {
        //to test clients will always receive the last data
        intDataEventController.sendEvent(7);
        intDataEventController.sendEvent(9);
        intDataEventController.sendCompleted();
    }

    @Override
    public RemoteObservable<FooParcelable> getFooObservable() {
        Log.v(TAG, "getFooObservable");
        return new RemoteObservable<>(new RemoteEventController<FooParcelable>(FooParcelable.class) {
            boolean stopped = false;
            int counter = 0;
            Thread eventThread;

            @Override
            public void onSubscribed() {
                Log.v(TAG, "Foo onSubscribed");
                if (!stopped) {
                    eventThread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            while (!stopped) {
                                try {
                                    Thread.sleep(1000);
                                    if (!stopped) {
                                        Log.v(TAG, "Sending event");
                                        sendEvent(new FooParcelable(String.valueOf(counter), counter));
                                        counter++;
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Exception in thread", e);
                                }
                            }
                            Log.v(TAG, "Out of thread");
                        }
                    });
                    eventThread.start();
                }
            }

            @Override
            public void onUnSubscribed() {
                Log.v(TAG, "foo onUnSubscribed");
                stopped = true;
                eventThread.interrupt();
                sendCompleted();
            }
        });
    }

    @Override
    public RemoteObservable<CustomData> getCDObservable() {
        return new RemoteObservable<>(new RemoteEventController<CustomData>(CustomData.class) {
            boolean stopped = false;
            int counter = 0;
            Thread eventThread;

            @Override
            public void onSubscribed() {
                Log.v(TAG, "CD onSubscribed");
                if (!stopped) {
                    eventThread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            while (!stopped) {
                                try {
                                    Thread.sleep(1000);
                                    if (!stopped) {
                                        CustomData data = null;
                                        if (counter % 2 == 0) {
                                            data = new CustomData(counter);
                                        } else {
                                            data = new ExtendedCustomData(counter);
                                        }
                                        Log.v(TAG, "Sending " + data);
                                        sendEvent(data);
                                        counter++;
                                    }
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    });
                    eventThread.start();
                }
            }

            @Override
            public void onUnSubscribed() {
                Log.v(TAG, "CD onUnSubscribed");
                stopped = true;
                eventThread.interrupt();
                sendCompleted();
            }
        });
    }

    @Override
    public RemoteObservable<Integer> getIntObservable() {
        return new RemoteObservable<>(intDataEventController);
    }

    @Override
    public RemoteObservable<Integer> getIntObservableThatThrowsException() {
        return new RemoteObservable<>(new RemoteEventController<Integer>(Integer.class) {
            boolean stopped = false;
            int counter = 0;
            Thread eventThread;

            @Override
            public void onSubscribed() {
                if (!stopped) {
                    eventThread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            while (!stopped) {
                                try {
                                    Thread.sleep(1000);
                                    if (!stopped) {
                                        if (counter == 2) {
                                            Log.v(TAG, "Sending exception");
                                            sendError(new Exception("Test"));
                                            stopped = true;
                                        } else {
                                            Log.v(TAG, "Sending " + counter);
                                            sendEvent(counter);
                                        }
                                        counter++;
                                    }
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    });
                    eventThread.start();
                }
            }

            @Override
            public void onUnSubscribed() {
                stopped = true;
                eventThread.interrupt();
                sendCompleted();
            }
        });
    }

    @Override
    public RemoteObservable<String> getStringObservable() {
        return new RemoteObservable<>(new RemoteEventController<String>(String.class) {
            boolean stopped = false;
            int counter = 0;
            Thread eventThread;

            @Override
            public void onSubscribed() {
                if (!stopped) {
                    eventThread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            while (!stopped) {
                                try {
                                    Thread.sleep(1000);
                                    if (!stopped) {
                                        sendEvent(String.valueOf(counter));
                                        counter++;
                                    }
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    });
                    eventThread.start();
                }
            }

            @Override
            public void onUnSubscribed() {
                stopped = true;
                eventThread.interrupt();
                sendCompleted();
            }
        });
    }
}
