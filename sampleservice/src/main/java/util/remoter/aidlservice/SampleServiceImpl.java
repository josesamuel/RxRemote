package util.remoter.aidlservice;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.remote.RemoteEventController;
import io.reactivex.remote.RemoteObservable;
import io.reactivex.remote.RemoteObservableListener;
import rx.Observable;
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
        Log.v(TAG, "SampleServiceImpl Create");
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
    public RemoteObservable<Integer> testForRemoteClose() {
        final PublishSubject<Integer> subject = PublishSubject.create();
        Log.v(TAG, "testForRemoteClose");
        RemoteObservable<Integer> remoteObservable =  new RemoteObservable<>(subject.asObservable().doOnSubscribe(new Action0() {
            @Override
            public void call() {
                Thread eventThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(50);
                            Log.v(TAG, "Sending 1");
                            subject.onNext(1);
                            Thread.sleep(1000);
                            Log.v(TAG, "Sending 2");
                            subject.onNext(2);
                            Thread.sleep(1000);
                            Log.v(TAG, "Sending 3");
                            subject.onNext(3);

                            Thread.sleep(1000);
                            subject.onCompleted();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                });

                eventThread.start();
            }
        }).doOnUnsubscribe(() -> Log.v(TAG, " observable OnSubscribed"))

        ).setRemoteObservableListener(new RemoteObservableListener() {
            @Override
            public void onSubscribed() {
                Log.v(TAG, " service OnSubscribed");
            }

            @Override
            public void onUnsubscribe() {
                Log.v(TAG, "service  onUnsubscribe");
            }

            @Override
            public void onClosed() {
                Log.v(TAG, "service  onClosed");
            }
        });

        remoteObservable.setDebug(true);
        return remoteObservable;
    }

    @Override
    public RemoteObservable<Integer> getIntObservableCreatedFromRxObservable() {

        Observable.from(new String[]{"Hello", "World"});

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



    public RemoteObservable<Integer> getIntObservableForClose(){
        Log.d(TAG, "getIntObservableForClose");
        RemoteEventController<Integer> controller = new RemoteEventController<Integer>(){
            @Override
            public void onSubscribed() {
                super.onSubscribed();
                Log.d(TAG, "OnSubscribed");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "Sending 1");
                        sendEvent(1);
                        try {
                            Thread.sleep(1000L);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        Log.d(TAG, "Sending 2");
                        sendEvent(2);
                        try {
                            Thread.sleep(1000L);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        Log.d(TAG, "Sending completed");
                        sendCompleted();
                    }
                }).start();
            }

            @Override
            public void onClosed() {
                super.onClosed();
                Log.d(TAG, "OnClosed");
            }

            @Override
            public void onUnSubscribed() {
                super.onUnSubscribed();
                Log.d(TAG, "onUnSubscribed");
            }
        };
        controller.setDebug(true);
        return new RemoteObservable<>(controller);
    }



}
