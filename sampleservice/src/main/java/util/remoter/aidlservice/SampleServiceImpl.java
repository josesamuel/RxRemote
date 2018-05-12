package util.remoter.aidlservice;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.remote.RemoteEventController;
import io.reactivex.remote.RemoteObservable;
import rx.functions.Action0;
import rx.subjects.PublishSubject;
import util.remoter.service.CustomData;
import util.remoter.service.ExtendedCustomData;
import util.remoter.service.ExtendedCustomData2;
import util.remoter.service.FooParcelable;
import util.remoter.service.IEcho;
import util.remoter.service.IGen;
import util.remoter.service.ISampleService;

public class SampleServiceImpl implements ISampleService {


    private static final String TAG = "RemoteObservablesrc";
    RemoteEventController<Integer> intDataEventController = new RemoteEventController<>();
    RemoteEventController<IEcho> remoterDataEventController = new RemoteEventController<>();
    RemoteEventController<IGen<String>> genericRemoterDataEventController = new RemoteEventController<>();

    SampleServiceImpl() {
        //to test clients will always receive the last data
        intDataEventController.sendEvent(7);
        intDataEventController.sendEvent(9);
        intDataEventController.sendCompleted();

        remoterDataEventController.sendEvent(new EchoImpl());
        remoterDataEventController.sendCompleted();

        genericRemoterDataEventController.sendEvent(new GenImpl<String>());
        genericRemoterDataEventController.sendCompleted();

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
        return new RemoteObservable<>(new RemoteEventController<CustomData>() {
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
                                        if (counter % 3 == 0) {
                                            data = new CustomData(counter);
                                        } else if (counter % 3 == 1) {
                                            data = new ExtendedCustomData(counter);
                                        } else {
                                            data = new ExtendedCustomData2(counter);
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
        return new RemoteObservable<>(new RemoteEventController<Integer>() {
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
    public RemoteObservable<IEcho> getRemoterObservable() {
        return new RemoteObservable<>(remoterDataEventController);
    }

    @Override
    public RemoteObservable<IGen<String>> getGenericRemoterObservable() {
        return new RemoteObservable<>(genericRemoterDataEventController);
    }

    @Override
    public RemoteObservable<String> getStringObservable() {
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

    @Override
    public RemoteObservable<List<String>> getRemoterObservableOfListOfStrings() {
        List<String> data = new ArrayList<>();
        data.add("1");
        data.add("2");
        RemoteEventController<List<String>> controller = new RemoteEventController<List<String>>();
        controller.setDebug(true);
        controller.sendEvent(data);
        controller.sendCompleted();
        return new RemoteObservable<>(controller);
    }

    @Override
    public RemoteObservable<List<CustomData>> getRemoterObservableOfListOfParceler() {
        List<CustomData> data = new ArrayList<>();
        data.add(new CustomData(1));
        data.add(new ExtendedCustomData2(2));
        RemoteEventController<List<CustomData>> controller = new RemoteEventController<List<CustomData>>();
        controller.setDebug(true);
        controller.sendEvent(data);
        controller.sendCompleted();
        return new RemoteObservable<>(controller);

    }

    @Override
    public RemoteObservable<Integer> getIntObservableCreatedFromRxObservable() {
        final PublishSubject<Integer> subject = PublishSubject.create();

        return new RemoteObservable<>(subject.asObservable().doOnSubscribe(new Action0() {
            @Override
            public void call() {
                Thread eventThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(100);
                            subject.onNext(1);
                            Thread.sleep(100);
                            subject.onNext(2);
                            Thread.sleep(100);
                            subject.onNext(3);

                            Thread.sleep(100);
                            subject.onCompleted();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                });

                eventThread.start();
            }
        }));

    }

}
