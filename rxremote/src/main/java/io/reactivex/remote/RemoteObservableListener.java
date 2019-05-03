package io.reactivex.remote;

/**
 * Listen for the remote close events
 */
public class RemoteObservableListener {

    /**
     * Override this to know when <b>first</b> client subscribed to the observable
     */
    public void onSubscribed() {
    }

    /**
     * Called when <b>ALL</b> clients have unsubscribed.
     */
    public void onUnsubscribe() {
    }


    /**
     * Override this to know when the client closed the remote observable.
     * Perform any cleanup here
     */
    public void onClosed() {
    }
}
