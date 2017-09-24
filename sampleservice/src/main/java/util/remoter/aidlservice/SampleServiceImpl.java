package util.remoter.aidlservice;

import android.util.Log;

import io.reactivex.remote.RemoteEventController;
import io.reactivex.remote.RemoteObservable;
import util.remoter.service.CustomData;
import util.remoter.service.FooParcelable;
import util.remoter.service.ISampleService;

public class SampleServiceImpl implements ISampleService {


    private static final String TAG = "SampleService";
    RemoteEventController<Integer> intDataEventController = new RemoteEventController<>();

    SampleServiceImpl() {
        //to test clients will always receive the last data
        intDataEventController.sendEvent(7);
        intDataEventController.sendEvent(9);
        intDataEventController.sendCompleted();
    }

    @Override
    public RemoteObservable<FooParcelable> getFooObservable() {
        Log.v(TAG, "getFooObservable");
        return new RemoteObservable<>(new RemoteEventController<FooParcelable>() {
            boolean stopped = false;
            int counter = 0;
            Thread eventThread;

            @Override
            public void onSubscribed() {
                Log.v(TAG, "onSubscribed");
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
                Log.v(TAG, "onUnSubscribed");
                stopped = true;
                eventThread.interrupt();
                sendCompleted();
            }
        });
    }

    @Override
    public RemoteObservable<CustomData> getCDObservable() {
        return new RemoteObservable<>(new RemoteEventController<CustomData>() {
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
                                        sendEvent(new CustomData(counter));
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
    public RemoteObservable<Integer> getIntbservable() {
        return new RemoteObservable<>(intDataEventController);
    }

    @Override
    public RemoteObservable<String> getStringbservable() {
        return new RemoteObservable<>(new RemoteEventController<String>() {
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
