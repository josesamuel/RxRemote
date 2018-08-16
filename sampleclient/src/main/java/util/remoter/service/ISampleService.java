package util.remoter.service;

import java.util.List;

import io.reactivex.remote.RemoteObservable;
import remoter.annotations.Remoter;


/**
 * Service interface to return the remote observables
 */
@Remoter
public interface ISampleService {

    RemoteObservable<FooParcelable> getFooObservable();

    RemoteObservable<CustomData> getCDObservable();

    RemoteObservable<Integer> getIntObservable();

    RemoteObservable<String> getStringObservable();

    RemoteObservable<Integer> getIntObservableThatThrowsException();

    RemoteObservable<IEcho> getRemoterObservable();

    RemoteObservable<IGen<String>> getGenericRemoterObservable();

    RemoteObservable<List<String>> getRemoterObservableOfListOfStrings();

    RemoteObservable<List<CustomData>> getRemoterObservableOfListOfParceler();

    RemoteObservable<Integer> getIntObservableCreatedFromRxObservable();

    RemoteObservable<Integer> getIntObservableForClose();

}
