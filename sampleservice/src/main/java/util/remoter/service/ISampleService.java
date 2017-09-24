package util.remoter.service;

import io.reactivex.remote.RemoteObservable;
import remoter.annotations.Remoter;


/**
 * Service interface to return the remote observables
 */
@Remoter
public interface ISampleService {

    RemoteObservable<FooParcelable> getFooObservable();

    RemoteObservable<CustomData> getCDObservable();

    RemoteObservable<Integer> getIntbservable();

    RemoteObservable<String> getStringbservable();

}
