package util.remoter.service;

import remoter.annotations.Remoter;

/**
 * To test @Remoter can be passed
 */
@Remoter
public interface IGen<T> {
    T echo(T input);
}
