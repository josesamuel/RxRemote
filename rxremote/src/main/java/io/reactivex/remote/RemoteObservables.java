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
 *   RemoteObservable{@literal <}Integer{@literal >} remoteObservable = RemoteObservables.{@literal <}Integer{@literal >}of("Download").newObservable();
 *
 *   //notify all clients
 *   RemoteObservables.{@literal <}Integer{@literal >}of("Download")
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
     * By default this observable will emit every data that is send using {@link #onNext(Object)}
     *
     * @return a new instance of {@link RemoteObservable} to return to client
     */
    public RemoteObservable<T> newObservable() {
        return newObservable(false);
    }

    /**
     * Creates a new {@link RemoteObservable} to return to client.
     * To send data use {@link #onNext(Object)}
     *
     * @param ignoreDuplicates Whether this observable to ignore duplicates send to it
     * @return a new instance of {@link RemoteObservable} to return to client
     */
    public RemoteObservable<T> newObservable(boolean ignoreDuplicates) {
        final RemoteEventController<T> eventController = new RemoteEventController<>();
        eventController.setIgnoreIfDuplicateOfLast(ignoreDuplicates);
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
     * Send the given data to all clients that created using {@link #newObservable()}
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
