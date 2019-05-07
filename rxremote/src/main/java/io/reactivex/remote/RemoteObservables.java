package io.reactivex.remote;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Helper class to create {@link RemoteObservable} and send data through it
 * <p/>
 *
 * <pre><code>
 *
 *   //Example to send RemoteObservables for send events for "Download"
 *
 *   //return to client
 *   RemoteObservable<Integer> remoteObservable = RemoteObservables.<Integer>of("Download").newObservable();
 *
 *   //notify all clients
 *   RemoteObservables.<Integer>of("Download")
 *       .onNext(50)
 *       .onNext(100)
 *       .onCompleted();
 *
 * </code></pre>
 */
public final class RemoteObservables<T> {

    private boolean hasData = false;
    private T lastData = null;
    private final CopyOnWriteArrayList<RemoteEventController<T>> remoteEventControllers = new CopyOnWriteArrayList<>();

    private static final ConcurrentHashMap<Object, RemoteObservables> remoteObservablesMap = new ConcurrentHashMap<>();

    /**
     * Returns {@link RemoteObservables} tied to the given type
     */
    public synchronized static <T> RemoteObservables<T> of(Object type) {
        RemoteObservables<T> remoteObservables = (RemoteObservables<T>) remoteObservablesMap.get(type);
        if (remoteObservables == null) {
            remoteObservables = new RemoteObservables<>();
            remoteObservablesMap.put(type, remoteObservables);
        }
        return remoteObservables;
    }

    private RemoteObservables() {
    }

    /**
     * Creates a new {@link RemoteObservable} to return to client
     *
     * @return a new instance of {@link RemoteObservable} to return to client
     */
    public RemoteObservable<T> newObservable() {
        final RemoteEventController<T> eventController = new RemoteEventController<>();
        remoteEventControllers.add(eventController);
        return new RemoteObservable<>(eventController)
                .setRemoteObservableListener(new RemoteObservableListener() {
                    @Override
                    public void onSubscribed() {
                        if (hasData) {
                            eventController.sendEvent(lastData);
                        }
                    }

                    @Override
                    public void onClosed() {
                        remoteEventControllers.remove(eventController);
                    }
                });
    }

    /**
     * Send the given data to all clients
     *
     * @param data data to send
     */
    public RemoteObservables<T> onNext(T data) {
        lastData = data;
        hasData = true;
        for (RemoteEventController<T> controller : remoteEventControllers) {
            controller.sendEvent(data);
        }
        return this;
    }

    /**
     * Notify all clients that this observable has completed
     */
    public RemoteObservables<T> onCompleted() {
        for (RemoteEventController<T> controller : remoteEventControllers) {
            controller.sendCompleted();
        }
        remoteEventControllers.clear();
        return this;
    }

    /**
     * Notify all clients of error
     */
    public RemoteObservables<T> onError(Exception exception) {
        for (RemoteEventController<T> controller : remoteEventControllers) {
            controller.sendError(exception);
        }
        remoteEventControllers.clear();
        return this;
    }

}
